import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

class Ascensor implements Runnable {
    private int id;
    private int pisoActual;
    private BlockingQueue<Solicitud> solicitudes;
    private List<Integer> lugaresDeDetencion;
    private int pisoObjetivo;

    public Ascensor(int id, BlockingQueue<Solicitud> solicitudes, List<Integer> lugaresDeDetencion) {
        this.id = id;
        this.pisoActual = 0;
        this.solicitudes = solicitudes;
        this.lugaresDeDetencion = lugaresDeDetencion;
        this.pisoObjetivo = 0;
    }

    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Solicitud solicitud = solicitudes.take();
                int pisoDestino = solicitud.getPisoDestino();
                 
        if (lugaresDeDetencion.contains(pisoActual)) {
            System.out.println("Ascensor " + id + " Cumplio con una solicitud de paso en en piso" + pisoActual);
        } 
                System.out.println("Ascensor " + id + " está en el piso " + pisoActual);
                moverAscensor(pisoDestino);
                System.out.println("Ascensor " + id + " ha llegado al piso " + pisoActual);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void moverAscensor(int pisoDestino) throws InterruptedException {
        int direccion = (pisoDestino - pisoActual) / Math.abs(pisoDestino - pisoActual);
        while (pisoActual != pisoDestino) {
            pisoActual += direccion;
            System.out.println("Ascensor " + id + " se encuentra en el piso " + pisoActual);
            Thread.sleep(1000);
            if (lugaresDeDetencion.contains(pisoActual)) {
                System.out.println("Ascensor " + id + " se detiene en el piso " + pisoActual);
                lugaresDeDetencion.remove(Integer.valueOf(pisoActual)); // Eliminar el piso de detención
                Thread.sleep(1000);
            }
        }
    }

    public void setPisoObjetivo(int pisoObjetivo) {
        this.pisoObjetivo = pisoObjetivo;
    }

    public int getPisoActual() {
        return pisoActual;
    }

    public int getPisoObjetivo() {
        return pisoObjetivo;
    }
    public int getId(){
        return this.id;
    }
}

class AsignadorSolicitudes implements Runnable {
    private List<BlockingQueue<Solicitud>> colasAscensores;
    private Queue<Solicitud> solicitudesPendientes;
    private int tiempoActual;
    private Map<Integer, Ascensor> mapaAscensores;

    public AsignadorSolicitudes(List<BlockingQueue<Solicitud>> colasAscensores, Queue<Solicitud> solicitudesPendientes) {
        this.colasAscensores = colasAscensores;
        this.solicitudesPendientes = solicitudesPendientes;
        this.tiempoActual = 0;
        this.mapaAscensores = new HashMap<>();
    }

    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                while (!solicitudesPendientes.isEmpty() && solicitudesPendientes.peek().getTiempo() <= tiempoActual) {
                    Solicitud solicitud = solicitudesPendientes.poll();
                    int ascensorId = asignarAscensor(solicitud.getPisoDestino());
                    colasAscensores.get(ascensorId).put(solicitud);
                }
                tiempoActual++;
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private int asignarAscensor(int pisoDestino) {
        // Lógica de asignación personalizada
        // Aquí puedes implementar tu lógica para determinar a qué ascensor asignar la solicitud
        // En este ejemplo, se asigna de manera aleatoria
        List<Ascensor> ascensoresDisponibles = new ArrayList<>();
        for (Map.Entry<Integer, Ascensor> entry : mapaAscensores.entrySet()) {
            Ascensor ascensor = entry.getValue();
            if (ascensor.getPisoActual() <= pisoDestino && ascensor.getPisoObjetivo() >= pisoDestino) {
                ascensoresDisponibles.add(ascensor);
            }
        }
        if (!ascensoresDisponibles.isEmpty()) {
            ascensoresDisponibles.sort(Comparator.comparingInt(Ascensor::getPisoActual));
            Ascensor ascensorSeleccionado = ascensoresDisponibles.get(0);
            ascensorSeleccionado.setPisoObjetivo(pisoDestino);
            return ascensorSeleccionado.getId();
        } else {
            int ascensorId = (int) (Math.random() * colasAscensores.size());
            Ascensor ascensor = mapaAscensores.getOrDefault(ascensorId, null);
            if (ascensor == null) {
                ascensor = new Ascensor(ascensorId + 1, colasAscensores.get(ascensorId), new ArrayList<>());
                mapaAscensores.put(ascensorId, ascensor);
            }
            ascensor.setPisoObjetivo(pisoDestino);
            return ascensorId;
        }
    }
}

class Solicitud {
    private int pisoDestino;
    private int tiempo;

    public Solicitud(int pisoDestino, int tiempo) {
        this.pisoDestino = pisoDestino;
        this.tiempo = tiempo;
    }

    public int getPisoDestino() {
        return pisoDestino;
    }

    public int getTiempo() {
        return tiempo;
    }
}

public class SimuladorAscensores {
    public static void main(String[] args) {
        int numAscensores = 1;
        List<BlockingQueue<Solicitud>> colasAscensores = new ArrayList<>();
        Queue<Solicitud> solicitudesPendientes = new PriorityQueue<>(Comparator.comparingInt(Solicitud::getTiempo));
        Thread[] ascensores = new Thread[numAscensores];

        for (int i = 0; i < numAscensores; i++) {
            colasAscensores.add(new ArrayBlockingQueue<>(10));
            List<Integer> lugaresDeDetencion = new ArrayList<>();
            Ascensor ascensor = new Ascensor(i + 1, colasAscensores.get(i), lugaresDeDetencion);
            ascensores[i] = new Thread(ascensor);
            ascensores[i].start();
        }

        AsignadorSolicitudes asignadorSolicitudes = new AsignadorSolicitudes(colasAscensores, solicitudesPendientes);
        Thread hiloAsignador = new Thread(asignadorSolicitudes);
        hiloAsignador.start();

        String archivo = "instrucciones.txt";

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(",");
                int pisoDestino = Integer.parseInt(partes[0]);
                int tiempo = Integer.parseInt(partes[1]);
                Solicitud solicitud = new Solicitud(pisoDestino, tiempo);
                solicitudesPendientes.offer(solicitud);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

 
    }
}
