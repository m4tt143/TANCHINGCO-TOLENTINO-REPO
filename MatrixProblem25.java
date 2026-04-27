public class MatrixProblem25 {

    public static void main(String[] args) {

        int M11 = 6; 
        int M12 = -3; 
        int M13 = 2;

        int a21 = 4; 
        int a22 = 1; 
        int a23 = 3;
        
        int a31 = 2; 
        int a32 = -1; 
        int a33 = 1;
        
        int determinant = (M11 * ((a22 * a33) - (a23 * a32))) 
                        - (M12 * ((a21 * a33) - (a23 * a31))) 
                        + (M13 * ((a21 * a32) - (a22 * a31)));



        System.out.println("[ " + M11 + " ((" + a22 + " * " + a33 + ") - (" + a23 + " * " + a32 + ")) ] - " +
                           "[ " + M12 + " ((" + a21 + " * " + a33 + ") - (" + a23 + " * " + a31 + ")) ] + " +
                           "[ " + M13 + " ((" + a21 + " * " + a32 + ") - (" + a22 + " * " + a31 + ")) ]");
        

        System.out.println("det(A) = " + determinant);
    }
}
