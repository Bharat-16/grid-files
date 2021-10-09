import java.util.*;
import java.io.*;

class GridFiles {

	int bucket_count;
	boolean overflow_bucket = false;
	boolean split_xdir = true;
	int bucket_size;
	public List<List<Integer>> buckets = new ArrayList<>();
	public List<Integer> overflow = new ArrayList<>();
	public List<Integer> overflow_size = new ArrayList<>();

	GridFiles(int size) {
		bucket_count = 1;
		bucket_size = size;
		List<Integer> item = new ArrayList<>();
		item.add(1);
		item.add(0);
		item.add(-1);
		item.add(400);
		item.add(-1);
		item.add(400);
		buckets.add(item);
	}

	List getBucket(int x, int y) {
		for (List bucket : buckets) {
			int x1 = (Integer) bucket.get(2);
			int x2 = (Integer) bucket.get(3);
			int y1 = (Integer) bucket.get(4);
			int y2 = (Integer) bucket.get(5);
			if (x > x1 && x <= x2 && y > y1 && y <= y2)
				return bucket;
		}
		return null;
	}

	boolean bucketOverflow(int size) {
		if (size > bucket_size)
			return true;
		return false;

	}

	void shiftPoints(String old_bucket_name, List new_bucket) throws IOException {
		int x1 = (Integer) new_bucket.get(2);
		int x2 = (Integer) new_bucket.get(3);
		int y1 = (Integer) new_bucket.get(4);
		int y2 = (Integer) new_bucket.get(5);

		File old = new File(old_bucket_name);
		BufferedReader br = new BufferedReader(new FileReader(old));
		List<String> points = new ArrayList<>();
		String str;
		while ((str = br.readLine()) != null) {
			points.add(str);
		}

		FileWriter oldfile = new FileWriter(old_bucket_name);
		File file = new File("Buckets/bucket" + new_bucket.get(0) + ".txt");
		file.createNewFile();
		FileWriter newfile = new FileWriter("Buckets/bucket" + new_bucket.get(0) + ".txt");

		for (String point : points) {
			String s[] = point.split(",", 3);
			int x = Integer.parseInt(s[1]);
			int y = Integer.parseInt(s[2]);
			if (x > x1 && x <= x2 && y > y1 && y <= y2)
				newfile.write(point + '\n');
			else
				oldfile.write(point + '\n');
		}
		newfile.close();
		oldfile.close();
	}

	private boolean checkForAlingmentAlongAxis(List<List<Integer>> points_list, boolean def) {
		int x = points_list.get(0).get(1);
		int y = points_list.get(0).get(2);
		boolean x_align = true;
		boolean y_align = true;
		// check for x alingment
		for (List<Integer> lst : points_list) {
			if (lst.get(1) != x) {
				x_align = false;
				break;
			}
		}
		// check for y alingment
		for (List<Integer> lst : points_list) {
			if (lst.get(2) != y) {
				y_align = false;
				break;
			}
		}
		if (x_align && y_align) {
			overflow_bucket = true;
			return def;
		} else if (x_align)
			return false;
		else if (y_align)
			return true;

		return def;
	}

	private void chainingForOverflow(List bucket, List<List<Integer>> points_list) throws IOException {
		if (!overflow.contains(bucket.get(0))) { // bucket id
			overflow.add((Integer) bucket.get(0));
			overflow_size.add(0);
			File file = new File("Overflow/overflow" + bucket.get(0) + ".txt");
			file.createNewFile();
		}
		int index = overflow.indexOf(bucket.get(0));
		int temp = overflow_size.get(index);
		overflow_size.set(index, temp + 1);

		// reduce size of all buckets of this id
		for (int j = 0; j < buckets.size(); j++) {
			if (buckets.get(j).get(0) == bucket.get(0))
				buckets.get(j).set(1, (Integer) buckets.get(j).get(1) - 1);
		}

		FileWriter overflow_file = new FileWriter("Overflow/overflow" + bucket.get(0) + ".txt", true);
		FileWriter oldfile = new FileWriter("Buckets/bucket" + bucket.get(0) + ".txt");

		int i = 1;
		for (List<Integer> point : points_list) {
			int id = (Integer) point.get(0);
			int x = (Integer) point.get(1);
			int y = (Integer) point.get(2);
			if (i < points_list.size())
				oldfile.write(id + "," + x + "," + y + '\n');
			else
				overflow_file.write(id + "," + x + "," + y + '\n');
			i++;
		}
		overflow_file.close();
		oldfile.close();

		overflow_bucket = false;
	}

