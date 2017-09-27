import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

public class FileManager{
	public static void main(String[] args) throws SQLException, FileNotFoundException, IOException{
		File file=new File("C:\\Users\\lenovo\\Documents\\workspace\\chp01\\date.txt");		//date.txt存储程序上次结束运行时间，首次运行时将创建此文件
		if(file.createNewFile()){
			Date d=new Date(0);
			DataOutputStream out = new DataOutputStream(new FileOutputStream("C:\\Users\\lenovo\\Documents\\workspace\\chp01\\date.txt"));
			out.writeLong(d.getTime());
			out.close();
		}
		DataInputStream in = new DataInputStream(new FileInputStream("C:\\Users\\lenovo\\Documents\\workspace\\chp01\\date.txt"));
		Long lasttime=in.readLong();		//读取程序上次结束运行的时间
		in.close();
		
		Connect cnct=new Connect();
		Connection con1=cnct.connect();		//连接数据库
		String path="F:\\test";				//path为程序所管理的根目录
		
		Update update=new Update();
		update.AddL(path,con1,lasttime);	//程序进行根目录更新
		update.Add(path,con1,lasttime);		//程序进行更新
		
		Search srh=new Search();			//使用查询类
		GUI Cgui=new GUI();					//使用GUI类
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				Cgui.constructGUI(srh,con1);
			}
		});
		
		DataOutputStream out = new DataOutputStream(new FileOutputStream("C:\\Users\\lenovo\\Documents\\workspace\\chp01\\date.txt"));
		out.writeLong((System.currentTimeMillis()));		//记录本次程序结束运行的时间
		out.close();
	}
}


class GUI{
	private static void copyFile(String source, String dest) throws IOException {   //实现文件的备份，source为源文件路径，dest为目的路径（无文件名） 
	    InputStream input = null;    
	    OutputStream output = null; 
	    File oldfile=new File(source);
	    String name=source.substring(source.lastIndexOf("\\")+1);                   //从源路径中取得文件名字
	    String target=dest+"\\"+name;                                               //将文件名加到目的路径上
	    File   newfile=new File(target);
	    try {																		//实现文件从源路径备份到目的路径
	           input = new FileInputStream(oldfile);
	           output = new FileOutputStream(newfile);        
	           byte[] buf = new byte[1024];        
	           int bytesRead;        
	           while ((bytesRead = input.read(buf)) > 0) {
	               output.write(buf, 0, bytesRead);
	           }
	    } finally {
	        input.close();
	        output.close();
	    }
	}
	static void constructGUI(Search s,Connection con){									//构建GUI
	JFrame.setDefaultLookAndFeelDecorated(true);
	JFrame frame=new JFrame("File Seclect");
	frame.setLayout(new FlowLayout());
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	JTextField FileNameField=new JTextField(20);
	JButton button=new JButton("Seclect File");
	button.addActionListener(new ActionListener(){										//为"Select File"添加事件监听
		public void actionPerformed(ActionEvent e){
		String  FileName	=FileNameField.getText();										//获取需要搜索的文件的文件名
		Set<String> result = new HashSet<String>();
		try {
			result=s.search(FileName, con);												//将搜索得到的结果放入一个容器中
		} catch (SQLException e1) {
		}
		JList list=new JList(result.toArray());
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);						//将列表设为单项选中
		JPopupMenu popupMenu=new JPopupMenu();											//创建弹出菜单
		JMenuItem open=new JMenuItem("打开");											//为菜单添加“打开”、“复制”选项
		JMenuItem copy=new JMenuItem("复制");
		popupMenu.add(open);
		popupMenu.add(copy);
		list.addMouseListener(new  MouseAdapter(){										//给列表添加鼠标事件监听
			@Override
			   public void mouseClicked(MouseEvent e) {
				  if(SwingUtilities.isLeftMouseButton(e)&&e.getClickCount()==2){ 		//当鼠标左键被按中且点击两次时，打开文件
					  File file=new File((String) list.getSelectedValue());
	                    try {
							Desktop.getDesktop().open(file);
						} catch (IOException e1) {
						}
				  }
			   }

			   @Override
			   public void mousePressed(MouseEvent e) {
			     list.setSelectedIndex(list.locationToIndex(e.getPoint())); 				//获取鼠标点击的项
			     maybeShowPopup(e);

			}

			   @Override
			   public void mouseReleased(MouseEvent e) {
			    maybeShowPopup(e);
			   }
			    //弹出菜单
			    private void maybeShowPopup(MouseEvent e) {
			           if (e.isPopupTrigger()&&list.getSelectedIndex()!=-1) {
			               popupMenu.show(e.getComponent(),e.getX(), e.getY());
			           }
			       }

		});
		open.addActionListener(new ActionListener(){										//给右键中的“打开”选项添加事件监听，当鼠标点击时，将打开文件
			public void actionPerformed(ActionEvent e){
				File file=new File((String) list.getSelectedValue());
                try {
					Desktop.getDesktop().open(file);
				} catch (IOException e1) {
				}
			}    
		});
		copy.addActionListener(new ActionListener(){										//给右键中的“复制”选项添加事件监听，当鼠标点击时。将复制文件到指定目录
				public void actionPerformed(ActionEvent e){
				JFileChooser chooser = new JFileChooser();								//利用文件选择器来选取所需的目的路径
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);				
				chooser.showOpenDialog(null);
				try {
					String path = chooser.getSelectedFile().getPath();
					File file=new File((String) list.getSelectedValue());
					File target=new File(path);
					copyFile((String) list.getSelectedValue(),path);						//复制文件
				} catch (Exception e1) {
				}
				
			}
		});
		JScrollPane scrollpanel=new JScrollPane(list);
		scrollpanel.setPreferredSize(new Dimension(400,250));
		JFrame frame1=new JFrame("Result");
		frame1.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame1.add(scrollpanel);
		frame1.setLocation(800, 200);
		frame1.setSize(400, 400);
		frame1.setVisible(true);
		}
	});
	frame.add(FileNameField);
	frame.add(button);
	frame.getContentPane().add(button);
	frame.setLocation(400, 200);
	frame.setSize(400, 120);
	frame.setVisible(true);	
}
}

