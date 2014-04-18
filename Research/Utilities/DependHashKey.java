package Utilities;

import java.util.ArrayList;
import java.util.HashSet;

public class DependHashKey {
	private double[] array;
	
	public DependHashKey(double[] array) {
		
		this.array = new double[array.length];
		
		for(int i = 0; i < array.length; i++) {
			
			this.array[i] = array[i];
		}
	}
	
	public double[] getArray() {
		
		return array;
	}
	
	@Override
	public boolean equals(Object o) {
		
		if(o instanceof DependHashKey) {
			DependHashKey dhk = (DependHashKey) o;
			
			if(array.length == dhk.getArray().length) {
				
				for(int i = 0; i < array.length; i++) {
					
					if(array[i] != dhk.getArray()[i]) {
						
						return false;
					}
				}
				
				return true;
			}		
			
		}
		
		return false;
		
	}
	
	@Override
	public int hashCode() {
		
		int result = 17;
		
		result = 37 * result + array.hashCode();
		
		return 0;
		
	}
	
	public static void main(String[] args) {
		
		double[] d1 = {0.01, 0.02, 17.5, 100.8};
		double[] d2 = {0.01, 0.02, 17.5, 100.8};
		DependHashKey dhk1 = new DependHashKey(d1);
		DependHashKey dhk2 = new DependHashKey(d2);
		
		System.out.println(dhk1.equals(dhk2));
		
		ArrayList<Double> D1 = new ArrayList<Double>();
		D1.add(0.01);
		D1.add(-0.02);
		
		ArrayList<Double> D2 = new ArrayList<Double>();
		D2.add(0.01);
		D2.add(-0.02);
		
		System.out.println(D1.equals(D2));
		
		HashSet<DependHashKey> hs = new HashSet<DependHashKey>();
		
		hs.add(dhk1);
		System.out.println(hs.contains(dhk2));
		
		HashSet<ArrayList<Double>> hs2 = new HashSet<ArrayList<Double>>();
		
		hs2.add(D1);
		System.out.println(hs2.contains(D2));
		
	}
}