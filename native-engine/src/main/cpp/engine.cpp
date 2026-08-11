#include "engine.h"

#include <algorithm>
#include <climits>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <string>
#include <vector>

// ---------------------------------------------------------------------------
// Representation
// ---------------------------------------------------------------------------

namespace {

// Piece codes. White 1..6, Black 9..14, 0 = empty.
constexpr uint8_t EMPTY = 0;
constexpr uint8_t WP = 1, WN = 2, WB = 3, WR = 4, WQ = 5, WK = 6;
constexpr uint8_t BP = 9, BN = 10, BB = 11, BR = 12, BQ = 13, BK = 14;

inline bool isWhite(uint8_t p) { return p != EMPTY && p <= 6; }
inline bool isBlack(uint8_t p) { return p >= 9; }
inline uint8_t typeOf(uint8_t p) { return p <= 6 ? p : p - 8; }

inline int fileOf(int sq) { return sq & 7; }
inline int rankOf(int sq) { return sq >> 3; }
inline int idx(int f, int r) { return r * 8 + f; }

struct State {
  uint8_t b[64];
  int side;      // 0 white, 1 black
  int ep;        // en-passant target square, or -1
  int castling;  // WK=1 WQ=2 BK=4 BQ=8
};

struct Move {
  uint8_t from, to, promo;
};

State root;

constexpr int WK_FLAG = 1, WQ_FLAG = 2, BK_FLAG = 4, BQ_FLAG = 8;

const int MAT[7] = {0, 100, 320, 330, 500, 900, 20000};

// Piece-square tables from White's view (index 0 = a8 .. 63 = h1).
const int PST[7][64] = {
    {},  // unused
    { // pawn
        0, 0, 0, 0, 0, 0, 0, 0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
        5, 5, 10, 25, 25, 10, 5, 5,
        0, 0, 0, 20, 20, 0, 0, 0,
        5, -5, -10, 0, 0, -10, -5, 5,
        5, 10, 10, -20, -20, 10, 10, 5,
        0, 0, 0, 0, 0, 0, 0, 0,
    },
    { // knight
        -50, -40, -30, -30, -30, -30, -40, -50,
        -40, -20, 0, 0, 0, 0, -20, -40,
        -30, 0, 10, 15, 15, 10, 0, -30,
        -30, 5, 15, 20, 20, 15, 5, -30,
        -30, 0, 15, 20, 20, 15, 0, -30,
        -30, 5, 10, 15, 15, 10, 5, -30,
        -40, -20, 0, 5, 5, 0, -20, -40,
        -50, -40, -30, -30, -30, -30, -40, -50,
    },
    { // bishop
        -20, -10, -10, -10, -10, -10, -10, -20,
        -10, 0, 0, 0, 0, 0, 0, -10,
        -10, 0, 5, 10, 10, 5, 0, -10,
        -10, 5, 5, 10, 10, 5, 5, -10,
        -10, 0, 10, 10, 10, 10, 0, -10,
        -10, 10, 10, 10, 10, 10, 10, -10,
        -10, 5, 0, 0, 0, 0, 5, -10,
        -20, -10, -10, -10, -10, -10, -10, -20,
    },
    { // rook
        0, 0, 0, 0, 0, 0, 0, 0,
        5, 10, 10, 10, 10, 10, 10, 5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        0, 0, 0, 5, 5, 0, 0, 0,
    },
    { // queen
        -20, -10, -10, -5, -5, -10, -10, -20,
        -10, 0, 0, 0, 0, 0, 0, -10,
        -10, 0, 5, 5, 5, 5, 0, -10,
        -5, 0, 5, 5, 5, 5, 0, -5,
        0, 0, 5, 5, 5, 5, 0, -5,
        -10, 5, 5, 5, 5, 5, 0, -10,
        -10, 0, 5, 0, 0, 0, 0, -10,
        -20, -10, -10, -5, -5, -10, -10, -20,
    },
    { // king
        -30, -40, -40, -50, -50, -40, -40, -30,
        -30, -40, -40, -50, -50, -40, -40, -30,
        -30, -40, -40, -50, -50, -40, -40, -30,
        -30, -40, -40, -50, -50, -40, -40, -30,
        -20, -30, -30, -40, -40, -30, -30, -20,
        -10, -20, -20, -20, -20, -20, -20, -10,
        20, 20, 0, 0, 0, 0, 20, 20,
        20, 30, 10, 0, 0, 10, 30, 20,
    },
};

int kingSq(const State& s, int side) {
  uint8_t target = side == 0 ? WK : BK;
  for (int i = 0; i < 64; ++i)
    if (s.b[i] == target) return i;
  return -1;
}

// Returns the file at which a ray walk along `delta` wraps (0 or 7), or -1
// for pure vertical moves.
inline int wrapFile(int delta) {
  switch (delta) {
    case 1:
    case 9:
    case -7:
      return 0;
    case -1:
    case 7:
    case -9:
      return 7;
    default:
      return -1;
  }
}

// ---------------------------------------------------------------------------
// Attack detection
// ---------------------------------------------------------------------------

bool attackedBy(const State& s, int sq, int by) {
  int f = fileOf(sq);
  if (by == 0) {  // white pawns attack sq from sq-7 (file+1) / sq-9 (file-1)
    if (f < 7) {
      int a = sq - 7;
      if (a >= 0 && s.b[a] == WP) return true;
    }
    if (f > 0) {
      int a = sq - 9;
      if (a >= 0 && s.b[a] == WP) return true;
    }
  } else {  // black pawns attack sq from sq+9 (file+1) / sq+7 (file-1)
    if (f < 7) {
      int a = sq + 9;
      if (a < 64 && s.b[a] == BP) return true;
    }
    if (f > 0) {
      int a = sq + 7;
      if (a < 64 && s.b[a] == BP) return true;
    }
  }
  // Knights
  static const int ND[8] = {17, 15, 10, 6, -6, -10, -15, -17};
  for (int d : ND) {
    int a = sq + d;
    if (a < 0 || a >= 64) continue;
    if (by == 0 && s.b[a] == WN) return true;
    if (by == 1 && s.b[a] == BN) return true;
  }
  // Adjacent kings
  static const int KD[8] = {1, -1, 7, -7, 8, -8, 9, -9};
  for (int d : KD) {
    int a = sq + d;
    if (a < 0 || a >= 64) continue;
    if (by == 0 && s.b[a] == WK) return true;
    if (by == 1 && s.b[a] == BK) return true;
  }
  // Sliders
  static const int SD[8] = {1, -1, 8, -8, 7, -7, 9, -9};
  for (int d : SD) {
    bool orth = d == 1 || d == -1 || d == 8 || d == -8;
    int wf = wrapFile(d);
    for (int s2 = sq + d; s2 >= 0 && s2 < 64; s2 += d) {
      if (wf >= 0 && fileOf(s2) == wf) break;
      uint8_t p = s.b[s2];
      if (p == EMPTY) continue;
      uint8_t t = typeOf(p);
      bool attacks = (orth && (t == WR || t == WQ)) || (!orth && (t == WB || t == WQ));
      bool color = isWhite(p) ? 0 : 1;
      if (color == by && attacks) return true;
      break;
    }
  }
  return false;
}

bool inCheck(const State& s, int side) {
  int k = kingSq(s, side);
  if (k < 0) return false;
  return attackedBy(s, k, 1 - side);
}

// ---------------------------------------------------------------------------
// Move generation
// ---------------------------------------------------------------------------

inline bool canMoveTo(const State& s, int to, int side) {
  uint8_t p = s.b[to];
  if (p == EMPTY) return true;
  return (side == 0 && isBlack(p)) || (side == 1 && isWhite(p));
}

void pushPromo(std::vector<Move>& moves, int from, int to, int side) {
  int promoRank = side == 0 ? 7 : 0;
  if (rankOf(to) == promoRank) {
    for (uint8_t pr = 2; pr <= 5; ++pr) moves.push_back({(uint8_t)from, (uint8_t)to, pr});
  } else {
    moves.push_back({(uint8_t)from, (uint8_t)to, 0});
  }
}

void addPawnMoves(const State& s, int from, int side, std::vector<Move>& moves) {
  int f = fileOf(from), r = rankOf(from);
  if (side == 0) {
    int one = from + 8;
    if (one < 64 && s.b[one] == EMPTY) {
      if (r == 6) {
        for (uint8_t pr = 2; pr <= 5; ++pr) moves.push_back({(uint8_t)from, (uint8_t)one, pr});
      } else {
        moves.push_back({(uint8_t)from, (uint8_t)one, 0});
        int two = from + 16;
        if (r == 1 && two < 64 && s.b[two] == EMPTY)
          moves.push_back({(uint8_t)from, (uint8_t)two, 0});
      }
    }
    if (f > 0) {
      int to = from + 7;
      if (to < 64 && (isBlack(s.b[to]) || to == s.ep)) pushPromo(moves, from, to, side);
    }
    if (f < 7) {
      int to = from + 9;
      if (to < 64 && (isBlack(s.b[to]) || to == s.ep)) pushPromo(moves, from, to, side);
    }
  } else {
    int one = from - 8;
    if (one >= 0 && s.b[one] == EMPTY) {
      if (r == 1) {
        for (uint8_t pr = 2; pr <= 5; ++pr) moves.push_back({(uint8_t)from, (uint8_t)one, pr});
      } else {
        moves.push_back({(uint8_t)from, (uint8_t)one, 0});
        int two = from - 16;
        if (r == 6 && two >= 0 && s.b[two] == EMPTY)
          moves.push_back({(uint8_t)from, (uint8_t)two, 0});
      }
    }
    if (f > 0) {
      int to = from - 9;
      if (to >= 0 && (isWhite(s.b[to]) || to == s.ep)) pushPromo(moves, from, to, side);
    }
    if (f < 7) {
      int to = from - 7;
      if (to >= 0 && (isWhite(s.b[to]) || to == s.ep)) pushPromo(moves, from, to, side);
    }
  }
}

void addSlidingMoves(const State& s, int from, int side, int delta,
                     std::vector<Move>& moves) {
  int wf = wrapFile(delta);
  for (int to = from + delta; to >= 0 && to < 64; to += delta) {
    if (wf >= 0 && fileOf(to) == wf) break;
    uint8_t p = s.b[to];
    if (p != EMPTY) {
      if (canMoveTo(s, to, side)) moves.push_back({(uint8_t)from, (uint8_t)to, 0});
      break;
    }
    moves.push_back({(uint8_t)from, (uint8_t)to, 0});
  }
}

void generatePseudo(const State& s, int side, std::vector<Move>& moves) {
  for (int from = 0; from < 64; ++from) {
    uint8_t p = s.b[from];
    if (p == EMPTY) continue;
    bool mine = (side == 0 && isWhite(p)) || (side == 1 && isBlack(p));
    if (!mine) continue;
    uint8_t t = typeOf(p);
    int f = fileOf(from), r = rankOf(from);
    switch (t) {
      case 1:
        addPawnMoves(s, from, side, moves);
        break;
      case 2: {
        static const int D[8] = {17, 15, 10, 6, -6, -10, -15, -17};
        for (int d : D) {
          int to = from + d;
          if (to < 0 || to >= 64) continue;
          if (d == 17 && f > 6) continue;
          if (d == 15 && f == 0) continue;
          if (d == 10 && f > 5) continue;
          if (d == 6 && f < 2) continue;
          if (d == -6 && f > 5) continue;
          if (d == -10 && f < 2) continue;
          if (d == -15 && f == 7) continue;
          if (d == -17 && f < 1) continue;
          if (canMoveTo(s, to, side)) moves.push_back({(uint8_t)from, (uint8_t)to, 0});
        }
        break;
      }
      case 3:
        addSlidingMoves(s, from, side, 7, moves);
        addSlidingMoves(s, from, side, -7, moves);
        addSlidingMoves(s, from, side, 9, moves);
        addSlidingMoves(s, from, side, -9, moves);
        break;
      case 4:
        addSlidingMoves(s, from, side, 1, moves);
        addSlidingMoves(s, from, side, -1, moves);
        addSlidingMoves(s, from, side, 8, moves);
        addSlidingMoves(s, from, side, -8, moves);
        break;
      case 5:
        addSlidingMoves(s, from, side, 1, moves);
        addSlidingMoves(s, from, side, -1, moves);
        addSlidingMoves(s, from, side, 8, moves);
        addSlidingMoves(s, from, side, -8, moves);
        addSlidingMoves(s, from, side, 7, moves);
        addSlidingMoves(s, from, side, -7, moves);
        addSlidingMoves(s, from, side, 9, moves);
        addSlidingMoves(s, from, side, -9, moves);
        break;
      case 6: {
        static const int D[8] = {1, -1, 7, -7, 8, -8, 9, -9};
        for (int d : D) {
          int to = from + d;
          if (to < 0 || to >= 64) continue;
          if (d == 1 && f == 7) continue;
          if (d == -1 && f == 0) continue;
          if (d == 7 && f == 0) continue;
          if (d == -7 && f == 7) continue;
          if (d == 9 && f == 7) continue;
          if (d == -9 && f == 0) continue;
          if (canMoveTo(s, to, side)) moves.push_back({(uint8_t)from, (uint8_t)to, 0});
        }
        if (side == 0 && from == 4 && r == 0) {
          if ((s.castling & WK_FLAG) && s.b[5] == EMPTY && s.b[6] == EMPTY)
            moves.push_back({4, 6, 0});
          if ((s.castling & WQ_FLAG) && s.b[1] == EMPTY && s.b[2] == EMPTY && s.b[3] == EMPTY)
            moves.push_back({4, 2, 0});
        } else if (side == 1 && from == 60 && r == 7) {
          if ((s.castling & BK_FLAG) && s.b[61] == EMPTY && s.b[62] == EMPTY)
            moves.push_back({60, 62, 0});
          if ((s.castling & BQ_FLAG) && s.b[57] == EMPTY && s.b[58] == EMPTY && s.b[59] == EMPTY)
            moves.push_back({60, 58, 0});
        }
        break;
      }
    }
  }
}

State applyMove(State s, Move m) {
  uint8_t moving = s.b[m.from];
  uint8_t captured = s.b[m.to];

  if (typeOf(moving) == 1 && m.to == s.ep && captured == EMPTY) {
    int capSq = (s.side == 0) ? m.to - 8 : m.to + 8;
    s.b[capSq] = EMPTY;
  }

  s.b[m.from] = EMPTY;
  if (m.promo != 0) {
    s.b[m.to] = (uint8_t)((s.side == 0 ? 0 : 8) + m.promo);
  } else {
    s.b[m.to] = moving;
  }

  if (typeOf(moving) == 6) {
    int diff = m.to - m.from;
    if (diff == 2) {
      int rookFrom = m.from + 3, rookTo = m.from + 1;
      s.b[rookTo] = s.b[rookFrom];
      s.b[rookFrom] = EMPTY;
    } else if (diff == -2) {
      int rookFrom = m.from - 4, rookTo = m.from - 1;
      s.b[rookTo] = s.b[rookFrom];
      s.b[rookFrom] = EMPTY;
    }
    s.castling &= (s.side == 0) ? ~(WK_FLAG | WQ_FLAG) : ~(BK_FLAG | BQ_FLAG);
  }
  if (typeOf(moving) == 4) {
    if (m.from == 0) s.castling &= ~WQ_FLAG;
    if (m.from == 7) s.castling &= ~WK_FLAG;
    if (m.from == 56) s.castling &= ~BQ_FLAG;
    if (m.from == 63) s.castling &= ~BK_FLAG;
  }
  if (m.to == 0) s.castling &= ~WQ_FLAG;
  if (m.to == 7) s.castling &= ~WK_FLAG;
  if (m.to == 56) s.castling &= ~BQ_FLAG;
  if (m.to == 63) s.castling &= ~BK_FLAG;

  if (typeOf(moving) == 1) {
    int dr = m.to - m.from;
    if (dr == 16) s.ep = m.from + 8;
    else if (dr == -16) s.ep = m.from - 8;
    else s.ep = -1;
  } else {
    s.ep = -1;
  }

  s.side = 1 - s.side;
  return s;
}

void generateLegal(const State& s, int side, std::vector<Move>& moves) {
  std::vector<Move> pseudo;
  generatePseudo(s, side, pseudo);
  for (Move m : pseudo) {
    State n = applyMove(s, m);
    if (inCheck(n, side)) continue;
    int wk = kingSq(n, 0), bk = kingSq(n, 1);
    if (wk >= 0 && bk >= 0) {
      int df = fileOf(wk) - fileOf(bk);
      int dr = rankOf(wk) - rankOf(bk);
      if (df * df <= 1 && dr * dr <= 1) continue;
    }
    moves.push_back(m);
  }
}

// ---------------------------------------------------------------------------
// Evaluation
// ---------------------------------------------------------------------------

int evaluate(const State& s) {
  int score = 0;
  for (int sq = 0; sq < 64; ++sq) {
    uint8_t p = s.b[sq];
    if (p == EMPTY) continue;
    int val = MAT[typeOf(p)] + PST[typeOf(p)][sq];
    if (isBlack(p)) score -= val;
    else score += val;
  }
  return score;
}

// ---------------------------------------------------------------------------
// Search
// ---------------------------------------------------------------------------

constexpr int MATE = 100000;

int negamax(const State& s, int depth, int alpha, int beta) {
  std::vector<Move> legal;
  generateLegal(s, s.side, legal);
  if (legal.empty()) return inCheck(s, s.side) ? -(MATE + depth) : 0;
  if (depth <= 0) return s.side == 0 ? evaluate(s) : -evaluate(s);
  int best = INT_MIN;
  for (Move m : legal) {
    State n = applyMove(s, m);
    int score = -negamax(n, depth - 1, -beta, -alpha);
    if (score > best) best = score;
    if (best > alpha) alpha = best;
    if (alpha >= beta) break;
  }
  return best;
}

int moveOrderScore(const State& s, Move m) {
  uint8_t victim = s.b[m.to];
  if (victim != EMPTY) return 1000 + 10 * (9 - typeOf(victim));
  if (m.promo != 0) return 800;
  return 0;
}

Move searchBest(const State& rootIn, int maxDepth) {
  std::vector<Move> legal;
  generateLegal(rootIn, rootIn.side, legal);
  if (legal.empty()) return Move{0, 0, 0};
  std::sort(legal.begin(), legal.end(), [&](Move a, Move b) {
    return moveOrderScore(rootIn, a) > moveOrderScore(rootIn, b);
  });

  Move best = legal[0];
  for (int depth = 1; depth <= maxDepth; ++depth) {
    Move depthBest = legal[0];
    int depthScore = INT_MIN;
    for (Move m : legal) {
      State n = applyMove(rootIn, m);
      int score = -negamax(n, depth - 1, INT_MIN, INT_MAX);
      if (score > depthScore) {
        depthScore = score;
        depthBest = m;
      }
    }
    best = depthBest;
  }
  return best;
}

std::string uciOf(Move m) {
  std::string s;
  s += static_cast<char>('a' + fileOf(m.from));
  s += static_cast<char>('1' + rankOf(m.from));
  s += static_cast<char>('a' + fileOf(m.to));
  s += static_cast<char>('1' + rankOf(m.to));
  if (m.promo != 0) {
    static const char* names = " pnbrq";
    s += names[m.promo];
  }
  return s;
}

bool parseFen(const char* fen, State& s) {
  std::memset(&s, 0, sizeof(s));
  s.ep = -1;
  int sq = 56;
  const char* p = fen;
  while (*p && *p != ' ') {
    if (*p == '/') {
      sq -= 16;
    } else if (*p >= '1' && *p <= '8') {
      sq += (*p - '0');
    } else {
      if (sq < 0 || sq >= 64) return false;
      switch (*p) {
        case 'P': s.b[sq] = WP; break;
        case 'N': s.b[sq] = WN; break;
        case 'B': s.b[sq] = WB; break;
        case 'R': s.b[sq] = WR; break;
        case 'Q': s.b[sq] = WQ; break;
        case 'K': s.b[sq] = WK; break;
        case 'p': s.b[sq] = BP; break;
        case 'n': s.b[sq] = BN; break;
        case 'b': s.b[sq] = BB; break;
        case 'r': s.b[sq] = BR; break;
        case 'q': s.b[sq] = BQ; break;
        case 'k': s.b[sq] = BK; break;
        default: return false;
      }
      ++sq;
    }
    ++p;
  }
  while (*p == ' ') ++p;
  if (*p == 'w') s.side = 0;
  else if (*p == 'b') s.side = 1;
  else return false;
  ++p;
  while (*p == ' ') ++p;
  if (*p == '-') {
    ++p;
  } else {
    while (*p && *p != ' ') {
      if (*p == 'K') s.castling |= WK_FLAG;
      else if (*p == 'Q') s.castling |= WQ_FLAG;
      else if (*p == 'k') s.castling |= BK_FLAG;
      else if (*p == 'q') s.castling |= BQ_FLAG;
      ++p;
    }
  }
  while (*p == ' ') ++p;
  if (*p && *p != '-') {
    int f = *p - 'a';
    ++p;
    int r = *p - '1';
    if (f >= 0 && f <= 7 && r >= 0 && r <= 7) s.ep = idx(f, r);
  }
  return true;
}

}  // namespace

// ---------------------------------------------------------------------------
// Exported C API
// ---------------------------------------------------------------------------

extern "C" {

int chess_engine_version(void) { return ENGINE_BINDING_VERSION; }

int chess_engine_load(const char* fen) {
  State s;
  if (!fen || !parseFen(fen, s)) return -1;
  root = s;
  return 0;
}

int chess_engine_best_move(char* out, int cap) {
  Move m = searchBest(root, 4);
  if (m.from == m.to && m.to == 0) return 0;
  std::string uci = uciOf(m);
  int n = std::min<int>(cap - 1, static_cast<int>(uci.size()));
  if (n < 0) n = 0;
  std::memcpy(out, uci.data(), static_cast<size_t>(n));
  out[n] = 0;
  return n;
}

int chess_engine_eval(void) { return evaluate(root); }

}  // extern "C"