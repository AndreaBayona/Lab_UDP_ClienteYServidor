package clienteUDP;

import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Cliente {

	//==================================================
	// DIRECCIONES
	//==================================================
	public static String RUTA_ARCHIVOS = "./data/multimedia/cliente/";
	public static String RUTA_LOGS = "./data/logs/cliente/";
	public static String RUTA_NUMERO_PRUEBAS = "./data/logs/cliente/num.txt";

	//==================================================
	// CONSTANTES
	//==================================================
	public static int PUERTO_SERVIDOR = 8000;
	public static int TAMANIO_BUFFER = 2048;
	public static  String DIRECCION_SERVIDOR = "localhost";
	//==================================================
	// COMANDOS
	//==================================================
	public static String ARCHIVOS_RECIBIDOS = "RECIBIDOS";
	public static String INTEGRIDAD_OK = "OK";
	public static String INTEGRIDAD_ERROR = "ERROR";
	public static String INICIAR_CONEXION = "HOLA";
	//==================================================
	// ATRIBUTOS
	//==================================================
	private DatagramSocket socket;
	private InetAddress addressServer;
	private int id;

	//==================================================
	// CONSTRUCTORES
	//==================================================

	public Cliente (){

		try {
			socket = new DatagramSocket();
			addressServer = InetAddress.getByName(DIRECCION_SERVIDOR);
			this.id = numeroCliente();

		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//==================================================
	// METODOS
	//==================================================

	public void iniciarCliente() throws IOException, NoSuchAlgorithmException {
		byte[] buffer = new byte[0];
		DatagramPacket packet = null;
		int sizeArchivo = 0;

		//El cliente se conecta al servidor
		conexionServidor(buffer, packet);

		//El cliente recibe el tamanio del archivo del servidor
		sizeArchivo = recibirTamanio(buffer, packet);

		//El cliente recibe los paquetes del archivo y calcula el Hash de este
		MessageDigest digest = recibirPaquetesYCalcularHAshDelArchivo(buffer, packet, sizeArchivo);

		//El Cliente confirma la recepcion de los paquetes
		confirmarPaquetes(buffer, packet);

		//El cliente recibe el hash original y confirma la integridad
		recibirHashOriginalYconfirmarIntegridad(buffer, packet,digest);

		//Fin del la comunicacion
		socket.close();
	}

	private void conexionServidor(byte[] buffer, DatagramPacket packet) throws IOException {
		String msg = INICIAR_CONEXION;
		buffer = msg.getBytes();
		packet = new DatagramPacket(buffer, buffer.length, addressServer, PUERTO_SERVIDOR);
		socket.send(packet);
	}

	private int recibirTamanio(byte[] buffer, DatagramPacket packet) throws IOException {
		buffer = new byte[256];
		packet = new DatagramPacket(buffer, buffer.length);
		socket.receive(packet);
		String r = new String(packet.getData(), 0, packet.getLength());
		System.out.println("CLIENTE: recibido tamanio archivo " + r);
		int tamanioArch = Integer.parseInt(r);
		return  tamanioArch;
	}

	private MessageDigest recibirPaquetesYCalcularHAshDelArchivo(byte[] buffer, DatagramPacket packet, int tamanioArch) throws NoSuchAlgorithmException, IOException {
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		File file = new File(RUTA_ARCHIVOS + "descarga"+ id +".mp4");
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));

		int count = 0;
		socket.setSoTimeout(20000); //Si pasa mas de 30 segundos sin recibir archivos se sale del loop
		buffer = new byte[TAMANIO_BUFFER];
		while(count < tamanioArch){
			try {
				packet = new DatagramPacket(buffer, buffer.length);
				socket.receive(packet);
				System.out.println("CLIENTE: paquete recibido "+ packet.getData() + "Buffer " + buffer.length);

				digest.update(buffer, 0, Math.min(buffer.length, tamanioArch - count));
				bos.write(buffer, 0,  buffer.length);

				count += packet.getData().length;
				System.out.println("bytes leidos " + count);
				buffer = new byte[TAMANIO_BUFFER];
			} catch (SocketTimeoutException e) {
				e.printStackTrace();
				socket.setSoTimeout(0);
				break;
			}
		}//fin del while

		bos.close();
		return digest;
	}

	private void confirmarPaquetes(byte[] buffer, DatagramPacket packet) throws IOException {
		enviarStringDatagrama(ARCHIVOS_RECIBIDOS, buffer, packet);
		System.out.println("CLIENTE: enviada confirmacion de paquetes recibidos");

	}

	private void recibirHashOriginalYconfirmarIntegridad(byte[] buffer, DatagramPacket packet, MessageDigest digest) throws IOException {
		buffer = new byte[256];
		packet = new DatagramPacket(buffer, buffer.length);
		socket.receive(packet);
		String hashOriginal = new String(packet.getData(), 0, packet.getLength());
		System.out.println("CLIENTE: recibido hash del archivo " + hashOriginal);
		if(comprobarHash(digest, hashOriginal)){
			System.out.print("Archivo llego correctamente");
			enviarStringDatagrama(INTEGRIDAD_OK, buffer, packet);
		}
		else{
			System.out.print("Archivo NO llego correctamente");
			enviarStringDatagrama(INTEGRIDAD_ERROR, buffer, packet);
		}
	}

	private void enviarStringDatagrama(String msg, byte[] buffer, DatagramPacket packet) throws IOException {
		buffer = msg.getBytes();
		packet = new DatagramPacket(buffer, buffer.length, addressServer, PUERTO_SERVIDOR);
		socket.send(packet);
	}

	/**
	 * Compara el hash enviado por el servidor con el hash calculado al archivo enviado
	 *
	 * @param digest
	 * @param hash
	 */
	private boolean comprobarHash(MessageDigest digest, String hash) {
		byte[] bytes2 = digest.digest();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes2.length; i++) {
			sb.append(Integer.toString((bytes2[i] & 0xff) + 0x100, 16).substring(1));
		}
		String ss = sb.toString();
		System.out.println("CLIENTE: hash calculado: " + ss);
		System.out.println("CLIENTE: hash enviado: " + hash);
		boolean iguales = ss.equals(hash);

		if (iguales)
			return true;
		else
			return false;
	}

	/**
	 * Encargado de obtener el idCliente y admeas incrementarlo en el archivo
	 *
	 * @return el id del cliente
	 * @throws IOException
	 */
	public int numeroCliente() throws IOException {
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

		Cliente c = new Cliente();

		try {
			c.iniciarCliente();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}


}
