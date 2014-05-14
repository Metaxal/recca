/*
 * Enumerates a bounded integer by order of the sum of its bits:
 * sum = 0
 * 	000
 * sum = 1
 *  001, 010, 100
 * sum = 2
 *  011, 101, 110
 * sum = 3
 *  111
 *   
 * @author: Laurent Orseau
 */
import java.util.Enumeration;

public class EnumInt implements Enumeration<Integer> {
	
	int num[]; // boolean in fact
	int nbBit;
	int nmax;
	
	public EnumInt(int nbB) {
		nbBit = nbB;
		num = new int[nbBit];
		nmax = -1;
		
	}
	
	// initializes the nbUn lower bits to 1 -> sum = 1
	// bits after nfin are not modified
	private void initN(int nbUn, int nfin) { 
		for(int i = 0; i < nfin; i++)
			if(i < nbUn)
				num[i] = 1;
			else
				num[i] = 0;
	}
	
	public int next() {
		return nextElement().intValue();
	}
		
	// not very efficient, but effective...
	public Integer nextElement() {
		
		// try to push low weight bits to the left 
		int i;
		int nbUn = 0;
		for(i = 0; i < nbBit-1; i++) {
			if(num[i] == 1 && num[i+1] == 0) {
				num[i] = 0;
				num[i+1] = 1;
				break;
			}
			if(num[i] == 1)
				nbUn++;
		}
		if(i == nbBit-1) { // if break
			// could not push bits, gotta reinit the number with the next sum: 11100000 -> 00001111
			nmax++;
			initN(nmax, nbBit);
		} else { // pushed a bit, put the lower active bits to the right: 00101110 -> 00110011  
			initN(nbUn, i);
		}
				
		int n = 0;
		for(i = nbBit-1; i >=0; i--)
			n = (n << 1) | num[i]; 
		
		return new Integer(n);
	}
	
	public boolean hasMoreElements() {
		return nmax != nbBit;
	}

}
