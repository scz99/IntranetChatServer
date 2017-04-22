import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import java.util.*;
class DatagramController implements Runnable{
    Thread t;
    public int[] portList=new int[100];
    public String[] userList=new String[100];
    int availablePortNos=0;
    DatagramReceiver dr;
    DatagramSocket dcs;
    public volatile boolean running=true;

    DatagramController(int port,String username,DatagramReceiver dr){
        t=new Thread(this);
        this.dr=dr;
        t.start();
        try{
            dcs=new DatagramSocket(port);
            byte[] controlByte=((port-1000)+":1:"+username).getBytes();
            for(int i=5100;i<5200;++i){
                DatagramPacket controlPacket=new DatagramPacket(controlByte,controlByte.length,InetAddress.getLocalHost(),i);
                dcs.send(controlPacket);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public void run(){
        while(running){
            byte[] controlByte=new byte[1024];
            DatagramPacket controlPacket=new DatagramPacket(controlByte,controlByte.length);
            
            try{
                dcs.receive(controlPacket);
                String controlInfo=new String(controlPacket.getData());
                String[] controls=controlInfo.split(":");
                int port=Integer.parseInt(controls[0]);
                int first=Integer.parseInt(controls[1]);
                String user=controls[2];
                boolean flag=true;
                for(int i=0;i<availablePortNos;++i){
                    if(portList[i]==(port)){
                        flag=false;
                    }
                }
                if(flag&&first!=2){
                    portList[availablePortNos]=port;
                    userList[availablePortNos]=user;
                    dr.onlineList.add(user);
                    ++availablePortNos;
                }
                if(first==1){
                    int recvPort=port+1000;
                    for(int i=0;i<availablePortNos;++i){
                        byte[] controlSendByte=(portList[i]+":0:"+userList[i]).getBytes();
                        DatagramPacket controlSendPacket=new DatagramPacket(controlSendByte,controlSendByte.length,InetAddress.getLocalHost(),recvPort);
                        dcs.send(controlSendPacket);
                    }          
                }else if(first==2){
                    int i;
                    for(i=0;i<availablePortNos;++i){
                        if(portList[i]==(port)){
                            --availablePortNos;
                            dr.onlineList.remove(userList[i]);
                            for(;i<availablePortNos;++i){
                                portList[i]=portList[i+1];
                                userList[i]=userList[i+1];
                            }
                            break;
                        }
                    }
                    
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}
class DatagramReceiver extends JFrame implements Runnable,ActionListener,MouseListener{
    String username;
    int port;
    
    TextArea messagebox,message;
    java.awt.List onlineList;
    boolean[] blockList=new boolean[100];
    Button send;
    Thread datagramReceiver;
    DatagramSocket dds;
    DatagramController dcs;
    volatile boolean running=true;
    DatagramReceiver(String username){
        setSize(570,690);
        for(int i=0;i<100;++i){
            blockList[i]=false;
        }
        setTitle("Logged in as:"+username);
        this.username=username;
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        JPanel messageAndList=new JPanel();
        messagebox=new TextArea("",50,100,TextArea.SCROLLBARS_VERTICAL_ONLY);
        messageAndList.setLayout(null);
        messagebox.setBounds(20,20,400,500);
        messagebox.setEditable(false);
        messageAndList.add(messagebox);
        setLayout(null);
        onlineList=new java.awt.List(50,false);
        onlineList.setBounds(430,20,100,500);
        onlineList.addMouseListener(this);
        messageAndList.add(onlineList);
        messageAndList.setBounds(00,00,540,520);
        add(messageAndList);
        JPanel sender=new JPanel();
        sender.setLayout(null);
        sender.setBounds(00,520,540,130);
        message=new TextArea("",10,100,TextArea.SCROLLBARS_VERTICAL_ONLY);
        message.setBounds(20,10,400,100);
        send=new Button("Send");
        send.setBounds(430,35,100,50);
        send.addActionListener(this);
        sender.add(send);
        sender.add(message);
        add(sender);
        setResizable(false);
        setVisible(true);
        datagramReceiver=new Thread(this);
        datagramReceiver.start();
    }
    public void processWindowEvent(WindowEvent we){
        if(we.getID()==WindowEvent.WINDOW_CLOSING){
            int port = dds.getLocalPort();
            byte[] controlSendByte=(port+":2:"+username).getBytes();
            for(int i=0;i<dcs.availablePortNos;++i){
                
                if(port!=dcs.portList[i]){
                    try{
                        DatagramPacket controlSendPacket=new DatagramPacket(controlSendByte,controlSendByte.length,InetAddress.getLocalHost(),dcs.portList[i]+1000);
                        dcs.dcs.send(controlSendPacket);
                        
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
            running=false;
            System.exit(0);
        }
    }
    public void run(){
        port=4100;
        while(port<4200)
        try{
            dds=new DatagramSocket(port);
            dcs=new DatagramController(port+1000,username,this);   
            break;
        }catch(Exception e){
            ++port;
        }
        while(running){
            byte[] recBytes=new byte[1024];
            DatagramPacket recv=new DatagramPacket(recBytes,1024);
            try{
                dds.receive(recv);
            }catch(Exception e){
                e.printStackTrace();
            }
            String incoming=new String(recv.getData());
            if(messagebox.getText().equals(""))
                messagebox.setText(incoming);
            else
                messagebox.setText(messagebox.getText()+"\n"+incoming);
        }
    }
    public void actionPerformed(ActionEvent ae){
        if(ae.getActionCommand().equals("Send")){
            byte[] sendBytes=(username+":"+message.getText()).getBytes();
            message.setText("");
            for(int i=0;i<dcs.portList.length;++i){
                DatagramPacket datPack;
                if(blockList[i]==false){
                    try{
                        datPack=new DatagramPacket(sendBytes,sendBytes.length,InetAddress.getLocalHost(),dcs.portList[i]);
                        dds.send(datPack);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    public void mouseClicked(MouseEvent me){
        if(me.getClickCount()==2){
            int index=onlineList.getSelectedIndex();
            if(blockList[index]==true)
                blockList[index]=false;
            else
                blockList[index]=true;
        }
    }
    public void mouseEntered(MouseEvent me){}
    public void mouseExited(MouseEvent me){}
    public void mousePressed(MouseEvent me){}
    public void mouseReleased(MouseEvent me){}
}

class DatagramLogin extends JFrame implements Runnable,ActionListener{
    Panel pan;
    JTextField username;
    JPasswordField password;
    JButton loginButton;
    Thread t;
    volatile boolean login=false;
    DatagramLogin(){
        t=new Thread(this);
        setTitle("Login");
        setSize(450,235);
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        username=new JTextField(32);
        password=new JPasswordField(32);
        loginButton=new JButton("Login");
        JPanel topPanel=new JPanel();
        JPanel bottomPanel=new JPanel();
        topPanel.setLayout(new FlowLayout());
        bottomPanel.setLayout(new FlowLayout());
        setLayout(null);
        topPanel.setBounds(20,25,400,100);
        topPanel.add(new JLabel("Username"));
        topPanel.add(username);
        topPanel.add(new JLabel("Password"));
        topPanel.add(password);
        add(topPanel);
        bottomPanel.setBounds(170,140,100,100);
        loginButton.setBounds(50,20,100,50);
        loginButton.addActionListener(this);
        bottomPanel.add(loginButton);
        add(bottomPanel);
        setResizable(false);
        setVisible(true);
        t.start();
    }
    public void run(){
       while(!login);
        this.setVisible(false);
    }
    public void processWindowEvent(WindowEvent we){
        if(we.getID()==WindowEvent.WINDOW_CLOSING){
            System.exit(0);
        }
    }
    public void actionPerformed(ActionEvent ae){
        if(ae.getActionCommand().equals("Login")){
            if((new String(password.getPassword()).equals("password"))&&(!username.getText().equals(""))){
                login=true;
            }else if(username.getText().equals("")||(new String(password.getPassword()).equals(""))){
                JOptionPane.showMessageDialog(null, "UserName or PassWord Empty", "Incomplete Fields", JOptionPane.WARNING_MESSAGE);
            }else{
                JOptionPane.showMessageDialog(null, "Wrong Password", "Incomplete Fields", JOptionPane.WARNING_MESSAGE);
            }          
        }
    }
}
 class DatagramChat{
    public static void main(String[] args){
        try{
            DatagramLogin loginFrame=new DatagramLogin();
            loginFrame.t.join();
            String username=loginFrame.username.getText();
            loginFrame.dispose();
            DatagramReceiver dr=new DatagramReceiver(username);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
	}