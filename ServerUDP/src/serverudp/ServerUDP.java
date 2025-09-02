/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package serverudp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Gilsepi
 */
public class ServerUDP {
    private static BaseDeDados bd = null;
    private static ArrayList<String> usuarios = null;
                 
    /**
     * @param args the command line arguments
     * 
     */
    
    public static int contarChar(String texto, char c) {
        int contador = 0;
        for (int i = 0; i < texto.length(); i++) {
            if (texto.charAt(i) == c) {
                contador++;
            }
        }
        return contador;
    }
    public static void main(String[] args) {
       System.out.println("INICIANDO");
       DatagramSocket aSocket = null;
       bd = new BaseDeDados();
       usuarios = new ArrayList();
       Atualizador atu = new Atualizador(usuarios);
       atu.start();
       try{
           aSocket = new DatagramSocket(6789);
           while(true){
               byte[] buffer = new byte[600];
               DatagramPacket request = new DatagramPacket(buffer, buffer.length);
               aSocket.receive(request);
           
           
                String mensagem = new String(request.getData()).trim();
                
                System.out.println(mensagem);
                
                String[] parts = mensagem.split("/");
                String command = parts[0];
                String data = parts[1];
                String users = "USERS";
                
                switch (command) {
                    case "***ATUALIZE***":
                        break;
                    case "***REGISTER***":
                        if(usuarios.contains(data)){
                            System.out.println(data);
                        }else{
                            usuarios.add(data + "1");
                            System.out.println(data);
                            
                        }
                        break;
                    case "***GET_USERS***":
                        for(String user : usuarios){
                            users = users + "/" + user;
                        }
                        System.out.println(users);
                        byte [] info = users.getBytes();
                        DatagramPacket dataUsers = new DatagramPacket(info,info.length,request.getAddress(),request.getPort());
                        aSocket.send(dataUsers);
                        break;
                    case "***ONLINE***":
                        int index = usuarios.indexOf(data);
                        String usuarionome = usuarios.get(index);
                        int idx = usuarionome.length();
                        StringBuilder sb = new StringBuilder(usuarionome);

                        // Substituir o caractere na posição desejada
                        sb.setCharAt(idx - 1, '1');

                        // Atualizar a lista com a nova string
                        usuarios.set(index, sb.toString());
                        
                    default:
                        System.out.println("\nINSERIDO: "+mensagem+"    TEMPO: "+LocalTime.now());
                        bd.insere(data.toUpperCase());
                        break;
                }
                

                String resposta = bd.le();
                byte [] todasMsg = resposta.getBytes();

                DatagramPacket reply = new DatagramPacket(todasMsg,todasMsg.length,request.getAddress(),request.getPort());
                aSocket.send(reply);
                 
           }
       
       }catch(SocketException e){
            System.out.println("SERVIDOR - Socket: " + e.getMessage());
       } catch(IOException e){
           System.out.println("SERVIDOR - Input Output: " + e.getMessage());
           
       } finally{
           if(aSocket != null) aSocket.close();
       }
        
        
    
    }
    
}
