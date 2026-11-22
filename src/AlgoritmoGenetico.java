import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Algoritmo Genético para el TSP, siguiendo la teoría vista en clase:
 *
 *  - Representación basada en la trayectoria: un individuo (cromosoma) es el orden
 *    en que se visitan los nodos, por ejemplo [2, 7, 3, 1, 6, 4, 5].
 *    El inicio NO va en el cromosoma: toda ruta sale del inicio y regresa a él.
 *  - Población inicial al azar.
 *  - Aptitud = 1 / distancia total (rutas más cortas = más aptas).
 *  - Selección por ruleta (proporcional a la aptitud).
 *  - Cruza PMX (correspondencia parcial).
 *  - Mutación DM (desplazamiento).
 *  - Reducción elitista: de padres + hijos sobreviven los mejores.
 *  - Paro: cuando la población converge, o al llegar al máximo de generaciones.
 */
public class AlgoritmoGenetico {

    // ---------- Datos del problema ----------
    private final Nodo inicio;
    private final List<Nodo> nodos; // nodos.get(0) es el nodo 1, nodos.get(1) el nodo 2, ...

    // ---------- Parámetros del AG ----------
    private final int tamPoblacion;
    private final double probCruza;         // de 0 a 1  (ej. 0.80)
    private final double probMutacion;      // de 0 a 1  (ej. 0.05)
    private final int maxGeneraciones;
    private final double convergenciaParo;  // de 0 a 1  (ej. 0.90)

    // ---------- Estado ----------
    private final Random azar = new Random();
    private List<int[]> poblacion;
    private int generacionesUsadas;
    private final StringBuilder bitacora = new StringBuilder();

    public AlgoritmoGenetico(Nodo inicio, List<Nodo> nodos, int tamPoblacion, double probCruza,
                             double probMutacion, int maxGeneraciones, double convergenciaParo) {
        this.inicio = inicio;
        this.nodos = nodos;
        this.tamPoblacion = tamPoblacion;
        this.probCruza = probCruza;
        this.probMutacion = probMutacion;
        this.maxGeneraciones = maxGeneraciones;
        this.convergenciaParo = convergenciaParo;
    }

    // =====================================================================
    //  Ciclo principal
    // =====================================================================

    /** Ejecuta el AG y devuelve la mejor ruta encontrada. */
    public int[] ejecutar() {
        poblacion = crearPoblacionInicial();
        ordenarDeMejorAPeor(poblacion);
        registrarGeneracion(0);

        int generacion = 0;
        while (generacion < maxGeneraciones && calcularConvergencia() < convergenciaParo) {
            List<int[]> hijos = producirHijos();
            poblacion = reduccionElitista(poblacion, hijos);
            generacion++;
            registrarGeneracion(generacion);
        }

        generacionesUsadas = generacion;
        return poblacion.get(0); // tras ordenar, el primero es el mejor
    }

    /**
     * Ciclo reproductivo: se repite Tamaño(Población)/2 veces y cada vuelta produce 2 hijos.
     * Seleccionar 2 padres -> cruzar (con cierta probabilidad) -> mutar (con cierta probabilidad).
     */
    private List<int[]> producirHijos() {
        double[] aptitudes = calcularAptitudes(poblacion);
        List<int[]> hijos = new ArrayList<>();

        for (int i = 0; i < tamPoblacion / 2; i++) {
            int[] padre1 = seleccionarPorRuleta(aptitudes);
            int[] padre2 = seleccionarPorRuleta(aptitudes);

            // Si no hay cruza, los hijos son copias de los padres
            int[] hijo1 = padre1.clone();
            int[] hijo2 = padre2.clone();

            if (azar.nextDouble() < probCruza) {
                int[] cortes = elegirPuntosDeCorte();
                hijo1 = cruzaPMX(padre1, padre2, cortes[0], cortes[1]);
                hijo2 = cruzaPMX(padre2, padre1, cortes[0], cortes[1]);
            }

            if (azar.nextDouble() < probMutacion) {
                mutacionDM(hijo1);
            }
            if (azar.nextDouble() < probMutacion) {
                mutacionDM(hijo2);
            }

            hijos.add(hijo1);
            hijos.add(hijo2);
        }
        return hijos;
    }

