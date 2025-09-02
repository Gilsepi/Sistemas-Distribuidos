/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cliente;
/**
 *
 * @author 2023122760026
 */
public class Atualizador extends Thread{
    private Formulario form = null;
    
    public Atualizador(){
        form = new Formulario();
    }
    
    @Override
    public void run(){
        form.setVisible(true);
        
        while(true){
            form.atualizeMensagens();
            form.atualizaUsers();
            
            try{
                Thread.sleep(3000);
            }catch(InterruptedException ex){
                System.out.println(ex.getMessage());
            }
        }
    }
}