	void splitBucket(List bucket) throws IOException {
		String old_file = "Buckets/bucket" + bucket.get(0) + ".txt";
		File old = new File(old_file);
		BufferedReader br = new BufferedReader(new FileReader(old));
		List<String> points = new ArrayList<>();
		String str;
		while ((str = br.readLine()) != null) {
			points.add(str);
		}
		// count logical buckets of same id
		int same_bucket_count = 0;
		List<Integer> max_bucket = new ArrayList<>();
		List<List<Integer>> points_list = new ArrayList<>();
		for (String point : points) {
			List<Integer> temp = new ArrayList<>();
			String s[] = point.split(",", 3);
			temp.add(Integer.parseInt(s[0]));
			temp.add(Integer.parseInt(s[1]));
			temp.add(Integer.parseInt(s[2]));
			points_list.add(temp);
		}
		int max_points = 0;
		for (List bkt : buckets) {
			if (bkt.get(0) == bucket.get(0)) {
				int x1 = (Integer) bkt.get(2);
				int x2 = (Integer) bkt.get(3);
				int y1 = (Integer) bkt.get(4);
				int y2 = (Integer) bkt.get(5);
				int count = 0;
				for (List point : points_list) {
					int x = (Integer) point.get(1);
					int y = (Integer) point.get(2);
					if (x > x1 && x <= x2 && y > y1 && y <= y2)
						count++;
				}
				if (count > max_points) {
					max_points = count;
					max_bucket.clear();
					for (Object item : bkt)
						max_bucket.add((Integer) item);
				}
				same_bucket_count++;
			}
		}
		if (same_bucket_count > 1) { // multiple buckets with same id

			if (bucketOverflow(max_points)) { // divide both physically and logically
				int split_point;
				split_xdir = checkForAlingmentAlongAxis(points_list, split_xdir);
				if (overflow_bucket) {
					chainingForOverflow(bucket, points_list);
					return;
				}
				if (split_xdir) { // split in x direction

					Collections.sort(points_list, new Comparator<List<Integer>>() {
						@Override
						public int compare(List<Integer> firstList, List<Integer> secondList) {
							return firstList.get(1) - secondList.get(1);
						}
					});
					split_point = (int) (points_list.get((int) points_list.size() / 2 - 1).get(1)
							+ points_list.get((int) points_list.size() / 2).get(1)) / 2;
					int new_size1 = 0;
					for (List point : points_list) {
						if ((Integer) point.get(1) <= split_point) // if(x < split_point)
							new_size1++;
						else
							break;
					}
					int new_size2 = points_list.size() - new_size1;
					for (int i = 0; i < buckets.size(); i++) {
						// set new size of the bucket
						if (buckets.get(i).get(0) == bucket.get(0))
							buckets.get(i).set(1, new_size1);

						// all buckets within the new partition
						if ((Integer) buckets.get(i).get(2) < split_point
								&& (Integer) buckets.get(i).get(3) > split_point) {
							int count = 0;
							int x1 = (Integer) buckets.get(i).get(2);
							int x2 = (Integer) buckets.get(i).get(3);
							int y1 = (Integer) buckets.get(i).get(4);
							int y2 = (Integer) buckets.get(i).get(5);
							for (List point : points_list) {
								int x = (Integer) point.get(1);
								int y = (Integer) point.get(2);
								if (x > x1 && x <= x2 && y > y1 && y <= y2)
									count++;
							}

							int bucketx2 = (Integer) buckets.get(i).get(3);
							buckets.get(i).set(3, split_point);

							List<Integer> item = new ArrayList<>();
							if (buckets.get(i).get(0) == bucket.get(0) && bucketOverflow(count)) { // bucket to split
																									// physically
								bucket_count++;
								item.add(bucket_count);
								item.add(new_size2);
								item.add(split_point);
								item.add(bucketx2);
								item.add((Integer) buckets.get(i).get(4));
								item.add((Integer) buckets.get(i).get(5));
								buckets.add(item);
								// now split physical datapoints
								shiftPoints(old_file, item);
							} else { // other buckets lies on that partition
								item.add((Integer) buckets.get(i).get(0));
								item.add((Integer) buckets.get(i).get(1));
								item.add(split_point);
								item.add(bucketx2);
								item.add((Integer) buckets.get(i).get(4));
								item.add((Integer) buckets.get(i).get(5));
								buckets.add(item);
							}
						}
					}
					split_xdir = false; // set to Y dir
				} else { // split in y direction

					Collections.sort(points_list, new Comparator<List<Integer>>() {
						@Override
						public int compare(List<Integer> firstList, List<Integer> secondList) {
							return firstList.get(2) - secondList.get(2);
						}
					});
					split_point = (int) (points_list.get((int) points_list.size() / 2 - 1).get(2)
							+ points_list.get((int) points_list.size() / 2).get(2)) / 2;
					int new_size1 = 0;
					for (List point : points_list) {
						if ((Integer) point.get(2) <= split_point) // if(y < split_point)
							new_size1++;
						else
							break;
					}
					int new_size2 = points_list.size() - new_size1;
					for (int i = 0; i < buckets.size(); i++) {
						// set new size of the bucket
						if (buckets.get(i).get(0) == bucket.get(0))
							buckets.get(i).set(1, new_size1);

						// all buckets within the new partition
						if ((Integer) buckets.get(i).get(4) < split_point
								&& (Integer) buckets.get(i).get(5) > split_point) {
							int count = 0;
							int x1 = (Integer) buckets.get(i).get(2);
							int x2 = (Integer) buckets.get(i).get(3);
							int y1 = (Integer) buckets.get(i).get(4);
							int y2 = (Integer) buckets.get(i).get(5);
							for (List point : points_list) {
								int x = (Integer) point.get(1);
								int y = (Integer) point.get(2);
								if (x > x1 && x <= x2 && y > y1 && y <= y2)
									count++;
							}

							int buckety2 = (Integer) buckets.get(i).get(5);
							buckets.get(i).set(5, split_point);

							List<Integer> item = new ArrayList<>();
							if (buckets.get(i).get(0) == bucket.get(0) && bucketOverflow(count)) { // bucket to split
																									// physically
								bucket_count++;
								item.add(bucket_count);
								item.add(new_size2);
								item.add((Integer) buckets.get(i).get(2));
								item.add((Integer) buckets.get(i).get(3));
								item.add(split_point);
								item.add(buckety2);
								buckets.add(item);
								// now split physical datapoints
								shiftPoints(old_file, item);
							} else { // other buckets lies on that partition
								item.add((Integer) buckets.get(i).get(0));
								item.add((Integer) buckets.get(i).get(1));
								item.add((Integer) buckets.get(i).get(2));
								item.add((Integer) buckets.get(i).get(3));
								item.add(split_point);
								item.add(buckety2);
								buckets.add(item);
							}
						}
					}
					split_xdir = true; // set to X dir
				}
			} else { // divide only physically

				bucket_count++;
				int sizeofothers = points_list.size() - max_points;
				for (int i = 0; i < buckets.size(); i++) {
					if (buckets.get(i).get(0).equals(max_bucket.get(0))) {
						if (buckets.get(i).equals(max_bucket)) {
							buckets.get(i).set(0, bucket_count);
							buckets.get(i).set(1, max_points);
							shiftPoints(old_file, buckets.get(i));
						} else {
							buckets.get(i).set(1, sizeofothers);
						}
					}
				}
			}
		} else { // singal bucket with this id

			int split_point;
			split_xdir = checkForAlingmentAlongAxis(points_list, split_xdir);
			if (overflow_bucket) {
				chainingForOverflow(bucket, points_list);
				return;
			}
			if (split_xdir) { // split in x direction

				Collections.sort(points_list, new Comparator<List<Integer>>() {
					@Override
					public int compare(List<Integer> firstList, List<Integer> secondList) {
						return firstList.get(1) - secondList.get(1);
					}
				});
				split_point = (int) (points_list.get((int) points_list.size() / 2 - 1).get(1)
						+ points_list.get((int) points_list.size() / 2).get(1)) / 2;
				int new_size1 = 0;
				for (List point : points_list) {
					if ((Integer) point.get(1) <= split_point) // if(x < split_point)
						new_size1++;
					else
						break;
				}
				int new_size2 = points_list.size() - new_size1;
				for (int i = 0; i < buckets.size(); i++) {
					// set new size of the bucket
					if (buckets.get(i).get(0) == bucket.get(0))
						buckets.get(i).set(1, new_size1);

					// all buckets within the new partition
					if ((Integer) buckets.get(i).get(2) < split_point
							&& (Integer) buckets.get(i).get(3) > split_point) {

						int bucketx2 = (Integer) buckets.get(i).get(3);
						buckets.get(i).set(3, split_point);

						List<Integer> item = new ArrayList<>();
						if (buckets.get(i).get(0) == bucket.get(0)) { // bucket to split physically
							bucket_count++;
							item.add(bucket_count);
							item.add(new_size2);
							item.add(split_point);
							item.add(bucketx2);
							item.add((Integer) buckets.get(i).get(4));
							item.add((Integer) buckets.get(i).get(5));
							buckets.add(item);
							// now split physical datapoints
							shiftPoints(old_file, item);
						} else { // other buckets lies on that partition
							item.add((Integer) buckets.get(i).get(0));
							item.add((Integer) buckets.get(i).get(1));
							item.add(split_point);
							item.add(bucketx2);
							item.add((Integer) buckets.get(i).get(4));
							item.add((Integer) buckets.get(i).get(5));
							buckets.add(item);
						}
					}
				}
				split_xdir = false; // set to Y dir
			} else { // split in y direction

				Collections.sort(points_list, new Comparator<List<Integer>>() {
					@Override
					public int compare(List<Integer> firstList, List<Integer> secondList) {
						return firstList.get(2) - secondList.get(2);
					}
				});
				split_point = (int) (points_list.get((int) points_list.size() / 2 - 1).get(2)
						+ points_list.get((int) points_list.size() / 2).get(2)) / 2;
				int new_size1 = 0;
				for (List point : points_list) {
					if ((Integer) point.get(2) <= split_point) // if(y < split_point)
						new_size1++;
					else
						break;
				}
				int new_size2 = points_list.size() - new_size1;
				for (int i = 0; i < buckets.size(); i++) {
					// set new size of the bucket
					if (buckets.get(i).get(0) == bucket.get(0))
						buckets.get(i).set(1, new_size1);

					// all buckets within the new partition
					if ((Integer) buckets.get(i).get(4) < split_point
							&& (Integer) buckets.get(i).get(5) > split_point) {
						int buckety2 = (Integer) buckets.get(i).get(5);
						buckets.get(i).set(5, split_point);

						List<Integer> item = new ArrayList<>();
						if (buckets.get(i).get(0) == bucket.get(0)) { // bucket to split physically
							bucket_count++;
							item.add(bucket_count);
							item.add(new_size2);
							item.add((Integer) buckets.get(i).get(2));
							item.add((Integer) buckets.get(i).get(3));
							item.add(split_point);
							item.add(buckety2);
							buckets.add(item);
							// now split physical datapoints
							shiftPoints(old_file, item);
						} else { // other buckets lies on that partition
							item.add((Integer) buckets.get(i).get(0));
							item.add((Integer) buckets.get(i).get(1));
							item.add((Integer) buckets.get(i).get(2));
							item.add((Integer) buckets.get(i).get(3));
							item.add(split_point);
							item.add(buckety2);
							buckets.add(item);
						}
					}
				}
				split_xdir = true; // set to X dir
			}
		}
	}