    // =====================================================================
    //  Población inicial
    // =====================================================================

    /** Crea individuos al azar: cada uno es una permutación aleatoria de los nodos 1..n. */
    private List<int[]> crearPoblacionInicial() {
        List<int[]> nueva = new ArrayList<>();
        for (int i = 0; i < tamPoblacion; i++) {
            List<Integer> orden = new ArrayList<>();
            for (int id = 1; id <= nodos.size(); id++) {
                orden.add(id);
            }
            Collections.shuffle(orden, azar);
            nueva.add(aArreglo(orden));
        }
        return nueva;
    }

    // =====================================================================
    //  Función objetivo
    // =====================================================================

    /** Distancia de la gira completa: inicio -> nodos en orden -> regreso al inicio. */
    public double distanciaTotal(int[] ruta) {
        double total = 0;
        Nodo actual = inicio;
        for (int id : ruta) {
            Nodo siguiente = nodos.get(id - 1);
            total += actual.distanciaA(siguiente);
            actual = siguiente;
        }
        total += actual.distanciaA(inicio); // regreso al punto de partida
        return total;
    }

    /** Aptitud: mientras más corta la ruta, mayor la aptitud. */
    private double aptitud(int[] ruta) {
        return 1.0 / distanciaTotal(ruta);
    }

    private double[] calcularAptitudes(List<int[]> individuos) {
        double[] aptitudes = new double[individuos.size()];
        for (int i = 0; i < individuos.size(); i++) {
            aptitudes[i] = aptitud(individuos.get(i));
        }
        return aptitudes;
    }

    // =====================================================================
    //  Selección: ruleta
    // =====================================================================

    /**
     * Cada individuo ocupa en la ruleta un área proporcional a su aptitud.
     * Se "gira" la ruleta con un número al azar y se devuelve el individuo donde cae.
     */
    private int[] seleccionarPorRuleta(double[] aptitudes) {
        double suma = 0;
        for (double a : aptitudes) {
            suma += a;
        }

        double giro = azar.nextDouble() * suma;
        double acumulado = 0;
        for (int i = 0; i < aptitudes.length; i++) {
            acumulado += aptitudes[i];
            if (giro <= acumulado) {
                return poblacion.get(i);
            }
        }
        return poblacion.get(poblacion.size() - 1); // por redondeo
    }

    // =====================================================================
    //  Cruza: PMX
    // =====================================================================

    /** Dos puntos de corte distintos entre 0 y n. El tramo es [corte1, corte2). */
    private int[] elegirPuntosDeCorte() {
        int n = nodos.size();
        int a = azar.nextInt(n + 1);
        int b = azar.nextInt(n + 1);
        while (a == b) {
            b = azar.nextInt(n + 1);
        }
        return new int[]{Math.min(a, b), Math.max(a, b)};
    }

    /**
     * Cruza PMX. El hijo recibe:
     *   1) el tramo central (entre los cortes) del padreB, y
     *   2) el resto de las posiciones del padreA.
     * Si un nodo del padreA ya está en el tramo copiado, se sustituye usando la
     * correspondencia entre los tramos (padreB[i] <-> padreA[i]) hasta que ya no se repita.
     *
     * Ejemplo de clase: A = (1 2 3 | 4 5 6 | 7 8), B = (3 7 5 | 1 6 8 | 2 4)
     *   cruzaPMX(A, B) = (4 2 3 1 6 8 7 5)
     *   cruzaPMX(B, A) = (3 7 8 4 5 6 2 1)
     */
    private int[] cruzaPMX(int[] padreA, int[] padreB, int corte1, int corte2) {
        int n = padreA.length;
        int[] hijo = new int[n];

        // 1) Copiar el tramo central del padreB
        for (int i = corte1; i < corte2; i++) {
            hijo[i] = padreB[i];
        }

        // 2) Llenar el resto con el padreA, resolviendo repetidos
        for (int i = 0; i < n; i++) {
            if (i >= corte1 && i < corte2) {
                continue; // esta posición ya se llenó con el tramo
            }
            int gen = padreA[i];
            int posEnTramo = buscarEnTramo(padreB, gen, corte1, corte2);
            while (posEnTramo != -1) {
                gen = padreA[posEnTramo]; // aplicar la correspondencia
                posEnTramo = buscarEnTramo(padreB, gen, corte1, corte2);
            }
            hijo[i] = gen;
        }
        return hijo;
    }

