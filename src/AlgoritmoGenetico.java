import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Algoritmo Genético para el TSP:
 *
 *  - Representación basada en la trayectoria: un individuo (cromosoma) es el orden
 *    en que se visitan los nodos, por ejemplo [2, 7, 3, 1, 6, 4, 5].
 *  - El inicio NO va en el cromosoma: toda ruta sale del inicio y regresa a él.
 *  - Población inicial al azar.
 *  - Aptitud = 1 / distancia total (rutas más cortas = más aptas).
 *  - Selección por ruleta (proporcional a la aptitud).
 *  - Cruza PMX (correspondencia parcial).
 *  - Mutación DM (desplazamiento).
 *  - Reducción elitista: de padres + hijos sobreviven los mejores.
 *  - Paro: cuando la población converge, o al llegar al máximo de generaciones.
 */
public class AlgoritmoGenetico {

    private final Nodo inicio;
    private final List<Nodo> nodos;

    private final int tamPoblacion;
    private final double probCruza;         
    private final double probMutacion;      
    private final int maxGeneraciones;
    private final double convergenciaParo;  

    private final Random azar = new Random();
    private List<int[]> poblacion;
    private int generacionesUsadas;
    private final StringBuilder bitacora = new StringBuilder();

    public AlgoritmoGenetico(Nodo inicio, List<Nodo> nodos, int tamPoblacion, double probCruza, double probMutacion, int maxGeneraciones, double convergenciaParo) {
        this.inicio = inicio;
        this.nodos = nodos;
        this.tamPoblacion = tamPoblacion;
        this.probCruza = probCruza;
        this.probMutacion = probMutacion;
        this.maxGeneraciones = maxGeneraciones;
        this.convergenciaParo = convergenciaParo;
    }

    //  Ciclo principal
    
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
        return poblacion.get(0);
    }

    private List<int[]> producirHijos() {
        double[] aptitudes = calcularAptitudes(poblacion);
        List<int[]> hijos = new ArrayList<>();

        for (int i = 0; i < tamPoblacion / 2; i++) {
            int[] padre1 = seleccionarPorRuleta(aptitudes);
            int[] padre2 = seleccionarPorRuleta(aptitudes);

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
    
    public double distanciaTotal(int[] ruta) {
        double total = 0;
        Nodo actual = inicio;
        for (int id : ruta) {
            Nodo siguiente = nodos.get(id - 1);
            total += actual.distanciaA(siguiente);
            actual = siguiente;
        }
        total += actual.distanciaA(inicio);
        return total;
    }

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
        return poblacion.get(poblacion.size() - 1);
    }
    
    private int[] elegirPuntosDeCorte() {
        int n = nodos.size();
        int a = azar.nextInt(n + 1);
        int b = azar.nextInt(n + 1);
        while (a == b) {
            b = azar.nextInt(n + 1);
        }
        return new int[]{Math.min(a, b), Math.max(a, b)};
    }
    
    private int[] cruzaPMX(int[] padreA, int[] padreB, int corte1, int corte2) {
        int n = padreA.length;
        int[] hijo = new int[n];

        for (int i = corte1; i < corte2; i++) {
            hijo[i] = padreB[i];
        }

        for (int i = 0; i < n; i++) {
            if (i >= corte1 && i < corte2) {
                continue;
            }
            int gen = padreA[i];
            int posEnTramo = buscarEnTramo(padreB, gen, corte1, corte2);
            while (posEnTramo != -1) {
                gen = padreA[posEnTramo];
                posEnTramo = buscarEnTramo(padreB, gen, corte1, corte2);
            }
            hijo[i] = gen;
        }
        return hijo;
    }

    private int buscarEnTramo(int[] ruta, int gen, int corte1, int corte2) {
        for (int i = corte1; i < corte2; i++) {
            if (ruta[i] == gen) {
                return i;
            }
        }
        return -1;
    }
    
    private void mutacionDM(int[] ruta) {
        List<Integer> lista = aLista(ruta);

        int desde = azar.nextInt(lista.size());
        int hasta = desde + azar.nextInt(lista.size() - desde);

        List<Integer> tramo = new ArrayList<>(lista.subList(desde, hasta + 1));
        lista.subList(desde, hasta + 1).clear();

        int posicion = azar.nextInt(lista.size() + 1);
        lista.addAll(posicion, tramo);

        for (int i = 0; i < ruta.length; i++) {
            ruta[i] = lista.get(i);
        }
    }
    
    private List<int[]> reduccionElitista(List<int[]> padres, List<int[]> hijos) {
        List<int[]> todos = new ArrayList<>(padres);
        todos.addAll(hijos);
        ordenarDeMejorAPeor(todos);
        return new ArrayList<>(todos.subList(0, tamPoblacion));
    }

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
