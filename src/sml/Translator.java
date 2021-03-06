package sml;

import sml.exceptions.DuplicateLabelException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Scanner;


/*
 * The translator of a <b>S</b><b>M</b>al<b>L</b> program.
 */
public class Translator {

    // word + line is the part of the current line that's not yet processed
    // word has no whitespace
    // If word and line are not empty, line begins with whitespace
    private String line = "";
    private Labels labels; // The labels of the program being translated
    private ArrayList<Instruction> program; // The program to be created
    private String fileName; // source file of SML code

    private static final String SRC = "src";

    public Translator(String fileName) {
        this.fileName = SRC + "/" + fileName;
    }

    // translate the small program in the file into lab (the labels) and
    // prog (the program)
    // return "no errors were detected"
    public boolean readAndTranslate(Labels lab, ArrayList<Instruction> prog) {

        try (Scanner sc = new Scanner(new File(fileName))) {
            // Scanner attached to the file chosen by the user
            labels = lab;
            labels.reset();
            program = prog;
            program.clear();

            try {
                line = sc.nextLine();
            } catch (NoSuchElementException ioE) {
                return false;
            }

            // Each iteration processes line and reads the next line into line
            while (line != null) {
                // Store the label in label
                String label = scan();

                int idx = labels.indexOf(label);
                if (idx != -1) {
                    throw new DuplicateLabelException();
                }

                if (label.length() > 0) {
                    Instruction ins = getInstruction(label);

                    if (ins != null) {
                        labels.addLabel(label);
                        program.add(ins);
                    }
                }

                try {
                    line = sc.nextLine();
                } catch (NoSuchElementException ioE) {
                    return false;
                }
            }
        } catch (IOException ioE) {
            System.out.println("File: IO error " + ioE.getMessage());
            return false;
        } catch (DuplicateLabelException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    // line should consist of an MML instruction, with its label already
    // removed. Translate line into an instruction with label label
    // and return the instruction
    public Instruction getInstruction(String label) {

        if (line.equals(""))
            return null;

        String ins = scan();
        String instructionCode = ins.substring(0, 1).toUpperCase() + ins.substring(1);

        try {
            Class<?> instruction = null;

            try {
                instruction = Class.forName(this.getClass().getPackage().getName() + "." + instructionCode + "Instruction");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            Constructor<?>[] allConstructors = instruction.getDeclaredConstructors();
            Constructor constructor = allConstructors[1];

            Type[] parameters = constructor.getGenericParameterTypes();

            ArrayList<Object> list = new ArrayList<>();
            list.add(label);

            for (int y = 1; y < parameters.length; y++) {
                if (parameters[y] == int.class) list.add(scanInt());
                else list.add(scan());
            }

            return (Instruction) constructor.newInstance(list.toArray());

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;

        /**
         ***To run the code without using reflection please uncomment:***

         String ins = scan();

         switch (ins) {
         case "add":
         register = scanInt();
         s1 = scanInt();
         s2 = scanInt();
         return new AddInstruction(label, register, s1, s2);
         case "lin":
         register = scanInt();
         s1 = scanInt();
         return new LinInstruction(label, register, s1);
         case "out":
         register = scanInt();
         return new OutInstruction(label, register);
         case "mul":
         register = scanInt();
         s1 = scanInt();
         s2 = scanInt();
         return new MulInstruction(label, register, s1, s2);
         case "sub":
         register = scanInt();
         s1 = scanInt();
         s2 = scanInt();
         return new SubInstruction(label, register, s1, s2);
         case "div":
         register = scanInt();
         s1 = scanInt();
         s2 = scanInt();
         return new DivInstruction(label, register, s1, s2);
         case "bnz":
         register = scanInt();
         String jumpLabel = scan();
         return new BnzInstruction(label, register, jumpLabel);
         }
         */

    }


    private String scan() {
        line = line.trim();

        if (line.length() == 0)
            return "";
        int i = 0;
        while (i < line.length() && line.charAt(i) != ' ' && line.charAt(i) != '\t') {
            i = i + 1;
        }

        String word = line.substring(0, i);
        line = line.substring(i);

        return word;
    }

    // Return the first word of line as an integer. If there is
    // any error, return the maximum int
    private int scanInt() {
        String word = scan();

        if (word.length() == 0) {
            return Integer.MAX_VALUE;
        }

        try {
            return Integer.parseInt(word);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }
}