    /** Posición de 'gen' dentro del tramo [corte1, corte2) de 'ruta', o -1 si no está. */
    private int buscarEnTramo(int[] ruta, int gen, int corte1, int corte2) {
        for (int i = corte1; i < corte2; i++) {
            if (ruta[i] == gen) {
                return i;
            }
        }
        return -1;
    }

    // =====================================================================
    //  Mutación: DM (desplazamiento)
    // =====================================================================

    /**
     * Se saca un tramo al azar de la ruta y se inserta en otra posición al azar.
     * Ejemplo de clase: (1 2 3 4 5 6 7 8), tramo (3 4 5) -> (1 2 6 7 3 4 5 8)
     */
    private void mutacionDM(int[] ruta) {
        List<Integer> lista = aLista(ruta);

        // Elegir el tramo [desde, hasta]
        int desde = azar.nextInt(lista.size());
        int hasta = desde + azar.nextInt(lista.size() - desde);

        // Sacarlo de la ruta
        List<Integer> tramo = new ArrayList<>(lista.subList(desde, hasta + 1));
        lista.subList(desde, hasta + 1).clear();

        // Insertarlo en una posición al azar
        int posicion = azar.nextInt(lista.size() + 1);
        lista.addAll(posicion, tramo);

        // Copiar el resultado de vuelta en el arreglo original
        for (int i = 0; i < ruta.length; i++) {
            ruta[i] = lista.get(i);
        }
    }

    // =====================================================================
    //  Reducción y convergencia
    // =====================================================================

    /** Reducción elitista: se juntan padres e hijos y se quedan los 'tamPoblacion' mejores. */
    private List<int[]> reduccionElitista(List<int[]> padres, List<int[]> hijos) {
        List<int[]> todos = new ArrayList<>(padres);
        todos.addAll(hijos);
        ordenarDeMejorAPeor(todos);
        return new ArrayList<>(todos.subList(0, tamPoblacion));
    }

    /** Porcentaje (0 a 1) de la población que es idéntica al mejor individuo. */
    private double calcularConvergencia() {
        int[] mejor = poblacion.get(0);
        int iguales = 0;
        for (int[] individuo : poblacion) {
            if (Arrays.equals(individuo, mejor)) {
                iguales++;
            }
        }
        return (double) iguales / poblacion.size();
    }

    private void ordenarDeMejorAPeor(List<int[]> individuos) {
        individuos.sort(Comparator.comparingDouble(this::distanciaTotal));
    }

    // =====================================================================
    //  Resultados
    // =====================================================================

    private void registrarGeneracion(int generacion) {
        bitacora.append(String.format("Gen %3d | mejor: %6.2f | conv: %3.0f%%%n",
                generacion, distanciaTotal(poblacion.get(0)), calcularConvergencia() * 100));
    }

    public String getBitacora() {
        return bitacora.toString();
    }

    public int getGeneracionesUsadas() {
        return generacionesUsadas;
    }

    public double getConvergenciaFinal() {
        return calcularConvergencia();
    }

    /** Convierte una ruta a texto, por ejemplo "2-7-3-1-6-4-5". */
    public static String rutaATexto(int[] ruta) {
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < ruta.length; i++) {
            if (i > 0) {
                texto.append("-");
            }
            texto.append(ruta[i]);
        }
        return texto.toString();
    }

    // ---------- Utilidades ----------

    private static int[] aArreglo(List<Integer> lista) {
        int[] arreglo = new int[lista.size()];
        for (int i = 0; i < lista.size(); i++) {
            arreglo[i] = lista.get(i);
        }
        return arreglo;
    }

    private static List<Integer> aLista(int[] arreglo) {
        List<Integer> lista = new ArrayList<>();
        for (int valor : arreglo) {
            lista.add(valor);
        }
        return lista;
    }
}
