package servidorUDP;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class AuxServer extends Thread {

    private File archivo;

    private byte[] buffer;
    private int bufferSize;
    InetAddress adressCliente;
    int portCliente;
    private DatagramSocket socket;

    public AuxServer(File archivo, int bufferSize, InetAddress adressCliente, int portCliente, DatagramSocket socket) {
        this.archivo = archivo;
        this.bufferSize = bufferSize;
        this.adressCliente = adressCliente;
        this.portCliente = portCliente;
        this.socket = socket;
    }

    public void sendPackets() throws Throwable {
        DatagramPacket packet = null;
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(archivo));
        int in;
        buffer = new byte[bufferSize];
        int count0 = 0;

        while ((in = bis.read(buffer)) != -1) {
            System.out.println("SERVIDOR: enviando al cliente " + adressCliente.toString() + " : " + portCliente);
            packet = new DatagramPacket(buffer, buffer.length, adressCliente, portCliente);
            socket.send(packet);
            System.out.println("SERVIDOR: tamanio paquete " + packet.getData().length);
        }
        count0 += buffer.length;
        System.out.println("SERVIDOR: count enviado " + count0);
        buffer = new byte[bufferSize];
        bis.close();
        this.finalize();

    }

    @Override
    public void run() {
        try {
            this.sendPackets();


        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


}