class Search{
	public Set<String> search(String ss,Connection Scon) throws SQLException{
		Statement stmt=Scon.createStatement();
		Set<String> Sresult = new HashSet<String>();
		String tt1=Pattern.compile("\\*").matcher(ss).replaceAll("");
		String tt2=Pattern.compile("\\\\").matcher(tt1).replaceAll("\\\\\\\\\\\\\\\\");			//数据库中正则式查询需进行转义字符的转化
		String sqlselect;
		if(ss.contains("*")){			//含有通配符的查询
			sqlselect="select * from searchtest where Suffix regexp '("+tt2+"$)|(^"+tt2+")"+"'"+"or Filename regexp '("+tt2+"$)|(^"+tt2+")"+"'"+"or Path regexp '("+tt2+"$)|(^"+tt2+")"+"'";			
		}
		else sqlselect="select * from searchtest where Filename = \""+tt2+"\"";		//不含有通配符的按文件名查询
		System.out.println(sqlselect);
		ResultSet result=stmt.executeQuery(sqlselect);
		while(result.next()){
			String ppath=result.getString(1);
			Sresult.add(ppath);
		}
		return Sresult;
	}
}

class Connect{
		public Connection connect(){
		String url="jdbc:mysql://127.0.0.1:3306/filesearch?useUnicode=true&characterEncoding=utf-8";
		String name = "com.mysql.jdbc.Driver";
		String user = "root";
		String password = "root";
		Connection con1=null;
		Statement stmt1=null;
		try{
			Class.forName(name);
		}catch(java.lang.ClassNotFoundException e){
			System.err.print("ClassNotFoundException:");
			System.err.println(e.getMessage());
		}						
		try { 
			con1=DriverManager.getConnection(url,user,password);
			stmt1=con1.createStatement();	
			stmt1.executeUpdate("create table if not exists searchtest"+"(Path varchar(128) not  null primary key,"+
					"Filename varchar(128) not null,Suffix varchar(128),"+"Time bigint(20) not null) DEFAULT CHARSET=utf8");
        } catch (SQLException e) {  
            System.out.println("无法连接到数据库!");  
        }
		return con1;
	}
}

