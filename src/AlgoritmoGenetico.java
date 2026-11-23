import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
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
 *
 * Además, mientras trabaja, el AG va "anotando" por escrito todo lo que hace
 * (ruleta, cruzas, mutaciones, reducción y convergencia). Se guardan las primeras
 * y las últimas 5 generaciones para mostrarlas en la ventana de detalle.
 */
public class AlgoritmoGenetico {

    /** todo lo que pasó en una generación, escrito como texto. */
    public static class DetalleGeneracion {
        public final int numero;
        public final String texto;

        public DetalleGeneracion(int numero, String texto) {
            this.numero = numero;
            this.texto = texto;
        }
    }

    private static final int GENERACIONES_A_MOSTRAR = 5;

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


    private final StringBuilder bitacora = new StringBuilder();           // una línea por generación
    private StringBuilder detalle = new StringBuilder();                  // todo lo de la generación actual
    private final List<DetalleGeneracion> primeras = new ArrayList<>();   // generaciones 1 a 5
    private final LinkedList<DetalleGeneracion> ultimas = new LinkedList<>(); // las 5 más recientes
    private String textoFinal = "";

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
            generacion++;
            detalle = new StringBuilder();
            anotar("======================== GENERACIÓN %d ========================", generacion);

            List<int[]> hijos = producirHijos(generacion);
            poblacion = reduccionElitista(poblacion, hijos);
            anotarConvergencia(generacion);

