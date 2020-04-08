package test;

import clienteUDP.Cliente;
import uniandes.gload.core.Task;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class ClientServerTask extends Task {

    @Override
    public void fail() {
        System.out.println(Task.MENSAJE_FAIL);

    }

    @Override
    public void success() {
        System.out.println(Task.OK_MESSAGE);

    }

    @Override
    public void execute() {
        Cliente cliente = new Cliente();
        try {
            cliente.iniciarCliente();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

    }

}