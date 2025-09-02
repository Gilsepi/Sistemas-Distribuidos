/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package serverudp;

import java.util.ArrayList;

/**
 *
 * @author 2023122760026
 */
public class BaseDeDados {
    private ArrayList lista = null;
    
    public BaseDeDados(){
        lista = new ArrayList();
    }
    
    public void insere(String msg){
        lista.add(msg.trim());
    }
    
    public String le(){
        String s = "\n";
        int fim = lista.size();
        
        for (int pos = 0; pos < fim; pos++){
            s = s + "[" + (pos + 1) + "] " + (String)lista.get(pos) + "\n";
        }
        return s;
    }
}
