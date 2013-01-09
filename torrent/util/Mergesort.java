package torrent.util;

import java.util.ArrayList;

public class Mergesort {

	private ISortable[] array;
	
	public Mergesort(ArrayList<ISortable> arraylist) {
		array = arraylist.toArray(new ISortable[] {}); 
	}
	
	public Mergesort(ISortable[] array) {
		this.array = array;
	}
	
	public ISortable getItem(int index) {
		return array[index];
	}

	public void sort() {
		mergesort(array);
	}
	
	private void mergesort(ISortable[] subArray) {
		if(subArray.length <= 1)
			return;
		
		int middleIndex = subArray.length / 2;
		ISortable[] leftSection = new ISortable[middleIndex];
		ISortable[] rightSection = new ISortable[subArray.length - middleIndex];
		for(int i = 0; i < leftSection.length; i++) {
			leftSection[i] = subArray[i];
		}
		for(int i = middleIndex; i < subArray.length; i++) {
			rightSection[i - middleIndex] = subArray[i];
		}
		mergesort(leftSection);
		mergesort(rightSection);
		
		int leftIndex = 0;
		int rightIndex = 0;
		
		for(int i = 0; i < subArray.length; i++) {
			if(leftIndex >= leftSection.length) {
				subArray[i] = rightSection[rightIndex++];
			} else if (rightIndex >= rightSection.length) {
				subArray[i] = leftSection[leftIndex++];
			} else if (leftSection[leftIndex].getValue() < rightSection[rightIndex].getValue()) {
				subArray[i] = leftSection[leftIndex++];
			} else {
				subArray[i] = rightSection[rightIndex++];
			}
		}
	}
}