            guardarDetalle(generacion);
            registrarGeneracion(generacion);
        }

        generacionesUsadas = generacion;
        textoFinal = crearTextoFinal();
        return poblacion.get(0);
    }

    private List<int[]> producirHijos(int generacion) {
        double[] aptitudes = calcularAptitudes(poblacion);
        double suma = sumar(aptitudes);

        anotar("");
        anotar("1) POBLACIÓN DE ENTRADA Y RULETA");
        if (generacion == 1) {
            anotar("   (es la población inicial, creada al azar)");
        }
        anotarTablaRuleta(aptitudes, suma);

        anotar("");
        anotar("2) REPRODUCCIÓN: %d parejas -> %d hijos", tamPoblacion / 2, (tamPoblacion / 2) * 2);

        List<int[]> hijos = new ArrayList<>();
        for (int i = 0; i < tamPoblacion / 2; i++) {
            anotar("");
            anotar("--- Pareja %d de %d ---", i + 1, tamPoblacion / 2);

            int[] padre1 = poblacion.get(seleccionarPorRuleta(aptitudes, suma, "Padre 1"));
            int[] padre2 = poblacion.get(seleccionarPorRuleta(aptitudes, suma, "Padre 2"));

            int[] hijo1 = padre1.clone();
            int[] hijo2 = padre2.clone();

            double numeroCruza = azar.nextDouble();
            if (numeroCruza < probCruza) {
                anotar("  Cruza: aleatorio %.4f < %.2f -> SÍ hay cruza (PMX)", numeroCruza, probCruza);
                int[] cortes = elegirPuntosDeCorte();
                anotarInicioPMX(padre1, padre2, cortes[0], cortes[1]);

                anotar("    Hijo 1 = tramo del Padre 2 + resto del Padre 1");
                hijo1 = cruzaPMX(padre1, padre2, cortes[0], cortes[1]);
                anotar("    Hijo 2 = tramo del Padre 1 + resto del Padre 2");
                hijo2 = cruzaPMX(padre2, padre1, cortes[0], cortes[1]);
            } else {
                anotar("  Cruza: aleatorio %.4f >= %.2f -> NO hay cruza, los hijos son copias de los padres",
                        numeroCruza, probCruza);
            }

            intentarMutar(hijo1, "Hijo 1");
            intentarMutar(hijo2, "Hijo 2");

            anotar("  Hijos finales: Hijo 1 = %s (dist %.2f)  |  Hijo 2 = %s (dist %.2f)",
                    rutaATexto(hijo1), distanciaTotal(hijo1), rutaATexto(hijo2), distanciaTotal(hijo2));

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

    private int seleccionarPorRuleta(double[] aptitudes, double suma, String nombre) {
        double giro = azar.nextDouble() * suma;
        double acumulado = 0;
        int elegido = aptitudes.length - 1; // por si el redondeo deja el giro al final

        for (int i = 0; i < aptitudes.length; i++) {
            acumulado += aptitudes[i];
            if (giro <= acumulado) {
                elegido = i;
                break;
            }
        }

        int[] ruta = poblacion.get(elegido);
        anotar("  %s: giro de la ruleta = %.2f%% -> cae en #%d  %s  (dist %.2f)",
                nombre, giro / suma * 100, elegido + 1, rutaATexto(ruta), distanciaTotal(ruta));
        return elegido;
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
        int[] hijo = new int[n]; // los 0 son posiciones vacías

        for (int i = corte1; i < corte2; i++) {
            hijo[i] = padreB[i];
        }
        anotar("      Se copia el tramo:      %s", marcarTramo(hijo, corte1, corte2, "| ", " |"));

        for (int i = 0; i < n; i++) {
            if (i >= corte1 && i < corte2) {
                continue;
            }
            int gen = padreA[i];
            String cadena = String.valueOf(gen);
            int posEnTramo = buscarEnTramo(padreB, gen, corte1, corte2);
            while (posEnTramo != -1) {
                gen = padreA[posEnTramo];
                cadena += " -> " + gen;
                posEnTramo = buscarEnTramo(padreB, gen, corte1, corte2);
            }
            if (gen != padreA[i]) {
                anotar("      Posición %d: el %d ya está en el tramo; correspondencia %s -> se pone %d",
                        i + 1, padreA[i], cadena, gen);
            }
            hijo[i] = gen;
        }
        anotar("      Resultado:              %s", marcarTramo(hijo, corte1, corte2, "| ", " |"));
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

    /** decide con un número al azar si el hijo muta o no. */
    private void intentarMutar(int[] hijo, String nombre) {
        double numeroMutacion = azar.nextDouble();
        if (numeroMutacion < probMutacion) {
            anotar("  Mutación del %s: aleatorio %.4f < %.2f -> SÍ muta (DM)", nombre, numeroMutacion, probMutacion);
            mutacionDM(hijo);
        } else {
            anotar("  Mutación del %s: aleatorio %.4f >= %.2f -> no muta", nombre, numeroMutacion, probMutacion);
        }
    }

    private void mutacionDM(int[] ruta) {
        int[] antes = ruta.clone();
        List<Integer> lista = aLista(ruta);

        int desde = azar.nextInt(lista.size());
        int hasta = desde + azar.nextInt(lista.size() - desde);
        anotar("      Antes:             %s", marcarTramo(ruta, desde, hasta + 1, "[", "]"));

        List<Integer> tramo = new ArrayList<>(lista.subList(desde, hasta + 1));
        lista.subList(desde, hasta + 1).clear();
        anotar("      Se saca el tramo:  %s", rutaATexto(aArreglo(lista)).replace("-", " "));

        int posicion = azar.nextInt(lista.size() + 1);
        lista.addAll(posicion, tramo);

        for (int i = 0; i < ruta.length; i++) {
            ruta[i] = lista.get(i);
        }
        anotar("      Se inserta en la posición %d: %s", posicion + 1,
                marcarTramo(ruta, posicion, posicion + tramo.size(), "[", "]"));
        if (Arrays.equals(antes, ruta)) {
            anotar("      (el tramo cayó en su mismo lugar, así que la ruta quedó igual)");
        }
    }
    
    private List<int[]> reduccionElitista(List<int[]> padres, List<int[]> hijos) {
        List<int[]> todos = new ArrayList<>(padres);
        todos.addAll(hijos);
        ordenarDeMejorAPeor(todos);
        List<int[]> sobrevivientes = new ArrayList<>(todos.subList(0, tamPoblacion));

        anotarReduccion(todos, hijos);
        return sobrevivientes;
    }

    private double calcularConvergencia() {
        return (double) contarIgualesAlMejor() / poblacion.size();
    }

    private int contarIgualesAlMejor() {
        int[] mejor = poblacion.get(0);
        int iguales = 0;
        for (int[] individuo : poblacion) {
            if (Arrays.equals(individuo, mejor)) {
                iguales++;
            }
        }
        return iguales;
    }

    private void ordenarDeMejorAPeor(List<int[]> individuos) {
        individuos.sort(Comparator.comparingDouble(this::distanciaTotal));
    }

    //  Anotaciones (el texto que se ve en la ventana de detalle)
    /** Agrega una línea al detalle de la generacion actual. . */
    private void anotar(String formato, Object... datos) {
        detalle.append(String.format(formato, datos)).append('\n');
    }

    /** tabla con cada ruta de la poblacion y la parte de la ruleta que le toca. */
    private void anotarTablaRuleta(double[] aptitudes, double suma) {
        int ancho = Math.max(4, rutaATexto(poblacion.get(0)).length());
        String formatoFila = "  %4s  %-" + ancho + "s  %9s  %9s  %8s  %s";

        anotar(formatoFila, "#", "Ruta", "Distancia", "Aptitud", "% ruleta", "Rango en la ruleta");
        double acumulado = 0;
        for (int i = 0; i < poblacion.size(); i++) {
            int[] ruta = poblacion.get(i);
            double porcentaje = aptitudes[i] / suma * 100;
            anotar(formatoFila, i + 1, rutaATexto(ruta), String.format("%.2f", distanciaTotal(ruta)),
                    String.format("%.6f", aptitudes[i]), String.format("%.2f%%", porcentaje),
                    String.format("%6.2f%% a %6.2f%%", acumulado, acumulado + porcentaje));
            acumulado += porcentaje;
        }
        anotar("  Suma de aptitudes = %.6f  (aptitud = 1 / distancia)", suma);
    }

    private void anotarInicioPMX(int[] padre1, int[] padre2, int corte1, int corte2) {
        anotar("    Puntos de corte: el tramo va de la posición %d a la %d", corte1 + 1, corte2);
        anotar("    Padre 1:  %s", marcarTramo(padre1, corte1, corte2, "| ", " |"));
        anotar("    Padre 2:  %s", marcarTramo(padre2, corte1, corte2, "| ", " |"));

        StringBuilder correspondencias = new StringBuilder();
        for (int i = corte1; i < corte2; i++) {
            correspondencias.append(padre2[i]).append("<->").append(padre1[i]).append("   ");
        }
        anotar("    Correspondencias: %s", correspondencias.toString().trim());
    }

    private void anotarReduccion(List<int[]> todos, List<int[]> hijos) {
        anotar("");
        anotar("3) REDUCCIÓN ELITISTA");
        anotar("  Se juntan %d padres + %d hijos = %d rutas y se ordenan de menor a mayor distancia.",
                tamPoblacion, hijos.size(), todos.size());
        anotar("  Sobreviven las %d mejores:", tamPoblacion);

        int ancho = Math.max(4, rutaATexto(todos.get(0)).length());
        String formatoFila = "  %4s  %-" + ancho + "s  %9s  %-6s  %s";
        anotar(formatoFila, "#", "Ruta", "Distancia", "Origen", "Resultado");

        int hijosQueSobreviven = 0;
        for (int i = 0; i < todos.size(); i++) {
            int[] ruta = todos.get(i);
            boolean esHijo = contieneElMismoArreglo(hijos, ruta);
            boolean sobrevive = i < tamPoblacion;
            if (esHijo && sobrevive) {
                hijosQueSobreviven++;
            }
            anotar(formatoFila, i + 1, rutaATexto(ruta), String.format("%.2f", distanciaTotal(ruta)),
                    esHijo ? "hijo" : "padre", sobrevive ? "sobrevive" : "eliminada");
        }
        anotar("  Resumen: sobreviven %d padres y %d hijos.",
                tamPoblacion - hijosQueSobreviven, hijosQueSobreviven);
    }

    private void anotarConvergencia(int generacion) {
        double convergencia = calcularConvergencia();
        anotar("");
        anotar("4) CONVERGENCIA");
        anotar("  Mejor ruta: %s (dist %.2f)", rutaATexto(poblacion.get(0)), distanciaTotal(poblacion.get(0)));
        anotar("  %d de %d rutas son iguales a la mejor = %.0f%%",
                contarIgualesAlMejor(), poblacion.size(), convergencia * 100);

        if (convergencia >= convergenciaParo) {
            anotar("  %.0f%% >= %.0f%% -> la población CONVERGIÓ, el AG se detiene.",
                    convergencia * 100, convergenciaParo * 100);
        } else if (generacion >= maxGeneraciones) {
            anotar("  Se llegó al máximo de %d generaciones -> el AG se detiene.", maxGeneraciones);
        } else {
            anotar("  %.0f%% < %.0f%% -> todavía no converge, sigue a la generación %d.",
                    convergencia * 100, convergenciaParo * 100, generacion + 1);
        }
    }

    /** guarda el detalle de la generación siempre las 5 primeras, y solo las 5 mas recientes del resto */
    private void guardarDetalle(int generacion) {
        DetalleGeneracion d = new DetalleGeneracion(generacion, detalle.toString());
        if (generacion <= GENERACIONES_A_MOSTRAR) {
            primeras.add(d);
        } else {
            ultimas.addLast(d);
            if (ultimas.size() > GENERACIONES_A_MOSTRAR) {
                ultimas.removeFirst(); // se descarta la más vieja
            }
        }
    }

    private String crearTextoFinal() {
        detalle = new StringBuilder();
        anotar("======================== POBLACIÓN FINAL (generación %d) ========================",
                generacionesUsadas);
        if (calcularConvergencia() >= convergenciaParo) {
            anotar("Motivo de paro: la población convergió (%.0f%% >= %.0f%%).",
                    calcularConvergencia() * 100, convergenciaParo * 100);
        } else {
            anotar("Motivo de paro: se llegó al máximo de %d generaciones (convergencia %.0f%%).",
                    maxGeneraciones, calcularConvergencia() * 100);
        }
        anotar("");

        int[] mejor = poblacion.get(0);
        int ancho = Math.max(4, rutaATexto(mejor).length());
        String formatoFila = "  %4s  %-" + ancho + "s  %9s  %s";
        anotar(formatoFila, "#", "Ruta", "Distancia", "");
        for (int i = 0; i < poblacion.size(); i++) {
            int[] ruta = poblacion.get(i);
            anotar(formatoFila, i + 1, rutaATexto(ruta), String.format("%.2f", distanciaTotal(ruta)),
                    Arrays.equals(ruta, mejor) ? "= mejor" : "");
        }
        anotar("");
        anotar("MEJOR RUTA: Inicio-%s-Inicio   Distancia: %.2f", rutaATexto(mejor), distanciaTotal(mejor));
        return detalle.toString();
    }

    private void registrarGeneracion(int generacion) {
        bitacora.append(String.format("Gen %3d | mejor: %6.2f | conv: %3.0f%%%n",
                generacion, distanciaTotal(poblacion.get(0)), calcularConvergencia() * 100));
    }

    //  Resultados (los usa la interfaz)
   

    public String getBitacora() {
        return bitacora.toString();
    }

    public int getGeneracionesUsadas() {
        return generacionesUsadas;
    }

    public double getConvergenciaFinal() {
        return calcularConvergencia();
    }

    /** detalle de las primeras 5 y las últimas 5 generaciones, en orden */
    public List<DetalleGeneracion> getDetalles() {
        List<DetalleGeneracion> todas = new ArrayList<>(primeras);
        todas.addAll(ultimas);
        return todas;
    }

    public String getTextoFinal() {
        return textoFinal;
    }

    /** texto con los parámetros usados y qué generaciones se muestran. */
    public String getResumen() {
        StringBuilder texto = new StringBuilder();
        texto.append("PARÁMETROS\n");
        texto.append(String.format("  Nodos a visitar:      %d%n", nodos.size()));
        texto.append(String.format("  Tamaño de población:  %d%n", tamPoblacion));
        texto.append(String.format("  %% de cruza (PMX):     %.0f%%%n", probCruza * 100));
        texto.append(String.format("  %% de mutación (DM):   %.0f%%%n", probMutacion * 100));
        texto.append(String.format("  Máx. generaciones:    %d%n", maxGeneraciones));
        texto.append(String.format("  %% de convergencia:    %.0f%%%n", convergenciaParo * 100));

        texto.append("\nRESULTADO\n");
        texto.append(String.format("  Mejor ruta: Inicio-%s-Inicio%n", rutaATexto(poblacion.get(0))));
        texto.append(String.format("  Distancia:  %.2f%n", distanciaTotal(poblacion.get(0))));
        texto.append(String.format("  Generaciones usadas: %d%n", generacionesUsadas));

        texto.append("\nQUÉ SE MUESTRA EN LAS PESTAÑAS\n");
        List<DetalleGeneracion> detalles = getDetalles();
        if (detalles.isEmpty()) {
            texto.append("  La población inicial ya cumplía la convergencia; no hubo generaciones.\n");
        } else {
            texto.append("  Generaciones con detalle: ");
            for (DetalleGeneracion d : detalles) {
                texto.append(d.numero).append(" ");
            }
            texto.append("\n");
            if (generacionesUsadas > 2 * GENERACIONES_A_MOSTRAR) {
                texto.append(String.format("  (las generaciones %d a %d no se muestran)%n",
                        GENERACIONES_A_MOSTRAR + 1, generacionesUsadas - GENERACIONES_A_MOSTRAR));
            }
        }
        texto.append("  Cada pestaña tiene: 1) población de entrada y ruleta, 2) selección, cruza y\n");
        texto.append("  mutación de cada pareja, 3) reducción elitista, 4) convergencia.\n");
        texto.append("  La pestaña 'Final' muestra la población con la que terminó el AG.\n");

        texto.append("\nEVOLUCIÓN (todas las generaciones)\n");
        texto.append(bitacora);
        return texto.toString();
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

    /**
     * Ruta con un tramo marcado
     */
    private static String marcarTramo(int[] ruta, int desde, int hasta, String abre, String cierra) {
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < ruta.length; i++) {
            if (i == desde) {
                texto.append(abre);
            }
            texto.append(ruta[i] == 0 ? "_" : String.valueOf(ruta[i]));
            if (i == hasta - 1) {
                texto.append(cierra);
            }
            texto.append(' ');
        }
        return texto.toString().trim();
    }

    private static boolean contieneElMismoArreglo(List<int[]> lista, int[] arreglo) {
        for (int[] elemento : lista) {
            if (elemento == arreglo) { // el mismo objeto, no solo el mismo contenido
                return true;
            }
        }
        return false;
    }

    private static double sumar(double[] valores) {
        double suma = 0;
        for (double v : valores) {
            suma += v;
        }
        return suma;
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
