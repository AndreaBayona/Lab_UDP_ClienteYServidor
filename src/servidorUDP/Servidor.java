package servidorUDP;

import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;


public class Servidor {

    //==================================================
    // DIRECCIONES
    //==================================================
    public static String RUTA_ARCHIVOS = "./data/multimedia/servidor/";
    public static String RUTA_NUMERO_PRUEBAS = "./data/logs/servidor/num.txt";
    private static String RUTA_LOGS = "./data/logs/servidor/";

    //==================================================
    // CONSTANTES
    //==================================================
    public int SOCKET_SERVIDOR = 8000;
    public static int TAMANIO_BUFFER = 2048;
    //==================================================
    // COMANDOS
    //==================================================
    public static String ARCHIVOS_RECIBIDOS = "RECIBIDOS";
    public static String INTEGRIDAD_OK = "OK";
    public static String INTEGRIDAD_ERROR = "ERROR";

    //==================================================
    // ATRIBUTOS
    //==================================================

    private Object[][] clientes;
    private DatagramSocket socket;
    private File file;
    private String hash;
    private int idPrueba;
    private int maxNumeroClientes;
    private String nombreArchivo;

    //==================================================
    // CONSTRUCTORES
    //==================================================

    public Servidor(String nombreArchivo, int maxNumeroClientes) {

        try {
            this.nombreArchivo = nombreArchivo;
            this.maxNumeroClientes = maxNumeroClientes;
            socket = new DatagramSocket(SOCKET_SERVIDOR);
            clientes = new Object[maxNumeroClientes][2];
            file = new File(RUTA_ARCHIVOS + nombreArchivo);
            hash = getFileHash(file);
            idPrueba = numeroPrueba(); //ID PARA EL NOMBRE DEL LOG
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //==================================================
    // METODOS
    //==================================================

    public void inciarServidor() throws IOException {
        byte[] buffer = new byte[0];
        DatagramPacket packet = null;

        //El servidor recibe los clientes y a cada uno le envia el tamanio del archivo
        //Si despues de 10 segundos no se conecta la cantidad esperada de clientes cierra la conexion
        recibirClientesYEnviarTamanioArchivo(buffer, packet);

        //El servidor envia los paquetes del archivo a cada cliente
        enviarPaquetesArchivosAClientes(buffer, packet);

        //Cuando el servidor recibe la confirmacion del archivo recibido, envia el hash
        enviarHashAClientesYConfirmarIntegridad(buffer, packet);

        //El servidor  recibe la confirmacion de integridad del archivo de cada cliente

    }

    private void recibirClientesYEnviarTamanioArchivo(byte[] buffer, DatagramPacket packet) throws IOException {
        int count = 0;
        InetAddress adressCliente = null;
        int portCliente = 0;
        buffer = new byte[256];

        System.out.println("SERVIDOR: recibiendo clientes...");
        socket.setSoTimeout(10000);
        while (count < maxNumeroClientes) {

            try {
                packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                System.out.println("SERVIDOR: paquete recibido " + packet.toString());
                adressCliente = packet.getAddress();
                portCliente = packet.getPort();
                System.out.println("SERVIDOR: address " + adressCliente.toString());
                System.out.println("SERVIDOR: port " + portCliente);
                clientes[count][0] = adressCliente;
                clientes[count][1] = portCliente;

                enviarDatagramaString(String.valueOf(file.length()), buffer, packet, adressCliente, portCliente);
                System.out.println("SERVIDOR: enviando tamanio archivo " + String.valueOf(file.length()));
                buffer = new byte[256];
                count++;
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
                socket.close();
                return;
            }
        }

        socket.setSoTimeout(0);
    }


    private void enviarPaquetesArchivosAClientes(byte[] buffer, DatagramPacket packet) throws IOException {

        int in;
        InetAddress adressCliente = null;
        int portCliente = 0;
        buffer = new byte[TAMANIO_BUFFER];
        int count0 = 0;
        for (int i = 0; i < clientes.length; i++) {
            System.out.println("SERVIDOR: enviando paquetes a los clientes...");
            portCliente = (int) clientes[i][1];
            adressCliente = (InetAddress) clientes[i][0];
            System.out.println(adressCliente.toString());
            AuxServer envio = new AuxServer(file, TAMANIO_BUFFER, adressCliente, portCliente, socket);
            envio.start();
        }

    }

    private void enviarHashAClientesYConfirmarIntegridad(byte[] buffer, DatagramPacket packet) throws IOException {
        int count = 0;
        InetAddress adressCliente = null;
        int portCliente = 0;
        String confirmacion;
        buffer = new byte[256];
        while (count < clientes.length*2) {

            packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            System.out.println("SERVIDOR: paquete recibido " + packet.toString());
            adressCliente = packet.getAddress();
            portCliente = packet.getPort();
            confirmacion = new String(packet.getData(), 0, packet.getLength());
            System.out.println(confirmacion);
            if (confirmacion.equals(ARCHIVOS_RECIBIDOS)) {
                enviarDatagramaString(hash, buffer, packet, adressCliente, portCliente);
                System.out.println("SERVIDOR: Hash enviado " + hash);
            }
            else if (confirmacion.equals(INTEGRIDAD_OK)) {
                System.out.println("SERVIDOR: el archivo del cliente " + adressCliente.getCanonicalHostName() + ": " + portCliente);
                System.out.println("SERVIDOR: llego correctamente");

                //TODO
                //REGISTRAR EN LOG

            }
            else if (confirmacion.equals(INTEGRIDAD_ERROR)) {
                System.out.println("SERVIDOR: el archivo del cliente " + adressCliente.getCanonicalHostName() + ": " + portCliente);
                System.out.println("SERVIDOR: NO llego correctamente");
                //TODO
                //REGISTRAR EN LOG
            }
            count++;
        }
    }

    private void enviarDatagramaString(String msg, byte[] buffer, DatagramPacket packet, InetAddress adressCliente, int portCliente) throws IOException {
        buffer = msg.getBytes();
        packet = new DatagramPacket(buffer, buffer.length, adressCliente, portCliente);
        socket.send(packet);
    }

//    private void recibirConfirmacionIntegridadClientes(byte[] buffer, DatagramPacket packet) throws IOException {
//        int count = 0;
//        InetAddress adressCliente = null;
//        int portCliente = 0;
//        String confirmacion;
//        buffer = new byte[256];
//        while (count < maxNumeroClientes) {
//
//            packet = new DatagramPacket(buffer, buffer.length);
//            socket.receive(packet);
//            System.out.println("SERVIDOR: paquete recibido " + packet.toString());
//            adressCliente = packet.getAddress();
//            portCliente = packet.getPort();
//            confirmacion = new String(packet.getData(), 0, packet.getLength());
//
//            if (confirmacion.equals(INTEGRIDAD_OK)) {
//                System.out.println("SERVIDOR: el archivo del cliente " + adressCliente.getCanonicalHostName() + ": " + portCliente);
//                System.out.println("SERVIDOR: llego correctamente");
//
//                //TODO
//                //REGISTRAR EN LOG
//
//            } else if (confirmacion.equals(INTEGRIDAD_ERROR)) {
//                System.out.println("SERVIDOR: el archivo del cliente " + adressCliente.getCanonicalHostName() + ": " + portCliente);
//                System.out.println("SERVIDOR: llego NO correctamente");
//                //TODO
//                //REGISTRAR EN LOG
//            }
//            count++;
//        }
//    }

    private String getFileHash(File archivo) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        FileInputStream inputStream = new FileInputStream(archivo);
        byte[] byteArray = new byte[1024];
        int count = 0;
        while ((count = inputStream.read(byteArray)) != -1) {
            digest.update(byteArray, 0, count);
        }
        inputStream.close();
        byte[] bytes = digest.digest();


        StringBuilder stringB = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            stringB.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }

        return stringB.toString();
    }


    public int numeroPrueba() throws IOException {
        File file = new File(RUTA_NUMERO_PRUEBAS);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String ss = br.readLine();
        int idLog = Integer.parseInt(ss);
        idLog++;
        FileWriter fw = new FileWriter(file);
        fw.append(String.valueOf(idLog));
        fw.close();

        return idLog;
    }

    //==================================================
    // MAIN
    //==================================================

    public static void main(String[] args) {

        Scanner scan = new Scanner(System.in);
        System.out.println("Ingrese el archivo que desea enviar");
        System.out.println("1 = video 1 - 100 MB");
        System.out.println("2 = video 1 - 250 MB");

        String nombreArchivoScan = scan.nextLine();
        String nombreArchivo = "";

        if (nombreArchivoScan.equals("1")) {
            nombreArchivo = "video1.mp4";
        } else {
            nombreArchivo = "video2.mp4";
        }
        System.out.println("Ingrese la cantidad conexiones (max 25)");
        int numCon = Integer.parseInt(scan.nextLine());

        Servidor s = new Servidor(nombreArchivo, numCon);
        try {
            s.inciarServidor();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