class Update{
	public void AddL(String ss,Connection con,Long Lasttime) throws SQLException{		//AddL()方法用于更新根目录
		Statement stmt=null;
		stmt=con.createStatement();
		String pathh1=ss;
		Set<String> depath=new HashSet<String>();
		String tt=Pattern.compile("\\\\").matcher(pathh1).replaceAll("\\\\\\\\\\\\\\\\");		//数据库中正则式查询需进行转义字符的转化
		try{
			String sqlselect="select * from searchtest where Path regexp '"+tt+"'";
			ResultSet result=stmt.executeQuery(sqlselect);
			while(result.next()){
				String ppath=result.getString(1);
				File file2=new File(ppath);
				if(!file2.exists()){						//文件不存在则删除它的信息
					String tt1=Pattern.compile("\\\\").matcher(ppath).replaceAll("\\\\\\\\");
					String sqldelete="delete from searchtest where Path="+"'"+tt1+"'";
					depath.add(sqldelete);
				}
			 }
			for(String a:depath){
				stmt.executeUpdate(a);
			}
		}catch(NullPointerException e){	
			
		}
	}
	public void Add(String ss,Connection con,Long Lasttime) throws SQLException{
		Statement stmt=null;
		int i=0;
		Set<String> depath=new HashSet<String>();
		File file1=new File(ss);
		File[] templist1=file1.listFiles();
		for( i=0;i<templist1.length;i++){
			if(templist1[i].isFile() && !templist1[i].isHidden() &&templist1[i].lastModified()>=Lasttime){	
				//文件最后修改时间迟于程序上次结束运行时间，则向数据库中添加该文件信息，已存在则不会添加
				try{			
					stmt=con.createStatement();
					PreparedStatement updateSales;
					String updateString="insert into searchtest set Path=?,Filename=?,Suffix=?,Time=?";
					updateSales=con.prepareStatement(updateString);
					updateSales.setString(1,templist1[i].getPath());
					updateSales.setString(2,templist1[i].getName().substring(0,(templist1[i].getName()).lastIndexOf(".")));
					updateSales.setString(3,(templist1[i].getName()).substring((templist1[i].getName()).lastIndexOf(".")+1));
					updateSales.setLong(4,templist1[i].lastModified());
					updateSales.executeUpdate();
				}catch(SQLException ex){			//文件信息已存在于数据库中时将捕获异常
				}
			}
			else if(templist1[i].isDirectory() && !templist1[i].isHidden()){		//如果是文件夹的话
				try{
					stmt=con.createStatement();
					String pathh1=templist1[i].getPath();
					String tt=Pattern.compile("\\\\").matcher(pathh1).replaceAll("\\\\\\\\\\\\\\\\");
					try{
						String sqlselect="select * from searchtest where Path regexp '"+tt+"'";		//查询到数据库中此文件夹下的所有文件
						ResultSet result=stmt.executeQuery(sqlselect);
						while(result.next()){
							String ppath=result.getString(1);
							File file2=new File(ppath);
							if(!file2.exists()){						//确定此文件夹下的文件是否存在
								String tt1=Pattern.compile("\\\\").matcher(ppath).replaceAll("\\\\\\\\");
								String sqldelete="delete from searchtest where Path="+"'"+tt1+"'";
								depath.add(sqldelete);
							}
						 }
						for(String a:depath){
							stmt.executeUpdate(a);
						}
					}catch(NullPointerException e){	
					}
					stmt=con.createStatement();
					PreparedStatement updateSales;
					String updateString="insert into searchtest set Path=?,Filename=?,Suffix=null,Time=?";
					//将此文件夹的信息添加到数据库中，已存在则不会添加
					updateSales=con.prepareStatement(updateString);
					updateSales.setString(1,templist1[i].getPath());
					updateSales.setString(2,templist1[i].getName());
					updateSales.setLong(3,templist1[i].lastModified());
					updateSales.executeUpdate();
					}catch(SQLException ex){
				}			//文件信息已存在于数据库中时将捕获异常
				try{
				Add(templist1[i].getPath(),con,Lasttime);		//此处递归调用
				}catch(Exception e){
				}
			}
		}
	}
}
