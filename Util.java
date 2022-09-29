package model;

public class Util {
	public static String bytesToString(byte[] bytes) { //byte�迭�� ���ڿ� ���·� ��ȯ�ϴ� �޼ҵ�
		StringBuilder sb = new StringBuilder();
		int i=0;
		for(byte b:bytes) {
			sb.append(String.format("%02x ",b&0xff)); //AND������ �����Ͽ� ����Ʈ��Ʈ�� ���·� ��ȯ
			if(++i%16==0) sb.append("\n"); //16���� ���ڿ��� ����ϰ� �ٹٲ�
		}
		
		return sb.toString();
	}
}
