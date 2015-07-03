import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
	
class ChatClient extends JFrame implements ActionListener
{
	// CardLayout : �α���â -> ä��â
	// *�α���â* id �Է¹ް� enter ������ ä��â���� ��ȯ
	// *ä��â*
	// Center : ���� �����, ��ȭâ, ������ ���
	// South : �޼��� ����
	// North : Client Id
	
	Container c;
	JTextField id = new JTextField(20); //login id
	
	JLabel displayId; //Client id
	
	JTextArea users = new JTextArea(5,30); //���� �����
	JTextArea display = new JTextArea(10,30); //��ȭâ
	JTextArea spamwords= new JTextArea(5,30); //������ ���
	
	JTextField inData = new JTextField(20); //�޼���
	JButton send; //���۹�ư
	
	CardLayout window;
	
	BufferedReader br;
	PrintWriter pw;
	
	Socket sock;
	
	ChatClient(){ 
		setTitle("Messenger");
		//cardLayout
		window = new CardLayout();
		c = this.getContentPane(); 
		c.setLayout(window); 
	
		//�α���â
		JPanel login = new JPanel(new BorderLayout());
		JPanel bottom = new JPanel();
		JLabel idLabel = new JLabel("Id:");
		id.addActionListener(this);
		
		bottom.add(idLabel); 
		bottom.add(id); 
		login.add("South", bottom);
		
		c.add(login);
		
		
		//ä��â
		JPanel chat = new JPanel(new BorderLayout());
		
		//Center : ���� �����, ��ȭâ, ������ ���
		JPanel centerPanel= new JPanel(new BorderLayout());
		
		//��ȭâ
		JScrollPane spd = new JScrollPane(display); 
		display.setEditable(false);
		centerPanel.add("Center", spd);
		
		//������ ���
		JPanel spam = new JPanel();
		JLabel ban = new JLabel("������:");
		spam.add(ban);
		
		JScrollPane spb = new JScrollPane(spamwords);
		spamwords.setEditable(false);
		spam.add(spb);			
		centerPanel.add("South", spam);
		
		//���� �����
		JPanel user = new JPanel();
		JLabel nowuser = new JLabel("���� �����:");
		user.add(nowuser);
		
		JScrollPane spu= new JScrollPane(users);
		users.setEditable(false);
		user.add(spu);
		centerPanel.add("North", user);
		
		chat.add("Center", centerPanel);
		
		//South : �޼��� ����
		JPanel message = new JPanel();
		JLabel msg= new JLabel("�޼���:");
		message.add(msg);
		message.add(inData);
		
		send = new JButton("����");
		send.addActionListener(this);
		
		message.add(send);
		chat.add("South", message);
		
		//North : Client Id
		JPanel northPanel = new JPanel();
		displayId = new JLabel();
		northPanel.add(displayId);
		chat.add("North" , northPanel);
		
		
		c.add("chat", chat);
			
		window.show(c, "login");
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
		setSize(500, 600); 
		setVisible(true); 
	}

	public void process() // Thread ����
	{	
		try {
			sock = new Socket("127.0.0.1", 10001);
			br= new BufferedReader(new InputStreamReader(sock.getInputStream()));
			pw= new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
	
			InputThread it = new InputThread(sock,br);
			it.start();
		}
		catch(Exception ex){}
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource().equals(id)) //login �ϸ�
		{
			//Id���� -> ä��â ��ȯ
			String alias = id.getText();
			displayId.setText(alias);
			window.show(c, "chat");
			//Thread
			process();
			//Server�� Id����
			pw.println(alias);
			pw.flush(); 	
		}
		
		else if(e.getSource().equals(send)) //�����ϸ�
		{
			//Server�� msg����
			pw.println(inData.getText());
			pw.flush();
			inData.setText("");			
		}
	}
	
	class InputThread extends Thread{					
		private Socket sock = null;				
		private BufferedReader br = null;				
		public InputThread(Socket sock, BufferedReader br){				
			this.sock = sock;			
			this.br = br;			
		}				
		public void run(){				
			try{	
				String line = null;	
				while((line = br.readLine()) != null){		
					if(line.equals("/quit")) 
					{
						br.close();
						sock.close();
						System.exit(0);
					}
					
					//message
					//br# : ��ȭâ- display
					//li# : ���� �����â - users
					//ls# : ������ ���â - spamwords
					String msg[] = line.split("#");
					
					if(msg[0].equals("br"))
						display.append(msg[1] + "\n");
					
					else if(msg[0].equals("li"))
					{
						//*���� ����� ����� ������ �����ϴ�* ������ 
						//���� ����� TextArea �ʱ�ȭ
						if(msg[1].indexOf("*")!=-1) users.setText("");
						users.append(msg[1] + "\n");
					}
					
					else if(msg[0].equals("ls"))
					{
						//*���� ��ϵ� ������� ������ �����ϴ�* ������
						//������ ��� TextArea �ʱ�ȭ
						if(msg[1].indexOf("*")!=-1) spamwords.setText("");
						spamwords.append(msg[1]+"\n");
					}
				}		
			}catch(Exception ex){
			}finally{			
				try{		
					if(br != null)	
						br.close();
				}catch(Exception ex){}		
				try{		
					if(sock != null)	
						sock.close();
				}catch(Exception ex){}		
			}			
		} // InputThread				
	}
	
	public static void main(String[] args)
	{
		new ChatClient();
	}
}
						