	void insert(String point) throws IOException {
		String s[] = point.split(",", 3);
		List<Integer> bucket = getBucket(Integer.parseInt(s[1]), Integer.parseInt(s[2]));
		// insert new point to the bucket
		FileWriter file = new FileWriter("Buckets/bucket" + bucket.get(0) + ".txt", true);
		file.write(point + '\n');
		file.close();

		// increase size of the bucket
		for (int i = 0; i < buckets.size(); i++) {
			if (buckets.get(i).get(0) == bucket.get(0))
				buckets.get(i).set(1, (Integer) buckets.get(i).get(1) + 1);
		}

		if (bucketOverflow(bucket.get(1)))
			splitBucket(bucket);
	}

	// ****range query algorithms*********
	int rangeQueryNaive(int x1, int y1, int x2, int y2) throws IOException {
		int count = 0;
		List<Integer> physical = new ArrayList<>();
		for (List bucket : buckets) {
			if (!physical.contains(bucket.get(0)))
				physical.add((Integer) bucket.get(0));
		}

		for (Object obj : physical) {
			String str;
			File file = new File("Buckets/bucket" + (Integer) obj + ".txt");
			BufferedReader br = new BufferedReader(new FileReader(file));
			while ((str = br.readLine()) != null) {
				String s[] = str.split(",", 3);
				int x = Integer.parseInt(s[1]);
				int y = Integer.parseInt(s[2]);
				if (x >= x1 && x <= x2 && y >= y1 && y <= y2) {
					count++;
					System.out.println(count + " => " + str);
				}
			}
			if (overflow.contains(obj)) {
				file = new File("Overflow/overflow" + (Integer) obj + ".txt");
				br = new BufferedReader(new FileReader(file));
				while ((str = br.readLine()) != null) {
					String s[] = str.split(",", 3);
					int x = Integer.parseInt(s[1]);
					int y = Integer.parseInt(s[2]);
					if (x >= x1 && x <= x2 && y >= y1 && y <= y2) {
						count++;
						System.out.println(count + " => " + str);
					}
				}
			}
		}
		return count;
	}

