package plantillas;

import java.util.*;

/**
 * Trie binario persistente: el uso clásico en competitiva del "trie persistente".
 * Guarda enteros no negativos por sus `bits` bits (30 por defecto; `new PersistentBinaryTrie(20)` si los valores
 * son < 2^20, que es más rápido); cada insert copia un camino: O(bits).
 *
 * Problema típico: XOR máximo de x con algún a[i], i en [l, r].
 *   PersistentBinaryTrie t = new PersistentBinaryTrie();
 *   int[] pre = new int[n + 1];                    // pre[0] = 0 = trie vacío
 *   for (int i = 1; i <= n; i++) pre[i] = t.insert(pre[i - 1], a[i]);
 *   long best = t.maxXor(pre[l - 1], pre[r], x);   // usa la resta de versiones (r) - (l-1)
 *
 * CONTAR pares por XOR (p. ej. "cuántos a[j], j en [l, r], cumplen a[i] XOR a[j] > M"):
 *   long c = t.countXorGreater(pre[l - 1], pre[r], a[i], M);   // también countXorLess (XOR < K)
 *
 * VARIOS CASOS DE PRUEBA: crea el trie UNA vez y llama t.reset() al empezar cada caso (no reasigna memoria).
 * Con una sola versión: t.maxXor(0, v, x) mira todos los números de la versión v.
 * Las raíces son ids de nodo (no hay tabla de versiones): guárdalas tú como en el ejemplo.
 */
public class PersistentBinaryTrie {
    public static final int BITS = 30; // bits por defecto: valores < 2^30 (~1e9); usa 60 para long hasta ~1e18
    public final int bits;

    public PersistentBinaryTrie() { this(BITS); }

    /** bits = cantidad de bits de los valores (2^bits > valor máximo). */
    public PersistentBinaryTrie(int bits) { this.bits = bits; }

    int[][] ch = new int[2][1 << 20];
    int[] cnt = new int[1 << 20];
    int nodes = 0; // el nodo 0 es el nulo (= versión vacía)

    /** Olvida todas las versiones (conserva la memoria reservada). Las raíces anteriores dejan de ser válidas. */
    public void reset() { nodes = 0; }

    private int clone(int from) {
        if (++nodes == cnt.length) {
            int cap = cnt.length * 2;
            ch[0] = Arrays.copyOf(ch[0], cap);
            ch[1] = Arrays.copyOf(ch[1], cap);
            cnt = Arrays.copyOf(cnt, cap);
        }
        ch[0][nodes] = ch[0][from];
        ch[1][nodes] = ch[1][from];
        cnt[nodes] = cnt[from];
        return nodes;
    }

    /** Nueva raíz = versión root con x agregado. */
    public int insert(int root, long x) { return change(root, x, +1); }

    /** Nueva raíz = versión root sin UNA ocurrencia de x (x debe existir: verifica con count). */
    public int erase(int root, long x) { return change(root, x, -1); }

    private int change(int root, long x, int d) {
        int newRoot = clone(root), cur = newRoot;
        cnt[cur] += d;
        for (int b = bits - 1; b >= 0; b--) {
            int bit = (int) (x >> b & 1);
            int nxt = clone(ch[bit][cur]);
            ch[bit][cur] = nxt;
            cur = nxt;
            cnt[cur] += d;
        }
        return newRoot;
    }

    public int size(int root) { return cnt[root]; }

    /** Veces que aparece x en la versión root. */
    public int count(int root, long x) {
        int cur = root;
        for (int b = bits - 1; b >= 0 && cur != 0; b--) cur = ch[(int) (x >> b & 1)][cur];
        return cur == 0 ? 0 : cnt[cur];
    }

    /** max(x XOR y) sobre los y en (versión hi) - (versión lo). -1 si no hay elementos. */
    public long maxXor(int lo, int hi, long x) {
        if (cnt[hi] - cnt[lo] <= 0) return -1;
        long res = 0;
        for (int b = bits - 1; b >= 0; b--) {
            int want = (int) (x >> b & 1) ^ 1;
            if (cnt[ch[want][hi]] - cnt[ch[want][lo]] > 0) {
                res |= 1L << b;
                hi = ch[want][hi]; lo = ch[want][lo];
            } else {
                hi = ch[want ^ 1][hi]; lo = ch[want ^ 1][lo];
            }
        }
        return res;
    }

    /** min(x XOR y) sobre los y en (versión hi) - (versión lo). -1 si no hay elementos. */
    public long minXor(int lo, int hi, long x) {
        if (cnt[hi] - cnt[lo] <= 0) return -1;
        long res = 0;
        for (int b = bits - 1; b >= 0; b--) {
            int want = (int) (x >> b & 1);
            if (cnt[ch[want][hi]] - cnt[ch[want][lo]] > 0) {
                hi = ch[want][hi]; lo = ch[want][lo];
            } else {
                res |= 1L << b;
                hi = ch[want ^ 1][hi]; lo = ch[want ^ 1][lo];
            }
        }
        return res;
    }

    /** Cantidad de y en (versión hi) - (versión lo) con (x XOR y) > k. O(bits). */
    public long countXorGreater(int lo, int hi, long x, long k) {
        if (k < 0) return cnt[hi] - cnt[lo];
        if (k >= (1L << bits) - 1) return 0; // ningún XOR de `bits` bits supera k
        long res = 0;
        for (int b = bits - 1; b >= 0 && hi != lo; b--) {
            int xb = (int) (x >> b & 1), kb = (int) (k >> b & 1);
            if (kb == 0) { // si el bit del XOR es 1, ya supera a k
                res += cnt[ch[xb ^ 1][hi]] - cnt[ch[xb ^ 1][lo]];
                hi = ch[xb][hi]; lo = ch[xb][lo]; // sigo por el bit de XOR = 0 (empata con k)
            } else { // solo el bit de XOR = 1 empata; con 0 el XOR queda menor
                hi = ch[xb ^ 1][hi]; lo = ch[xb ^ 1][lo];
            }
        }
        return res;
    }

    /** Cantidad de y en (versión hi) - (versión lo) con (x XOR y) < k. O(bits). */
    public long countXorLess(int lo, int hi, long x, long k) {
        if (k <= 0) return 0;
        if (k > (1L << bits) - 1) return cnt[hi] - cnt[lo];
        long res = 0;
        for (int b = bits - 1; b >= 0 && hi != lo; b--) {
            int xb = (int) (x >> b & 1), kb = (int) (k >> b & 1);
            if (kb == 1) { // si el bit del XOR es 0, ya es menor que k
                res += cnt[ch[xb][hi]] - cnt[ch[xb][lo]];
                hi = ch[xb ^ 1][hi]; lo = ch[xb ^ 1][lo]; // sigo por el bit de XOR = 1 (empata con k)
            } else {
                hi = ch[xb][hi]; lo = ch[xb][lo];
            }
        }
        return res;
    }

    /** k-ésimo menor (1-indexado) en (versión hi) - (versión lo). -1 si hay menos de k. */
    public long kth(int lo, int hi, int k) {
        if (cnt[hi] - cnt[lo] < k) return -1;
        long res = 0;
        for (int b = bits - 1; b >= 0; b--) {
            int zeros = cnt[ch[0][hi]] - cnt[ch[0][lo]];
            if (k <= zeros) { hi = ch[0][hi]; lo = ch[0][lo]; }
            else { k -= zeros; res |= 1L << b; hi = ch[1][hi]; lo = ch[1][lo]; }
        }
        return res;
    }
}
