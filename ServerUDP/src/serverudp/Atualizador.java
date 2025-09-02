/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package serverudp;

import cliente.*;
import java.util.ArrayList;

/**
 *
 * @author 2023122760026
 */
public class Atualizador extends Thread{
    private static ArrayList<String> usuarios = null;
    
    public Atualizador(ArrayList<String> usuarios){
        this.usuarios = usuarios;
    }
    
    @Override
    public void run(){
        
        
        while(true){
            
            
            try{
                Thread.sleep(5000);
            }catch(InterruptedException ex){
                System.out.println(ex.getMessage());
            }
        }
    }
}
