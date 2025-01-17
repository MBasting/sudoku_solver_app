package com.sudoku.calc;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.opencv.core.CvType.*;

public class Sudoku_Scanner {
    // Array with saved numbers used for recognition.

//    public static void main(String[] args) throws IOException {
//        System.out.println("Welcome to OpenCV " + Core.VERSION);
////        TNumber[] template_numbers = getNumbers();
//
//        int[][] img = scan("Resources/test_images/test_2.jpg");
//        System.out.println(Arrays.deepToString(img));
//        int[][] sol = Solver.solve(img);
//        System.out.println(Arrays.deepToString(sol));
//
////        String result = scanImage("Resources/test_images/test_1.jpg");
//    }


    /** Prepares the image for recognition by converting to gray scaling and rescaling (speed up)
     *
     * @param img Matrix of image to be resized
     */
    public static void prepare(Mat img) {
        int rows = img.rows();
        int columns = img.cols();
//        if ((double) rows / columns != 1) {
//            if (rows > columns) {
//                int middle = rows / 2;
//                int range = columns / 2;
//                rows = range * 2;
//                img = img.submat(middle - range, middle + range, 0, columns);
//            } else {
//                int middle = columns / 2;
//                int range = rows / 2;
//                columns = range * 2;
//                img = img.submat(0, rows, middle - range, middle + range);
//            }
//        }
        int height = 720;
        System.out.println("default rows/ columns: " + rows + " " + columns);
        int width = (int) (columns / ((double)rows/ height));
        System.out.println("height: " + height + "  width" + width);
        Size newsize = new Size(width, height);
        Imgproc.resize(img, img, newsize, 0, 0, Imgproc.INTER_AREA);
        Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2GRAY);
    }


    /** This functions extracts the sudoku from a taken picture
     *  Based on size and ratio of width and height
     *
     * @param gray Image to be analyzed in gray
     * @return Image of sudoku with tight borders or null if no/multiple sudoku's are found
     */
    public static Mat isolateSudoku(Mat gray) {
        Size s = new Size(8, 8);
        Mat blur = new Mat();
        Imgproc.blur(gray, blur, s);
        Imgproc.GaussianBlur(blur, blur, new Size(5,5),0);
        Mat edged = new Mat();

        Imgproc.Canny(blur, edged, 0, 50);
        Imgproc.dilate(edged, edged, Mat.ones(3, 3, CV_32F));
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(edged, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        int index = 0;
        int nr_of_sudokus = 0;
        List<Integer> possibile = new ArrayList<>();
        // The first objective is to find the sudoku
        // We do this by finding the contour that satisfies a certain size and is also square
        for (MatOfPoint p : contours) {
            MatOfPoint2f temp = new MatOfPoint2f();
            Imgproc.approxPolyDP(new MatOfPoint2f(p.toArray()), temp, 3, true);
            Rect temprec = Imgproc.boundingRect(new MatOfPoint(temp.toArray()));
            double ratio = (double) temprec.width / temprec.height;
            // Add contour if certain height and square
            if (temprec.height > 0.3 * gray.size().height && ratio > 0.8 && ratio < 1.2) {
                possibile.add(index);
                nr_of_sudokus++;
            }
            index++;
        }

        if (nr_of_sudokus == 0) {
            System.out.println("Not enough or too much sudokus!");
            return null;
        }
        int save = 0;
        int max_width = 0;
        for (int a : possibile) {
            MatOfPoint temp = contours.get(a);
            Rect rect = Imgproc.boundingRect(new MatOfPoint(temp.toArray()));
            if (rect.width > max_width) {
                max_width = rect.width;
                save = a;
            }
        }
        MatOfPoint sud = contours.get(save);
        Rect rect = Imgproc.boundingRect(new MatOfPoint(sud.toArray()));
        return gray.submat(rect);
    }

    /** This function finds all the numbers present in the sudoku based
     *  dimension of the input sudoku.
     *  Note: the input matrix must be a tight sudoku, so no empty space around border of sudoku
     *
     * @param image_roi Matrix of sudoku
     * @return Array of region of interests of the number and the contours
     */
    public static List<Isolated_Number> isolatenumbers(Mat image_roi) {
        Mat thresh = new Mat();
        Imgproc.threshold(image_roi, thresh, 140, 255, Imgproc.THRESH_BINARY);
        List<MatOfPoint> contours1 = new ArrayList<>();
        Mat hierarchy1 = new Mat();
        List<Isolated_Number> numbers = new ArrayList<>();
        List<MatOfPoint> debug_numbers = new ArrayList<>();
        Mat zeros = Mat.zeros(image_roi.size(), CV_8U);
        Imgproc.findContours(thresh, contours1, hierarchy1, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        System.out.println("NUMBER OF CONTOURS FOUND: " + contours1.size());
        Imgproc.drawContours(zeros, contours1, -1, new Scalar(255));
        // For each contour we have found in the image check its dimensions
        int count = 0;
        for (MatOfPoint p : contours1) {
            Rect temprec = Imgproc.boundingRect(new MatOfPoint(p.toArray()));
            double margin = image_roi.size().width / 10;
            // TODO: ARE THESE REALLY THE BEST VALUES TO CHOOSE
            if (image_roi.size().width / 50 < temprec.width && temprec.width < margin &&
                    temprec.height > image_roi.size().height / 25 && temprec.height < margin) {
                debug_numbers.add(p);
                Mat numb_temp = image_roi.submat(temprec);

                // TODO: FIND OUT WHY IT IS 0.7
                int posx = (int) ((temprec.x + 0.7 * temprec.width) / (image_roi.size().width / 9));
                int posy = (int) ((temprec.y + 0.7 * temprec.height) / (image_roi.size().height / 9));

                Isolated_Number temp = new Isolated_Number(posx, posy, numb_temp);
                numbers.add(temp);
                count++;
            }
        }
        zeros = Mat.zeros(image_roi.size(), CV_8U);
        Imgproc.drawContours(zeros, debug_numbers, -1, new Scalar(255));
        System.out.println(count);
        return numbers;
    }


    /** This function extracts all numbers given as Isolated_Number and puts them in the correct spot.
     *
     * @param Isolated_Numbers List of Isolated_Numbers,
     *                         Each Isolated_Number consists of its position in the sudoku and the ROI.
     * @return Recognized Sudoku as double int array.
     */
    public static int[][] recognize_set(List<Isolated_Number> Isolated_Numbers, TNumber[] tNumbers) {
        int[][] sudoku = new int[9][9];
        // Prepare the array by setting all values to zero.
        for (int[] ints : sudoku) {
            Arrays.fill(ints, 0);
        }
        for (Isolated_Number numb: Isolated_Numbers) {
            int label = recognize(numb.roi, tNumbers);
            sudoku[numb.y][numb.x] = label;
        }
        return sudoku;
    }


    /** Recognizes a number given as Mat. Done by the absolute difference.
     *
     * @param num image of the separate number as Mat.
     * @return calculate value of the num as int, zero if Mat was not a valid size.
     */
    public static int recognize(Mat num, TNumber[] numbers) {
        if (num == null) {
            return 0;
        }
        Mat thresh = new Mat(num.size(), CV_8U);
        Imgproc.threshold(num, thresh, 140, 255, Imgproc.THRESH_BINARY_INV);

        int width = (int) (((double) 70 / num.size().height) * num.size().width);
        if (width > 100) return 0;
        Imgproc.resize(thresh, thresh, new Size(60, 70));
        Scalar sum1 = Core.sumElems(thresh);
        int s1 = (int) sum1.val[0];
        if (s1 > 350000) {
            Imgproc.morphologyEx(thresh, thresh, Imgproc.MORPH_OPEN, Mat.ones(3,3, CV_8U));
            Imgproc.morphologyEx(thresh, thresh, Imgproc.MORPH_ERODE, Mat.ones(5,5, CV_8U));
        }


        int distance = Integer.MAX_VALUE;
        int label = 0;
        for (TNumber n : numbers) {
            int number = n.number;
            Mat res = new Mat();
            Core.absdiff(n.img, thresh, res);
            Scalar sum = Core.sumElems(res);
            int s = (int) sum.val[0];
            if (s < distance) {
                label = number;
                distance = s;
            }
        }
        //if (distance > 300000) {
        //    return 0;
        //}
        return label;
    }

    /** Combines all previously defined functions. Reads the image and returns sudoku.
     *
     * @param path Path to the to be recognized sudoku (doesn't have to be isolated sudoku);
     * @return Sudoku as double int array.
     */
    public static int[][] scan(String path, TNumber[] numbers) {
        Mat img = Imgcodecs.imread(path);
        prepare(img);
        // Extract the numbers from the taken picture
        Mat sudoku = isolateSudoku(img);
        if (sudoku != null) {
            System.out.println("Finished with 1 sudoku!");
            List<Isolated_Number> Isolated_Numbers = isolatenumbers(sudoku);
            return recognize_set(Isolated_Numbers, numbers);
        } else {
            System.out.println("COULD not recognize sudoku");
            return null;
        }
    }




}