	int rangeQuerySmart(int x1, int y1, int x2, int y2) throws IOException {
		List<Boolean> flags = new ArrayList<>();

		for (List bucket : buckets) {
			int bx1 = (Integer) bucket.get(2);
			int bx2 = (Integer) bucket.get(3);
			int by1 = (Integer) bucket.get(4);
			int by2 = (Integer) bucket.get(5);

			if (bx1 >= x1 && bx2 <= x2 && by1 >= y1 && by2 <= y2)
				flags.add(true);
			else
				flags.add(false);
		}
		int i = 0;
		List<Integer> bucket_outside_rect = new ArrayList<>();
		for (List bucket : buckets) {
			if (!(Boolean) flags.get(i++)) {
				if (!bucket_outside_rect.contains(bucket.get(0)))
					bucket_outside_rect.add((Integer) bucket.get(0));
			}
		}
		int count = 0;
		List<Integer> checked = new ArrayList<>();
		for (List bucket : buckets) {
			if (!bucket_outside_rect.contains(bucket.get(0))) {
				bucket_outside_rect.add((Integer) bucket.get(0));
				checked.add((Integer) bucket.get(0));
				String str;
				File file = new File("Buckets/bucket" + bucket.get(0) + ".txt");
				BufferedReader br = new BufferedReader(new FileReader(file));
				while ((str = br.readLine()) != null) {
					count++;
					System.out.println(count + " => " + str);
				}
			} else if (!checked.contains(bucket.get(0))) {
				int bx1 = (Integer) bucket.get(2);
				int bx2 = (Integer) bucket.get(3);
				int by1 = (Integer) bucket.get(4);
				int by2 = (Integer) bucket.get(5);

				checked.add((Integer) bucket.get(0));
				String str;
				File file = new File("Buckets/bucket" + bucket.get(0) + ".txt");
				BufferedReader br = new BufferedReader(new FileReader(file));
				while ((str = br.readLine()) != null) {
					String s[] = str.split(",", 3);
					int x = Integer.parseInt(s[1]);
					int y = Integer.parseInt(s[2]);
					if (x >= x1 && x <= x2 && y >= y1 && y <= y2) {
						count++;
						System.out.println(count + " => " + str);
					}
				}
			}
		}
		for (Object obj : overflow) {
			String str;
			File file = new File("Overflow/overflow" + (Integer) obj + ".txt");
			BufferedReader br = new BufferedReader(new FileReader(file));
			while ((str = br.readLine()) != null) {
				String s[] = str.split(",", 3);
				int x = Integer.parseInt(s[1]);
				int y = Integer.parseInt(s[2]);
				if (x >= x1 && x <= x2 && y >= y1 && y <= y2) {
					count++;
					System.out.println(count + " => " + str);
				}
			}
		}
		return count;
	}
}

public class Grids {
	public static void main(String args[]) throws IOException {
		Scanner sc = new Scanner(System.in);
		int records;
		System.out.print("Enter no of records: ");
		records = sc.nextInt();

		System.out.print("Enter bucket Size: ");
		int bucket_size = sc.nextInt();

		// create dataset
		File dataset = new File("dataset.txt");
		dataset.createNewFile();
		FileWriter file = new FileWriter("dataset.txt");
		Random rand = new Random();
		for (int i = 1; i <= records; i++) {
			file.write(i + "," + rand.nextInt(401) + "," + rand.nextInt(401) + '\n');
		}
		file.close();
		
		// create a folder for all buckets
		File buckets_dir = new File("Buckets");
		if (buckets_dir.exists()) {
			for (File subfiles : buckets_dir.listFiles())
				subfiles.delete();
			buckets_dir.delete();
		}
		buckets_dir.mkdir();
		// create a folder for overflow buckets
		File overflow_dir = new File("Overflow");
		if (overflow_dir.exists()) {
			for (File subfiles : overflow_dir.listFiles())
				subfiles.delete();
			overflow_dir.delete();
		}
		overflow_dir.mkdir();

		// initial bucket
		File initial_file = new File("Buckets/bucket1.txt");
		initial_file.delete();
		initial_file.createNewFile();

		GridFiles grid = new GridFiles(bucket_size);

		File readdata = new File("dataset.txt");
		BufferedReader br = new BufferedReader(new FileReader(readdata));
		String str;
		while ((str = br.readLine()) != null) {
			grid.insert(str);
		}

		// ***************range query part*************
		int x1, y1, x2, y2, count;
		System.out.print("Enter lower left coordinate of query rectangle: ");
		x1 = sc.nextInt();
		y1 = sc.nextInt();
		System.out.print("Enter upper right coordinate of query rectangle: ");
		x2 = sc.nextInt();
		y2 = sc.nextInt();

		System.out.println("Points inside the query rectangle are: ");
		count = grid.rangeQueryNaive(x1, y1, x2, y2);
		System.out.println("total no of points: " + count);
		System.out.println("Points inside the query rectangle are: ");
		count = grid.rangeQuerySmart(x1, y1, x2, y2);
		System.out.println("total no of points: " + count);

		System.out.println("Do you want to print the Bucket Files (Y|N) ?");
		String choice = sc.next();
		if ("Y".equalsIgnoreCase(choice)) {
			System.out.println("\n\n\n\n");
			int totalFiles = buckets_dir.list().length;
			for (int i = 1; i <= totalFiles; i++) {
				String FileName = "bucket" + i + ".txt";
				System.out.println("Grid_" + i + "(<ID Number> <X cordinate> <Y cordinate>)");
				System.out.println("--------------------");
				try {
					File read_grid_file = new File("Buckets/" + FileName);
					BufferedReader grid_reader = new BufferedReader(new FileReader(read_grid_file));
					while ((str = grid_reader.readLine()) != null) {
						System.out.println(str);
						String s[] = str.split(",", 3);
					}
					boolean isoverflow = false;
					for(List<Integer> bkt: grid.buckets){
						if((Integer)bkt.get(0) == i){
							int px1 = (Integer)bkt.get(2);
							int px2 = (Integer)bkt.get(3);
							int py1 = (Integer)bkt.get(4);
							int py2 = (Integer)bkt.get(5);
							for(Object obj: grid.overflow){
								File contents = new File("Overflow/overflow" + (Integer)obj + ".txt");
								BufferedReader content_reader = new BufferedReader(new FileReader(contents));
								while ((str = content_reader.readLine()) != null) {
									String s[] = str.split(",", 3);
									int x = Integer.parseInt(s[1]);
									int y = Integer.parseInt(s[2]);
									if (x > px1 && x <= px2 && y > py1 && y <= py2) {
										if(!isoverflow){
											System.out.println("Content from overflow buckets: ");
											isoverflow = true;
										}
										System.out.println(str);
									}
								}
							}
						}
					}
					System.out.println("\n\n");
				} catch (Exception e) {
				}
			}
		}

		System.out.println("Do you want to print the Logical Space Grids (Y|N) ?");
		String choice_logicalSpace = sc.next();
		if ("Y".equalsIgnoreCase(choice_logicalSpace)) {
			List<List<Integer>> logicalSpace = grid.buckets;
			System.out.println("\n\n\n\n");
			System.out.println(
					"<Bucket Number> (<Lower X cordinate> <Lower Y cordinate>)   (<Upper X cordinate> <Upper Y cordinate>)");
			Collections.sort(logicalSpace, new Comparator<List<Integer>>() {
				@Override
				public int compare(List<Integer> firstList, List<Integer> secondList) {
					return firstList.get(0) - secondList.get(0);
				}
			});
			for (List<Integer> grid_logical : logicalSpace) {
				int id = (Integer) grid_logical.get(0);
				int _x1 = (Integer) grid_logical.get(2);
				int _x2 = (Integer) grid_logical.get(3);
				int _y1 = (Integer) grid_logical.get(4);
				int _y2 = (Integer) grid_logical.get(5);
				_x1 = _x1 == -1 ? 0 : _x1;
				_x2 = _x2 == -1 ? 0 : _x2;
				_y1 = _y1 == -1 ? 0 : _y1;
				_y2 = _y2 == -1 ? 0 : _y2;
				System.out.println(id + "   (" + _x1 + "," + _y1 + ")" + "   (" + _x2 + "," + _y2 + ")");
			}
		}
	}
}