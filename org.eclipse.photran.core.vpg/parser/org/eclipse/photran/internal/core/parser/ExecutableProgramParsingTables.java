/*******************************************************************************
 * Copyright (c) 2007 University of Illinois at Urbana-Champaign and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     UIUC - Initial API and implementation
 *******************************************************************************/
package org.eclipse.photran.internal.core.parser;

import org.eclipse.photran.internal.core.lexer.*;                   import org.eclipse.photran.internal.core.analysis.binding.ScopingNode;                   import org.eclipse.photran.internal.core.SyntaxException;                   import java.io.IOException;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;


import java.util.zip.Inflater;

import org.eclipse.photran.internal.core.parser.Parser.Nonterminal;
import org.eclipse.photran.internal.core.parser.Parser.Production;

@SuppressWarnings("all")
abstract class ParsingTables
{
    // Constants used for accessing both the ACTION table and the error recovery table
    public static final int ACTION_MASK   = 0xC000;  // 1100 0000 0000 0000
    public static final int VALUE_MASK    = 0x3FFF;  // 0011 1111 1111 1111

    // Constants used for accessing the ACTION table
    public static final int SHIFT_ACTION  = 0x8000;  // 1000 0000 0000 0000
    public static final int REDUCE_ACTION = 0x4000;  // 0100 0000 0000 0000
    public static final int ACCEPT_ACTION = 0xC000;  // 1100 0000 0000 0000

    // Constants used for accessing the error recovery table
    public static final int DISCARD_STATE_ACTION    = 0x0000;  // 0000 0000 0000 0000
    public static final int DISCARD_TERMINAL_ACTION = 0x8000;  // 1000 0000 0000 0000
    public static final int RECOVER_ACTION          = 0x4000;  // 0100 0000 0000 0000

    public abstract int getActionCode(int state, org.eclipse.photran.internal.core.lexer.Token lookahead);
    public abstract int getActionCode(int state, int lookaheadTokenIndex);
    public abstract int getGoTo(int state, Nonterminal nonterminal);
    public abstract int getRecoveryCode(int state, org.eclipse.photran.internal.core.lexer.Token lookahead);

    protected static final int base64Decode(byte[] decodeIntoBuffer, String encodedString)
    {
        int[] encodedBuffer = new int[4];
        int bytesDecoded = 0;
        int inputLength = encodedString.length();

        if (inputLength % 4 != 0) throw new IllegalArgumentException("Invalid Base64-encoded data (wrong length)");

        for (int inputOffset = 0; inputOffset < inputLength; inputOffset += 4)
        {
            int padding = 0;

            for (int i = 0; i < 4; i++)
            {
                char value = encodedString.charAt(inputOffset + i);
                if (value >= 'A' && value <= 'Z')
                    encodedBuffer[i] = value - 'A';
                else if (value >= 'a' && value <= 'z')
                    encodedBuffer[i] = value - 'a' + 26;
                else if (value >= '0' && value <= '9')
                    encodedBuffer[i] = value - '0' + 52;
                else if (value == '+')
                    encodedBuffer[i] = 62;
                else if (value == '/')
                    encodedBuffer[i] = 63;
                else if (value == '=')
                    { encodedBuffer[i] = 0; padding++; }
                else throw new IllegalArgumentException("Invalid character " + value + " in Base64-encoded data");
            }

            assert 0 <= padding && padding <= 2;

            decodeIntoBuffer[bytesDecoded+0] = (byte)(  ((encodedBuffer[0] & 0x3F) <<  2)
                                                      | ((encodedBuffer[1] & 0x30) >>> 4));
            if (padding < 2)
               decodeIntoBuffer[bytesDecoded+1] = (byte)(  ((encodedBuffer[1] & 0x0F) <<  4)
                                                         | ((encodedBuffer[2] & 0x3C) >>> 2));

            if (padding < 1)
               decodeIntoBuffer[bytesDecoded+2] = (byte)(  ((encodedBuffer[2] & 0x03) <<  6)
                                                         |  (encodedBuffer[3] & 0x3F));

            bytesDecoded += (3 - padding);
        }

        return bytesDecoded;
    }
}

@SuppressWarnings("all")
final class ExecutableProgramParsingTables extends ParsingTables
{
    private static ExecutableProgramParsingTables instance = null;

    public static ExecutableProgramParsingTables getInstance()
    {
        if (instance == null)
            instance = new ExecutableProgramParsingTables();
        return instance;
    }

    @Override
    public int getActionCode(int state, org.eclipse.photran.internal.core.lexer.Token lookahead)
    {
        return ActionTable.getActionCode(state, lookahead);
    }

    @Override
    public int getActionCode(int state, int lookaheadTokenIndex)
    {
        return ActionTable.get(state, lookaheadTokenIndex);
    }

    @Override
    public int getGoTo(int state, Nonterminal nonterminal)
    {
        return GoToTable.getGoTo(state, nonterminal);
    }

    @Override
    public int getRecoveryCode(int state, org.eclipse.photran.internal.core.lexer.Token lookahead)
    {
        return RecoveryTable.getRecoveryCode(state, lookahead);
    }

    /**
     * The ACTION table.
     * <p>
     * The ACTION table maps a state and an input symbol to one of four
     * actions: shift, reduce, accept, or error.
     */
    protected static final class ActionTable
    {
        /**
         * Returns the action the parser should take if it is in the given state
         * and has the given symbol as its lookahead.
         * <p>
         * The result value should be interpreted as follows:
         * <ul>
         *   <li> If <code>result & ACTION_MASK == SHIFT_ACTION</code>,
         *        shift the terminal and go to state number
         *        <code>result & VALUE_MASK</code>.
         *   <li> If <code>result & ACTION_MASK == REDUCE_ACTION</code>,
         *        reduce by production number <code>result & VALUE_MASK</code>.
         *   <li> If <code>result & ACTION_MASK == ACCEPT_ACTION</code>,
         *        parsing has completed successfully.
         *   <li> Otherwise, a syntax error has been found.
         * </ul>
         *
         * @return a code for the action to take (see above)
         */
        protected static int getActionCode(int state, org.eclipse.photran.internal.core.lexer.Token lookahead)
        {
            assert 0 <= state && state < Parser.NUM_STATES;
            assert lookahead != null;

            Integer index = Parser.terminalIndices.get(lookahead.getTerminal());
            if (index == null)
                return 0;
            else
                return get(state, index);
        }

        protected static final int[] rowmap = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 1, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 2, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 15, 65, 66, 67, 68, 3, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 0, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 18, 129, 0, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 7, 142, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 38, 155, 156, 119, 157, 158, 159, 160, 161, 162, 163, 164, 165, 166, 15, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 34, 199, 200, 0, 201, 202, 104, 1, 31, 60, 0, 106, 203, 204, 205, 206, 112, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 143, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 91, 237, 238, 239, 240, 241, 242, 243, 244, 245, 224, 1, 2, 3, 34, 1, 7, 126, 4, 127, 15, 5, 130, 231, 246, 128, 6, 131, 7, 129, 143, 0, 247, 183, 152, 8, 153, 248, 154, 104, 31, 9, 155, 156, 216, 157, 116, 31, 132, 10, 158, 11, 249, 223, 12, 13, 225, 0, 14, 226, 227, 2, 133, 228, 160, 229, 230, 250, 15, 16, 232, 31, 237, 238, 240, 17, 241, 32, 251, 252, 18, 118, 253, 254, 19, 255, 20, 256, 257, 258, 259, 260, 261, 262, 137, 140, 0, 21, 162, 263, 264, 265, 266, 267, 22, 23, 268, 269, 24, 1, 270, 271, 3, 25, 272, 273, 274, 26, 27, 164, 166, 28, 251, 275, 276, 246, 252, 277, 278, 4, 279, 280, 34, 29, 34, 256, 281, 282, 283, 0, 60, 42, 284, 285, 257, 247, 5, 255, 0, 258, 259, 286, 287, 288, 289, 290, 291, 292, 293, 294, 295, 42, 296, 30, 297, 298, 190, 6, 299, 300, 301, 260, 302, 303, 304, 248, 305, 306, 60, 307, 7, 308, 309, 310, 311, 312, 313, 314, 315, 316, 317, 31, 42, 318, 31, 319, 320, 32, 321, 8, 322, 33, 323, 324, 325, 0, 1, 2, 326, 31, 34, 327, 328, 329, 249, 330, 106, 331, 332, 333, 59, 9, 334, 261, 250, 262, 263, 7, 264, 265, 266, 267, 269, 335, 271, 272, 336, 253, 10, 191, 11, 337, 338, 35, 339, 91, 340, 274, 341, 342, 343, 275, 192, 254, 280, 344, 345, 346, 281, 284, 347, 348, 104, 349, 350, 351, 352, 353, 354, 12, 36, 37, 355, 13, 14, 15, 16, 0, 356, 357, 17, 18, 19, 20, 21, 358, 359, 0, 360, 22, 361, 23, 24, 26, 38, 362, 363, 364, 365, 366, 367, 368, 28, 369, 370, 371, 372, 373, 374, 375, 376, 377, 378, 379, 380, 381, 382, 383, 384, 385, 386, 387, 388, 389, 390, 391, 392, 393, 394, 395, 396, 39, 29, 40, 30, 290, 41, 42, 33, 397, 398, 399, 400, 35, 36, 401, 43, 44, 45, 46, 402, 37, 39, 40, 41, 47, 48, 403, 404, 405, 49, 406, 50, 51, 407, 52, 53, 54, 55, 56, 1, 408, 409, 410, 411, 412, 57, 58, 2, 59, 61, 62, 413, 63, 3, 64, 414, 65, 66, 67, 0, 415, 68, 416, 69, 70, 47, 4, 417, 71, 72, 418, 6, 73, 419, 420, 3, 421, 4, 48, 74, 5, 75, 422, 423, 424, 6, 76, 425, 426, 77, 78, 7, 427, 79, 80, 81, 295, 82, 428, 83, 429, 84, 85, 430, 49, 50, 431, 86, 8, 87, 88, 432, 89, 433, 434, 1, 435, 436, 437, 438, 439, 440, 126, 90, 441, 91, 92, 442, 9, 93, 94, 51, 95, 10, 96, 97, 0, 7, 11, 98, 99, 100, 443, 12, 444, 101, 102, 1, 103, 105, 107, 13, 108, 14, 0, 109, 445, 110, 111, 112, 113, 114, 446, 115, 116, 447, 117, 119, 120, 121, 448, 122, 449, 450, 451, 123, 15, 452, 453, 454, 455, 456, 457, 458, 124, 125, 459, 126, 460, 127, 17, 129, 195, 461, 462, 7, 463, 130, 132, 18, 133, 134, 464, 465, 466, 467, 25, 135, 20, 15, 136, 137, 468, 21, 469, 470, 471, 131, 472, 473, 138, 474, 52, 475, 476, 139, 140, 55, 0, 141, 142, 143, 144, 145, 477, 146, 22, 478, 479, 480, 481, 147, 56, 148, 146, 149, 150, 151, 482, 483, 484, 153, 154, 156, 158, 23, 7, 159, 485, 486, 487, 488, 489, 490, 91, 491, 492, 160, 493, 161, 7, 151, 162, 279, 259, 296, 299, 494, 163, 57, 495, 496, 164, 497, 498, 499, 500, 501, 165, 502, 503, 263, 504, 505, 183, 179, 166, 506, 507, 508, 509, 510, 167, 511, 512, 168, 513, 514, 515, 516, 169, 517, 2, 518, 519, 59, 170, 520, 521, 522, 523, 524, 525, 171, 526, 527, 528, 529, 172, 173, 530, 531, 532, 104, 180, 533, 534, 174, 535, 175, 536, 537, 538, 539, 15, 306, 27, 540, 176, 541, 308, 542, 317, 543, 318, 544, 320, 18, 177, 178, 181, 182, 24, 184, 545, 546, 547, 185, 548, 323, 549, 550, 551, 15, 326, 552, 7, 8, 60, 9, 10, 553, 11, 554, 555, 556, 12, 178, 557, 17, 186, 558, 315, 559, 58, 0, 3, 560, 561, 562, 563, 564, 565, 566, 567, 568, 569, 570, 571, 18, 572, 184, 573, 574, 575, 28, 189, 576, 577, 578, 217, 579, 30, 580, 31, 19, 581, 582, 583, 187, 188, 584, 189, 585, 190, 586, 191, 587, 588, 589, 590, 591, 592, 593, 594, 33, 595, 596, 597, 598, 599, 600, 34, 601, 42, 43, 44, 602, 603, 604, 605, 45, 606, 607, 608, 609, 610, 611, 612, 613, 614, 615, 616, 617, 618, 619, 620, 621, 622, 623, 624, 625, 626, 627, 628, 629, 630, 631, 632, 633, 634, 635, 636, 637, 2, 638, 331, 3, 639, 333, 46, 49, 65, 68, 640, 641, 4, 59, 642, 50, 51, 643, 192, 644, 645, 193, 646, 647, 648, 649, 650, 5, 651, 6, 652, 13, 15, 653, 654, 655, 26, 656, 657, 658, 194, 659, 660, 195, 196, 661, 52, 662, 663, 664, 665, 666, 667, 197, 198, 668, 199, 669, 197, 670, 200, 16, 671, 672, 673, 674, 675, 676, 677, 84, 85, 87, 88, 89, 94, 678, 201, 679, 95, 680, 681, 101, 682, 683, 7, 684, 102, 104, 106, 29, 21, 685, 686, 202, 687, 688, 109, 110, 118, 120, 122, 61, 689, 690, 691, 692, 693, 694, 695, 124, 8, 22, 23, 696, 697, 698, 337, 203, 699, 700, 204, 701, 702, 0, 703, 704, 338, 9, 705, 706, 707, 708, 709, 710, 711, 712, 713, 714, 715, 716, 125, 4, 717, 718, 719, 126, 128, 136, 720, 137, 62, 205, 138, 139, 140, 147, 721, 148, 150, 151, 722, 152, 159, 160, 723, 6, 157, 161, 162, 206, 207, 63, 208, 209, 724, 64, 66, 198, 67, 69, 70, 725, 726, 10, 11, 727, 728, 729, 730, 731, 732, 733, 734, 735, 736, 28, 30, 32, 737, 738, 739, 740, 741, 742, 743, 744, 745, 746, 747, 748, 749, 750, 751, 220, 752, 753, 754, 755, 756, 757, 758, 759, 760, 163, 761, 164, 762, 763, 764, 165, 765, 766, 767, 768, 769, 770, 771, 772, 773, 774, 775, 776, 777, 778, 779, 780, 781, 782, 783, 784, 785, 786, 25, 26, 27, 34, 787, 788, 789, 790, 791, 166, 792, 167, 793, 168, 235, 169, 794, 210, 795, 211, 796, 797, 170, 798, 35, 799, 800, 801, 802, 803, 804, 302, 805, 171, 806, 807, 808, 809, 810, 811, 812, 813, 814, 172, 815, 816, 817, 818, 173, 819, 820, 821, 822, 823, 12, 824, 825, 826, 827, 828, 829, 830, 831, 71, 7, 174, 175, 832, 833, 834, 835, 836, 837, 838, 839, 840, 841, 176, 36, 179, 180, 842, 184, 185, 212, 1, 186, 72, 192, 193, 194, 195, 196, 73, 197, 198, 199, 201, 205, 206, 207, 208, 60, 33, 843, 844, 209, 213, 75, 214, 215, 76, 340, 845, 40, 846, 217, 218, 219, 220, 221, 222, 223, 847, 224, 13, 848, 213, 214, 849, 215, 850, 851, 852, 853, 854, 35, 225, 77, 855, 856, 226, 227, 8, 857, 343, 858, 228, 230, 78, 859, 346, 860, 231, 232, 233, 234, 861, 862, 350, 863, 216, 864, 235, 236, 237, 865, 866, 217, 218, 867, 219, 868, 869, 220, 870, 871, 872, 221, 352, 0, 222, 268, 355, 873, 874, 223, 875, 876, 43, 224, 225, 877, 878, 229, 226, 879, 880, 881, 882, 230, 883, 231, 884, 885, 886, 42, 233, 887, 234, 888, 889, 890, 891, 79, 239, 240, 892, 60, 82, 44, 83, 86, 45, 50, 90, 53, 91, 54, 893, 241, 242, 243, 894, 895, 235, 896, 244, 236, 897, 898, 899, 237, 900, 34, 38, 36, 245, 247, 37, 356, 91, 238, 901, 38, 902, 239, 903, 39, 246, 248, 2, 40, 249, 84, 250, 253, 41, 254, 904, 251, 905, 906, 907, 1, 908, 359, 909, 910, 252, 55, 241, 911, 912, 256, 257, 262, 242, 913, 914, 243, 915, 916, 244, 917, 918, 249, 85, 259, 260, 263, 56, 268, 269, 0, 252, 271, 272, 919, 273, 274, 275, 253, 920, 921, 922, 276, 277, 278, 280, 282, 283, 284, 285, 286, 287, 288, 289, 290, 1, 923, 291, 292, 293, 294, 295, 296, 924, 297, 925, 926, 298, 300, 927, 928, 299, 301, 929, 302, 303, 304, 930, 305, 306, 931, 932, 44, 57, 933, 934, 935, 936, 937, 938, 939, 940, 941, 942, 307, 308, 943, 944, 945, 946, 947, 948, 949, 950, 951, 952, 953, 954, 955, 956, 957, 309, 59, 61, 62, 63, 64, 66, 67, 69, 70, 71, 72, 73, 76, 77, 958, 254, 0, 959, 310, 960, 311, 961, 78, 962, 312, 963, 964, 965, 255, 261, 313, 314, 263, 315, 357, 316, 318, 966, 967, 317, 319, 320, 321, 322, 968, 264, 324, 325, 327, 328, 104, 329, 330, 42, 332, 969, 323, 326, 331, 334, 335, 970, 333, 971, 972, 973, 265, 336, 339, 341, 342, 974, 975, 976, 343, 977, 344, 43, 338, 79, 340, 345, 346, 347, 348, 80, 81, 349, 350, 978, 351, 979, 266, 980, 352, 981, 353, 354, 355, 358, 2, 982, 983, 984, 985, 366, 367, 370, 372, 87, 373, 986, 397, 378, 386, 388, 391, 392, 393, 88, 399, 400, 361, 401, 402, 363, 403, 987, 988, 405, 406, 989, 990, 407, 991, 992, 993, 409, 410, 14, 994, 995, 412, 413, 360, 45, 0, 408, 1, 411, 2, 414, 418, 417, 92, 89, 415, 419, 996, 93, 96, 97, 420, 92, 997, 998, 999, 267, 93, 268, 1000, 1001, 1002, 422, 1003, 3, 1004, 1005, 1006, 1007, 1008, 97, 1009, 98, 1010, 1011, 1012, 425, 1013, 4, 1014, 1015, 427, 1016, 1017, 98, 6, 1018, 1019, 1020, 99, 1021, 1022, 1023, 1024, 269, 1025, 1026, 271, 99, 100, 1027, 273, 1028, 426, 428, 429, 430, 432, 433, 434, 435, 3, 46, 436, 103, 2, 47, 437, 438, 439, 440, 441, 442, 443, 444, 100, 445, 446, 447, 448, 449, 451, 452, 453, 454, 455, 456, 459, 460, 461, 462, 463, 465, 466, 3, 274, 467, 468, 469, 470, 471, 472, 473, 474, 475, 477, 478, 479, 480, 481, 275, 482, 281, 483, 484, 485, 1029, 103, 492, 493, 494, 4, 282, 486, 487, 495, 488, 5, 497, 1030, 499, 489, 283, 285, 490, 491, 496, 498, 500, 501, 502, 503, 1031, 286, 504, 505, 506, 507, 508, 509, 510, 511, 512, 513, 514, 515, 516, 517, 1032, 1033, 518, 519, 1034, 1035, 1036, 287, 520, 521, 4, 105, 107, 522, 1037, 523, 1038, 1039, 1040, 1, 524, 1041, 525, 112, 48, 1042, 1043, 526, 527, 528, 1044, 288, 1045, 1046, 289, 529, 1047, 290, 7, 1048, 1049, 291, 1050, 1051, 1052, 1053, 108, 530, 532, 531, 1054, 533, 535, 1055, 292, 1056, 534, 362, 1057, 536, 1058, 293, 294, 538, 540, 542, 1059, 1060, 1061, 1062, 537, 1063, 1064, 1065, 297, 1066, 1067, 1068, 303, 397, 1, 304, 109, 1069, 0, 1070, 1071, 1072, 305, 1073, 1074, 1075, 1076, 1077, 1078, 110, 104, 105, 106, 118, 120, 122, 1079, 123, 127, 129, 132, 1080, 1081, 107, 1082, 1083, 49, 1084, 1085, 364, 1086, 543, 544, 545, 546, 547, 548, 549, 365, 1087, 133, 1088, 1089, 116, 50, 1090, 51, 1091, 5, 550, 553, 52, 551, 135, 552, 108, 554, 555, 126, 556, 1092, 1093, 401, 1094, 307, 1095, 1096, 557, 1097, 558, 560, 1098, 561, 1099, 1100, 310, 109, 1101, 111, 562, 563, 564, 565, 566, 567, 568, 1102, 1103, 569, 570, 571, 1104, 559, 1105, 574, 1106, 1107, 572, 1108, 1109, 1110, 1111, 1112, 1113, 141, 1114, 1115, 573, 1116, 1117, 1118, 575, 576, 577, 578, 579, 1119, 1120, 1121, 580, 581, 6, 7, 584, 582, 1122, 583, 585, 309, 1123, 1124, 1125, 313, 586, 1126, 314, 1127, 311, 587, 588, 1128, 1129, 1130, 112, 590, 593, 595, 596, 597, 598, 2, 1131, 1132, 1133, 127, 53, 591, 54, 599, 1134, 315, 600, 1135, 1136, 1137, 1138, 316, 601, 1139, 1140, 1141, 1142, 1143, 1144, 1145, 1146, 605, 611, 1147, 1148, 613, 620, 1149, 622, 319, 1150, 1151, 623, 625, 142, 321, 1152, 626, 1153, 1154, 143, 1155, 1, 1156, 1157, 603, 604, 1158, 636, 630, 113, 9, 606, 632, 15, 1159, 607, 1160, 1161, 1162, 1163, 1164, 55, 608, 609, 1165, 1166, 638, 144, 610, 337, 1167, 345, 1168, 145, 146, 153, 639, 56, 1169, 1170, 1171, 1172, 1173, 642, 1174, 641, 1175, 643, 346, 644, 347, 646, 1176, 647, 114, 1177, 1178, 10, 648, 650, 651, 653, 1179, 654, 1180, 655, 1181, 656, 649, 348, 657, 115, 1182, 1183, 11, 1184, 658, 659, 350, 1185, 351, 1186, 660, 149, 1187, 1188, 1189, 154, 1190, 155, 1191, 353, 1192, 356, 357, 1193, 612, 1194, 1195, 1196, 0, 1197, 1198, 1199, 1200, 1201, 1202, 661, 1203, 1204, 116, 366, 1205, 1206, 1207, 614, 615, 616, 57, 662, 1208, 663, 664, 1209, 665, 1210, 1211, 666, 1212, 1213, 1214, 1215, 156, 667, 668, 1216, 1217, 669, 670, 1218, 0, 1219, 1220, 1221, 8, 158, 169, 617, 1222, 618, 1223, 177, 178, 1224, 367, 403, 1225, 671, 1226, 672, 1227, 673, 1228, 1229, 675, 674, 676, 1230, 12, 619, 370, 1231, 621, 181, 1232, 678, 1233, 682, 372, 683, 373, 378, 1234, 386, 684, 1235, 1236, 368, 685, 686, 1237, 408, 388, 369, 1, 1238, 1239, 391, 1240, 1241, 117, 1242, 118, 1243, 392, 1244, 393, 1245, 58, 3, 4, 624, 628, 1246, 128, 59, 397, 1247, 398, 629, 1248, 9, 1249, 182, 633, 634, 1250, 1251, 687, 184, 411, 691, 645, 689, 693, 694, 695, 129, 637, 1252, 399, 688, 119, 1253, 120, 1254, 1255, 1256, 185, 1257, 696, 13, 1258, 697, 698, 699, 1259, 700, 14, 703, 1260, 701, 1261, 15, 17, 18, 1262, 702, 1263, 1264, 1265, 1266, 186, 704, 1267, 1268, 705, 708, 1269, 706, 401, 707, 709, 371, 710, 711, 1270, 1271, 1272, 712, 713, 714, 715, 2, 130, 60, 122, 716, 717, 718, 1273, 1274, 719, 1275, 400, 1276, 374, 123, 124, 0, 125, 126, 725, 721, 187, 61, 62, 723, 726, 63, 727, 188, 64, 728, 1277, 402, 1278, 729, 730, 731, 732, 733, 734, 735, 736, 737, 738, 1279, 739, 740, 1280, 741, 1281, 742, 127, 743, 1282, 744, 189, 745, 1283, 1284, 746, 1285, 747, 1286, 190, 200, 1287, 1288, 1289, 403, 748, 404, 1290, 749, 750, 1291, 128, 1292, 1293, 751, 1294, 19, 405, 129, 1295, 1296, 752, 753, 754, 8, 1297, 1298, 1299, 20, 406, 132, 1300, 755, 756, 1301, 407, 2, 192, 194, 193, 415, 422, 1302, 757, 1303, 1304, 758, 65, 759, 195, 760, 761, 133, 762, 763, 764, 1305, 765, 766, 767, 425, 1306, 1307, 135, 1308, 1309, 1310, 1311, 768, 769, 1312, 770, 430, 1313, 1314, 1315, 196, 771, 772, 773, 1316, 431, 774, 775, 776, 412, 777, 9, 197, 778, 10, 11, 1317, 779, 780, 1318, 1319, 1320, 432, 1321, 441, 375, 1322, 442, 1323, 1324, 443, 1325, 1326, 138, 1327, 139, 1328, 1329, 1330, 1331, 1332, 376, 198, 781, 1333, 377, 131, 444, 66, 379, 1334, 782, 783, 784, 785, 786, 787, 788, 1335, 1336, 789, 1337, 790, 1338, 132, 67, 791, 1339, 1340, 1341, 199, 200, 792, 793, 1342, 794, 795, 1343, 796, 798, 1344, 1345, 1346, 799, 1347, 1348, 1349, 1350, 1351, 445, 10, 800, 11, 12, 1352, 1353, 801, 802, 803, 21, 22, 201, 804, 1354, 203, 1355, 68, 797, 1356, 805, 1357, 1358, 1359, 806, 1360, 807, 1361, 813, 1362, 808, 1363, 809, 810, 811, 812, 814, 815, 816, 12, 817, 818, 446, 69, 819, 1364, 140, 1365, 820, 13, 1366, 23, 821, 141, 1367, 1368, 1369, 1370, 1371, 447, 822, 14, 1372, 448, 142, 1373, 1374, 1375, 1376, 1377, 450, 824, 1378, 449, 451, 1379, 456, 1380, 1381, 459, 1382, 1383, 1384, 1385, 6, 17, 1386, 1387, 1388, 1389, 204, 1390, 827, 828, 823, 829, 1391, 830, 831, 380, 13, 205, 207, 1392, 416, 1393, 460, 461, 15, 1394, 17, 1395, 208, 1396, 1397, 462, 1398, 1399, 1400, 145, 146, 7, 8, 832, 833, 834, 825, 463, 826, 381, 1401, 1402, 464, 418, 1403, 1404, 420, 835, 14, 836, 206, 841, 1405, 70, 209, 212, 465, 466, 843, 844, 845, 1406, 1407, 1408, 846, 847, 1409, 1410, 1411, 1412, 1413, 1414, 1415, 15, 848, 1416, 1417, 849, 839, 842, 1418, 1419, 382, 213, 214, 215, 1420, 1421, 1422, 850, 851, 852, 853, 1423, 854, 204, 1424, 1425, 1426, 24, 467, 1427, 1428, 1429, 1430, 468, 470, 855, 469, 1431, 1432, 856, 1433, 1434, 1435, 1436, 471, 472, 857, 473, 1437, 1438, 1439, 225, 205, 1440, 71, 858, 859, 1441, 0, 238, 860, 861, 474, 245, 1442, 862, 863, 864, 1443, 865, 1444, 1445, 866, 421, 1446, 1447, 867, 1448, 868, 1449, 475, 1450, 1451, 1452, 1453, 383, 384, 385, 1454, 72, 477, 478, 387, 870, 871, 872, 873, 874, 869, 875, 1455, 480, 18, 1456, 147, 148, 1457, 1458, 1459, 876, 1460, 1461, 1462, 1463, 1464, 877, 16, 878, 879, 880, 881, 1465, 882, 479, 1466, 1467, 883, 884, 1468, 885, 886, 887, 888, 481, 1469, 1470, 501, 502, 889, 482, 1471, 1472, 150, 1473, 890, 503, 891, 487, 1474, 1475, 151, 1476, 498, 1477, 1478, 1479, 152, 892, 1480, 504, 893, 1481, 894, 506, 895, 896, 897, 898, 899, 507, 1482, 389, 390, 1483, 900, 1484, 422, 901, 1485, 154, 155, 156, 1486, 1487, 902, 903, 905, 908, 909, 904, 1488, 1489, 1490, 1491, 1492, 906, 1493, 907, 910, 911, 1494, 1495, 510, 1496, 1497, 157, 1498, 1499, 25, 1500, 158, 1501, 1502, 26, 206, 912, 1503, 1, 1, 1504, 913, 915, 423, 394, 395, 396, 511, 512, 916, 425, 1505, 1506, 1507, 246, 247, 1508, 917, 918, 1509, 914, 1510, 919, 1511, 1512, 926, 927, 248, 920, 928, 1513, 1514, 27, 513, 1515, 1516, 28, 523, 1517, 1518, 249, 160, 929, 931, 398, 1519, 404, 938, 250, 251, 252, 514, 522, 253, 254, 256, 1520, 1521, 943, 1522, 956, 958, 524, 1523, 1524, 526, 527, 1525, 1526, 529, 959, 18, 960, 528, 530, 536, 539, 1527, 1528, 961, 963, 964, 257, 258, 1529, 541, 1530, 1531, 543, 1532, 262, 406, 1533, 1534, 1535, 965, 966, 1536, 1537, 967 };
    protected static final int[] columnmap = { 0, 1, 2, 3, 4, 5, 2, 6, 0, 7, 8, 9, 10, 11, 2, 12, 13, 14, 15, 16, 17, 18, 19, 20, 8, 1, 21, 3, 22, 6, 23, 24, 25, 2, 26, 2, 6, 27, 0, 28, 29, 30, 31, 26, 32, 30, 33, 34, 0, 35, 9, 36, 37, 38, 39, 18, 2, 6, 9, 40, 14, 41, 42, 43, 32, 44, 45, 46, 47, 48, 18, 49, 38, 50, 20, 1, 50, 51, 3, 52, 26, 53, 54, 34, 55, 40, 56, 56, 57, 58, 46, 59, 60, 0, 61, 62, 63, 38, 2, 64, 0, 65, 66, 51, 41, 67, 24, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 36, 79, 80, 81, 38, 55, 82, 44, 83, 84, 0, 85, 86, 67, 87, 66, 88, 89, 90, 91, 46, 4, 92, 0, 93, 94, 2, 95, 1, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 67, 69, 108, 109, 110, 5, 72, 111, 112, 73, 113, 74, 3, 114, 3, 70, 115, 116, 45, 117, 118, 4, 119, 80, 3, 75, 120, 121, 122, 123, 124, 125, 7, 126, 127, 128, 129, 130, 131, 132, 9, 133, 1, 86, 11, 134, 135, 91, 97, 136, 137, 138, 101, 139, 1, 107, 140, 141, 142, 143, 144, 145, 0, 146, 147, 148, 149, 150, 151, 152, 153, 109, 154, 2, 112, 83, 155, 156, 157, 158, 1, 159, 107, 3, 160, 161, 0, 162, 163, 164, 165, 166, 6, 4, 167, 86, 4, 168 };

    public static int get(int row, int col)
    {
        if (isErrorEntry(row, col))
            return 0;
        else if (columnmap[col] % 2 == 0)
            return lookupValue(rowmap[row], columnmap[col]/2) >>> 16;
        else
            return lookupValue(rowmap[row], columnmap[col]/2) & 0xFFFF;
    }

    protected static boolean isErrorEntry(int row, int col)
    {
        final int INT_BITS = 32;
        int sigmapRow = row;

        int sigmapCol = col / INT_BITS;
        int bitNumberFromLeft = col % INT_BITS;
        int sigmapMask = 0x1 << (INT_BITS - bitNumberFromLeft - 1);

        return (lookupSigmap(sigmapRow, sigmapCol) & sigmapMask) == 0;
    }

    protected static int[][] sigmap = null;

    protected static void sigmapInit()
    {
        try
        {
            final int rows = 1083;
            final int cols = 9;
            final int compressedBytes = 3126;
            final int uncompressedBytes = 38989;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrVXb1vHMcVf7NYCUOGAlYHQqCDRBgpIUCpMBT3NpaSDFgfQR" +
                "hBiqAQiA+BA6hwABcuBXhMUAHdKVaqVIaApHCRxv8AixQqUth1" +
                "VBlJ4yRAmvTZ292724/5+M3suyW1AAUd+eObNzPve94sX5376u" +
                "rTL+8kD8T0zoXs7Vv7+v0nb+WH9787Or7yz4u/TzURvfrp86tP" +
                "X/xy8q6Y7s8wd2aY3cNffHf08sp/FpjPrz799vaF7Rlm8sNbd4" +
                "4ePrlX0nl55d8XnwVggLHOPQd49o+Vkii+iPLii7JrxT9be9eq" +
                "z0okn86+C859zPXhWcPykeR8QDq6+3tZjw4Pz8cbn18W09vJGa" +
                "LvXUg25B19duce6fTyUbFhlDHvxWnb0wLz4vbkTMlPgdEPn0y6" +
                "mHMlJjlT6kUTc3zlH0HzYsIAerqxRuuHBwmRmO5Rdn6X6OcfZ/" +
                "nhvZd0fOXmxUq0Xv3sq7/86dv9C/9bm/79wVtvP/rk6P0nv77x" +
                "u1/9azbWO9/AY6X1VyWkWaHm2V4hq7P/KFo/ZpVVhE4pz+r22e" +
                "2ZPFMhz0dndyY9eR7PJvxgTRR7MaFcTHPKflLsxaOPr+WHe7O9" +
                "ePfipwHrM6Z+MeqpRXc+qDGYzRyRZ0jfETo8/pSLTiWHSVsOd9" +
                "tyWOxDYTTKr8W+ZPNPik6l706bglLzmi++oceXHwDDJvOQbLyO" +
                "sdZ4mN4j4mK2cTEjriGXHRsx/unYsWz+j+rq14DY78Ma87clZt" +
                "/m40ZdH7ZcD5RD3xry5VaJLSfSATZzs6BzuIxXC/+R3c8K0mW8" +
                "mmjWeKyWjYkr/uFaHyzvHk/f6zi8yiupyivj4vDn8zxl/8Fkka" +
                "dUc2/mKS8qnjdrni+fpI0aMQ5Pc5GY912qz+p9Z8MYZEx1ZAyh" +
                "Y6pvGJ4DXw0EwSD1DQTz1wpT5ZVJlVcuMc9YMVhNxi8/XGOF0t" +
                "ms6VyP4Bm2Y17fDdixhn/frOkknVwPqqUw1WRiMdTBFOuXzPOi" +
                "3FKTaWJ2h2F69R9a0VjdRxn0nWusMWtNczpDfSU3Py46TcxmjU" +
                "k6GGPtq1NzwPTUj0HGei0w+XhjqW4d0prvvGzHq56aXhm3fLnf" +
                "3q/dik4rRvJgCqOfQOcgFhv+QYAvQOpjvLLaHktF1OK4MB2b8K" +
                "i0Cddrm/COMU8xYrjGipoXNTBknzutip/+h1YlkuZxlLLFWgE1" +
                "K6kIezBciv7MUl+FHumHcNV2VomhCDpGGevt6cnOq88zjw1Ham" +
                "iu2o5cyIbdzn8YXpNx1uuamCE+xTQv6tWs/Dk+UrOa09RLPUu7" +
                "vhKhMyamuz6pwb/HjkUR/HDV65p0bPLTyOPyRq5XxgB3F3kcUG" +
                "8hcdaXLyMYsMbIMtbGLfvc5zksV/0QodPEbNaYbi6DnBPxYZTL" +
                "dOC+XfD4ZYiunU4OEFM9QmlS2UixMA4LOqn060VBRrRMjDDUD6" +
                "XKi+9rOcecLyhVxiJb/GIZa8mG0U79K2z4bsGzSNOSR13yLEqe" +
                "ZfF5KpZ10SHrHL+nykrHu4bQvOB4lWPuqudasghZRWzvcuFKmZ" +
                "BZ9LyMMibr4QXM80wvmntBtr0wyvNiLGzufgPUqQ1SVRuUvbhl" +
                "JbYFoSOssiEGjxXET+KjI0axz5XuiKF+B5If1Cbk7ggasz/w3N" +
                "12A9oVuRB+m/9yrHNuIJ9ax4XmZfUQhrkz6xf4ZFGyymQTCPIX" +
                "Ypk2t+xGFoQ5HXFCDti6QN8E6DKAwfwXj+8GfVx4dCVi7SGPf0" +
                "fkxxI/u9dZGOwGnx9E9IsnhkQ4XVcmGyWFJ5bo2x/ExykwPgyM" +
                "+UWQXaU4mzlM5qN00DQW0nMC6BfYh3YArBpLXwrWv+qXje2t34" +
                "r0DIn3DoXWb/yX6MbW15dIyb2UPjqffKbCc7QUlyZhd8nOc4f4" +
                "PW3ZKK7ejN4hiPVx/1zX+5UN9JVNWX3Yk9Vv6npdWC9EbunfAM" +
                "+REtWxEFmY5YPp8N054sEg/RtNjK3GeBI8u3o8SjmEXZjDJjDV" +
                "f7j6QAx+5769tuNwnoieAj0VCM/2MzJpHKt97hA2VmxvhtFG+e" +
                "hAsnHJ63ew8zjlD1Ih2zteL0TUWNTvhYilQ1F0gJgWc6Z+DCQ/" +
                "sl01MtRkouYVaceQPhDE8HLdb4qQeXu/sXdeQPDXLfK5I3g7Jm" +
                "a/jNbfX9Pj6vGAbKZDJ2QQHeWwq4JVNhA6GsovlLt2SHH3idDe" +
                "A4rrc6jm5+jNmM+/ibHZQr0URxOdU4Vh7G/x7lfsvseccWCSHF" +
                "G3CavPh8XPQf6ipYNpcC13YB/I3f59hyAM9XtpkN4Vb15gooNg" +
                "YvptkD4ZE4Y6GL4eIZ5+m6aUJHYNAHozoNBPzH5B5uum2vIxKs" +
                "9I3+A8tzpec+VxPLocUlePqMJIC0YDGBqGYTsH8Z8lxdTn3Rht" +
                "P4uc8qzhTn1Wkq8rukkHxackk5kqpvqsUIuOPCfdsxsza97cyk" +
                "Vn6ud5XXXLnyVGBMsG0xrS4zad+bxSQ81cC02Wmvn21m8qzEGN" +
                "uV5jyIDRdkwqt6nAUK5nsvpRsdxbX89l9fyy1p1X/PbsYZDNhM" +
                "aaraHwnSn454XwjKwhMi/EN/V4vhG3XyCmXOf3Zus8YF5zOnmf" +
                "zpRfDi95ZSNofSB+/OuzYvmhAeuzZ9RTgekpqMvazzMNsi2IrQ" +
                "s6F04D/LuI87nIPXG2OCo+j2vzjPW3+OPeIwDDFY9JpAy7A6zh" +
                "Tvg6x2KQXggoluCKN4LoDMMAPaWIHHL1HqRyYQ70sgeYOrUdnj" +
                "ici2eLjMnBmBXmRNhZJHS/EujF4rGHo64P21h+DFaHHHFeUG+z" +
                "Gne/Uq/8xPq4Dp2duXZqbcn1ViaHYV37eZNqUv52ots6mOakFS" +
                "/PkT3bUXII1S6iq2IyvHYB0IH2dCljZJUxZCzVpoPsl4ir20A9" +
                "gWE8D5MxOWJMy+RPQX5EuT7ts+Ou/fFikJ43Pn3300HuSnPFdW" +
                "H9mcNyvQF1Yy7ffXJxr0Hm43rIO/0SYfq+F1LTY7MtVa1yRq7t" +
                "c9OSHw8dsaCjq6Uue2onjfNTdWIx5CmLAaAcP6ImI0bkOWTVUo" +
                "9W5yfJM5ccAjwXsV/x4fH69I+zu/1/pm0Sf/gi+yL90SRL8voc" +
                "lqv+A5nP8daQb08ROjahU8nJ1FKmZWwjSXRj2iwOQ8Mwi/qGox" +
                "8A4kdRy1+Yx+rF4Y6821WHhPlxP4/bdJZxZuD6QPUf4CwSiLV2" +
                "yv6NHZl/f/bpx1ooeqaLnFFNfPzExvzAfdixMBRLRzoxeK0yPB" +
                "aF+/BXZn9WYnsH3uEdEGsJjvqqoy5xIvVVKz9Rd8BNdvWIdqUn" +
                "vyjoSJ2Uv1+QF6IcNaf8iI7lzQ3vLrL7btlyAu0yUO5ew/AYiS" +
                "vm55J5Pr/DpIPAWOsRpYfYvtx1s4yJUJ5X0H+oh9XHYnPG1ZzH" +
                "sZ1b+XWH7W8YIe9WZYvnI3Jz8MkwO7aSuKWgusvRY7aKc0/NhB" +
                "meV/o2jimuw86XvfVDrvfS4PcvksF02OJnJjqwqnYkLu58GXqH" +
                "QA9jKLkFvfdADHiHAJfNRN4hMOBMKkIHue5fnC6Zj71r3zWbEJ" +
                "2g/VrctzIU7y4x7fvzN4F3er/Jcmf/lO07dL8bixOQvzsAvlc2" +
                "cA0Fi75b5i798Tx61taEms7RIGfpnxd031w2Farbf4jTQd4Rjc" +
                "aHPn64bDi4Pjx2fjVn2Y8GxYeRcqhgngVsV/l8NzDWiHE4dD6I" +
                "3HGe257h99/9Y0HvDo2oNZnfkchyR56Y7r9Dd+2BfQfv7G/V9d" +
                "We97q5WD0l5hPS9ho1gmGqNfHIBvT3FJDe+BFjfq47+1x5AdLT" +
                "gfAc+P4E7ZQN4cZcyu6KAlOeseq1Io/cJS1IySyd7XCPEQ3IoR" +
                "5Wq8TpDH6/MRc/SMEBf3e6Zyy/bCD9h9B9NCDE7vGMzyscA9WN" +
                "o/17MB3k7yEi+z5y36BfDmMwA3UZ1Ith+hVwrufugRnRJgTZMa" +
                "41hELJ7txHsasG+fHeO2aTebYeRaZeES6eITqPefy7fb8W5yC9" +
                "+93mO/JD7GFjLMs9+tQ9Vld3oPdLAPGYR56TADufM50PRtVbMp" +
                "+2J86528ear6GLjjELt2AcdOB3hlTvRujuV29eIIasGHOEFGMP" +
                "TyuGThmd1wmD+HdW2wK+M8Ql81ExSf9vuoHvI8LnPmh9uN4Vw2" +
                "VbVuBPrb6b/g+Iq60C");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            sigmap = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                sigmap[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] sigmap1 = null;

    protected static void sigmap1Init()
    {
        try
        {
            final int rows = 1083;
            final int cols = 9;
            final int compressedBytes = 2797;
            final int uncompressedBytes = 38989;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtXb2PHLcVfxyMBOp8BnhrQ1CCROApEbByESjubXCdM2DrYm" +
                "AjOBFsFd4iAtQEMAyXKmhhBZw7x2lSGi5S+K/YIoUKF07vykia" +
                "IAHyJ2Rm9mtmdmb4I/lmbk++MXDy7r4lHx/J9/Hj49tjdSIo+0" +
                "9mf+jagsiQFaRJESlF8hbljyTnk2bfTyn/evaou9mfG9O7y9da" +
                "JJ/t0Nu2hvj7skDrHTSySiNWb6Z+NKnMeL5S8GjXPOuMJns92/" +
                "Ass48FWUnJ8vVRRpX/+2o+Hes3N+1bQh+x+9b37/3ltWdffzh6" +
                "W8weXldvvHtqP3ry+mT+h3+fPb/z35t/Tjsa95bh9+99+dqzH+" +
                "5dv533Nfr5u6dnD57cN/P3877+c/OLVV9jJ8/I2shoJslShrZd" +
                "hmNnM4V8fvjw+qOC50w+Zx89+aQuH9nIZQvPSde4nPOOydA4tx" +
                "fWjnubtsyFLDMBtQPQIDxz0XQ8xmcdQvsLWc+ZLnb3BYxrtVYl" +
                "HWzWoVr+X/7PAu0L4uflbOzfnCa/F7PTfOwP87EX/Czu/GtXt7" +
                "TrXmy/M86FLHGU7mgjrnbY5AytZ80in7Q8EpUvGTVdbQq9VrWY" +
                "TdHYLDps3OLwy1tC37t6m+il63QoT8+ujkdk01tneQ8K3l/gnE" +
                "5a9PN0o9jBtZo02oK0TpN9ktjSPs3eTU2DhKxD914smu3Yazoq" +
                "G7vV8L7olKHVIfy0t8PYV4t3EOKzCUBnCsKeSP85mIa8fQnM18" +
                "o/tVKV9nK+/ZRQtDGJ47b58rVNxqk3IKsmm3RjLQYJtY4hNLLZ" +
                "V09950I30UhfP/z2jT+JLLYS78yFtT/5H9Fvbnx3TFpOU/r4KP" +
                "lcs+4vSegjgDUvoFYi/ech/dWan2ByPyHHEgo/4WDRpcPr+8vf" +
                "5m59WpcfJX3jL4gfJABH9A+bnN1r7PAaHcyf5hZsNiV1NCH63a" +
                "fKzO8/p8Wdk5vWRz5ADMuzxoaMB2s0H4TSgFo2Ka0ltf6jw+1p" +
                "F86GzGmEjROqYV/Ykrrv9vmTTjyBWtvZ8rNs55WCH9myTzv4kZ" +
                "uNZttsXD4zmAbP8dX6fE0953TVDnW14+4LwxiRvtZvcvFTxuvq" +
                "8wXIUFZptj5SED+0Kx9Po8uGPwPjQtYhgjkg3gkPdkEHuzRqNV" +
                "g/fkAa46khY/rCcOxhYj2meBDEGCF8ngXr7imy6RUjQrBlLhrf" +
                "sdsd95I1LmCLrS5pnPhGyKMu5cxPg9j3IdvZt3HFYWi+ccqQNG" +
                "IbzlH9rO2FXvOF7f76XnKlOPvLbLd98GS0Ovv7Y+0sMhazGtLu" +
                "MGOD8ZheFI1mx1KabJMC7FcQDaZ/3HuQWT83xMLKy+YiqxySz6" +
                "wtflde/jw9bouXffVYU4xPnZhMQ+tw/obg1C2Ir7UnNF5+HdSO" +
                "9VmHgePysO/RfXnY5Ugfe//kzL3GOvYpV6yneXASoB0PnCSaZ2" +
                "hcQA4VF07Sg28D6ajesdNhcQnkvNJJk8pjSlMSJiVr8zxhsZsn" +
                "XOQSp85cYpa+InKSZfi0R8/7JCLvwgCa3TT50t5eI5a/YUL3V9" +
                "y4zpHmRR1XvzTSJoXssveEKD4yZM5oIU8OnXI0TfrwwY4+/Ee4" +
                "LbiIa6Ohr1cz/THfONcq28LqfZWpFFPKg2XDASQ6zFI73blhcX" +
                "qVrS+ediLuMnjnYkH7Yu/yuiH/kCW3h83XAnRU0G4JvQPFhYf7" +
                "0bQqhdA7LCquHbZxtc8F07hebsef/9mNEQ2Z2xySf4g8T3H8Zw" +
                "gdBcqneS4MtwyLOyOzbG3kd0aSQ3lqr47v1++M7JsOR+5SSSae" +
                "EcwBwiV4bKVHjqvDZEB9Jeu+xKov2u0r0TWJ1vfX+l7SlfW9JN" +
                "t4LwmIqXnWGBtmTvu1xhr6SkP4+dk1cTB/OiIjZobUrydEH3x6" +
                "dzKf5vnPb9/8DKZB9mC2L3717JuH1XFNlnPhuB/XTztcc1HNW1" +
                "5u1yQE9vNe88tc67dW6/lNXK9CPPvfy27mxy+O68g35vE3/r7U" +
                "US12sOsetKgLVzTEcSFwL9IOV19c/AzWToQO78VfXZTXT4uNg+" +
                "5EhO73ID+TR69iNAPG71vcb2eFnQCLy7j3iSfWHYGHN7Bjt99M" +
                "675WUhqXLAc8ptyQdmPt4XhmK3bqef99aJyEgwY5v4jnZ/izAH" +
                "+eofMmYF8g9xnj8Drvu4quhwsP77keUS/xe4SeD9CZwTQBmOeA" +
                "GAhTLMyHoWlQ1TRsmXM6Bym9aV3wH4d/WDRqGr+6wGWI9GWcNN" +
                "D95SGx3MbwqGbFoNwMd44HlAcyqP6Jwnu95h2joQvYDkCzngqT" +
                "KeqTAvNPSCq9vKqcLKp4VHttjRdWPnLGM3ZEhuPVu+ZgOReCEl" +
                "WfC679BeRnRtS4i+Cn63nMauO6aDZ1YKyw1FYHBmjnWP12XRPV" +
                "2rwm6qSoiSpVSlO1XhTA2GF+BByD2Pa+HlX7emvVF2372jcaxG" +
                "dD2knl7WVtVZvbwY+zb974bm0Hjza28rhOM63TQDyjMYjLxjGN" +
                "fUtDsfNVyKernVQ+csqZa74YxwXwc8zCD6tOCNOHdEnzgtNIpI" +
                "mAGLaZxCeP2sb5CUAsjOJajrNstrip7o+FYyDAM/anEZ0QX6Qf" +
                "jvDjpcc6QYsIn02tfTa+uRgST0BoZsge5MHZmmsN9YQnzCD52K" +
                "2/LNZucxWg9+An9ddc5HtGFl47fQDMKpyGKycQwt6xvBQmvPfc" +
                "MXx/+Qx4fmHyfIC77yTZ5nsppUM6tVflKHs/zweg7rzK4HNhGu" +
                "quGUdual80SaifCc8FUh+bOuuZ4+3sTd1v6Lc/kHENqTcwTLjJ" +
                "B5AVHwDBM3nrCAlwHQ5F4/afXe141NisvDr/se+TnKvyaW4Hoa" +
                "k+R6XCun2PK7QvHxypq+Y5ZFOA2vJI3C2ROeXqS1fjbhGKOWhg" +
                "TnXo2vDXhw15F0H1zAGayLsVpvWThNCnWyJIO0mf7azmvVr/Wf" +
                "rHwl6+aJzdgWJP7cNDK88bvCUVlrZ4C5XxFj5fFKjFDeX/APsL" +
                "xUkEuLxd9cPdzbjrmQM6E7YXrhrskHykz7g65guyO8768+AdDV" +
                "PhfB2DVOpsA3c00Prh7hpl7naQ2unh/PQ17zFro6EmfEf9ebZc" +
                "WeRuDvvYbRs/EXciKs+3y7vSo65and9u71M/bKOBcKSDZj0vyF" +
                "sf8ugWtnr4AM2QOa77mO+3Rxh1hL9KFFv3qWv9pBTynA/WtHdr" +
                "Y2BM2EljLsLYL2n2lUYXK+hxMvtrpheSv2Wvxfwr9VX6ixElhj" +
                "vHHqQRl/N1cWi0j9Pia3N7waMi842Nn55318fmo+lk/hz28sxd" +
                "09urr65xAfLBcsjbfgNLDbSeA+bCXEAapnGNKQdfxtL8NH/1Sy" +
                "s0fWGVTvXI87fmGcf1o71ztG80UXq+RKMB49a73fHFM5n0z7nE" +
                "wr5xE/nuQSwv170vwBq2LPW624N3nThXjO/vtQFzKiuCqaanGB" +
                "88YcjfimKiCcrr9sPraufdlmEvQ3nvTHqDEaMm11mJ5uqraZa8" +
                "9w5fHTxiyn8G1jyTTuC7y6mxtYH8XgC+3zcPeFYrvfvq1y7v0g" +
                "yUPw/WN/bIt4Hm3XbOhcMHQGq0suWHX8yaqJs6pRMwb0fHxLkO" +
                "fQjrVeWSc1vd5ufbc0ZAzlCtTqa6oFAtOKb6vWAOA1MdzoDzbg" +
                "dNezuM8YWLBs19GoofJp6xfAD3fPHVNkTjC1kafbrLHRZfFCvc" +
                "btOt0rCa541678fy2zE8Mvw/twR86w==");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            sigmap1 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                sigmap1[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] sigmap2 = null;

    protected static void sigmap2Init()
    {
        try
        {
            final int rows = 1083;
            final int cols = 9;
            final int compressedBytes = 2790;
            final int uncompressedBytes = 38989;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrlXU1vFEcQ7R4NVkNAGhxkOVKCxkhIhhPJnWiWgAQ4SCtEgo" +
                "ADPgTJh0SKohx9aJCRnFtC/gHH/AofcuCY3H1CyiVKpPyEzM5+" +
                "eHZ3pvtV95vBTuZgsNVbU91dXf3qVXWvUrPHqrbHKO9zeO/nqy" +
                "9fP169pbefrGXX72zZp7ufDPa+/HP/zZW/L/6UToQbm5Q/dfV/" +
                "Xf/8Tf8ripo+g/LT1qhk/Pt5pdZH/15Qw/JnIpBTiOU0/jUf/5" +
                "PODaJZaqNDRnZB5+b5apNjAdkW1kc7+u7sl79NMq/I+E1Ze+fb" +
                "5Bzee3X15du7a5dHdrj60Z2t/Ye794u9ByM7/Ovij2M7BEY9VX" +
                "olnb47u1b+WB9eG/+e6+S5wOZzytqB+gXMKSxHB9lY2LsYcqCV" +
                "lDcZs5ba/OG5Up/Xd5NTentrLSv1sQ93Vyt9Dq58vdwv91sI49" +
                "xnm43sc13qrI3S1p4+UKUntlrlJktVlimT47bRn82z2mD6AP7Q" +
                "LGwmjbaxM6+PnnwynZPj37+gPW6nue+pbKe6vP6NTk8pfdtqqz" +
                "74R6nP1n/bKG1jmKpvzyc/5B59sqk+kXKGR3KezeTYkZwbDXKa" +
                "16B8lwb6tWRhybJ0Yd9te98BOTXc4t27e2kDrFPjBGLk/WJhbR" +
                "7NVzskSQCLSYD3Jk5DbPcJwBimRqs0rTphp9imnB1T/r49xTY8" +
                "v7rp309Z81XGBYl3DW4qweOyQ/m7dKPOWoZbvLZqxZ5LPhd+Xy" +
                "fxhxgSW9S5+A+2mR8E8exFxeZTwynGShQSpQtZPMiRw4q/WNgP" +
                "w7Qs3CvqV+sYFmdfXdL57dKVqfdSdVZt2RVTLk5zab8KfKt3tb" +
                "RR9TY1TqaOSQzoE+p4dRDhW2py8iaPYkLWxUSO9axldxtTKVa2" +
                "SWZ7d1b9NS2UzY/0eft47Vk176U++093v+tIH0HfPXsKwm+Q9p" +
                "RuYz35novykD7eb2wb1mQ1OSNMl+lsxoLNQuu5mFHeL+RdCFCF" +
                "dF6Wk2Esi5G2mcU7e5NYrzHe2ZxfF4H4uf6rrWuWhpOwBHvWsT" +
                "hhYvNagA8fVWv5xmQtfyrC6smcYZSfmZrSdOlBPgrZLzC8oSEO" +
                "38/tDIA9DliDlT56SZ+U4sdMgK8D/DOH647IXyiAq1RLXGVz3A" +
                "TA3vp+Cq2LgoNt2tromZxGfXw8f9KIW+T7ctK4LjA52iOnOY8W" +
                "kNvKJQ55Uc6woV/tuFe4pzi4AsR+RI91B6La2fdZGw20ccrxr0" +
                "HMfvzvCrFDJw/ptJz58TnCG0OxrVJsvgW3vCsb63sMGXEKTQ6I" +
                "6zy5iYMqNr+7crmMzdfUWbO1v7K5qmw6is3zo9iczddZENdl0x" +
                "+5sF80bkecFw5vE6Gz3LeY1tlIxVhLE8awglOmpk+6PNvGb0Es" +
                "Do3FJ2C8X3s9wFeieoDcG871yUOyant4nDBnvjDfu9O272TAXl" +
                "nPrXvlIDpXfn67tLGRn09KP29XNu8v+vkeeewWfrW7WI9Sj3Sk" +
                "z8MlfX4PWRcI3phvU3OLqdkY5WF1kSprR3lY3ZyH9S0UKJ+77/" +
                "eHwrywlyKMjd+lOaC2OXXsaoXYGzH6RcIkF8rR35sRq9nIpTzI" +
                "ygkpRh+ehKMR/RLXXSD1CcevJpBTU9HB3uQwoHfO6YnzDqx8E6" +
                "9+NeesCyg2749fpWJa/zi/WHxpFjSG/vk6PFfazy9byRcVxr5+" +
                "58nIfiYY+4+J/ZRLNpku20GWlW4wG5b6ZJU/PHMgmfckX5i9xn" +
                "5lSEzkfj48rc/svVhVhd4uVPbxQKlH318b7A3fqIMrty4+l9iP" +
                "N5+C6APykCI5NkqOX+dfxxxICzbGeRtMjiBOSYHupeK+h+Sk/A" +
                "ZfW3ymbt8Fpf4HmXfTKCdV0icG17VDHBo+pOUZSZxeiz7CPCOE" +
                "RZt9/omv5cPOLslima7r2RA+inbOhVTHKDz3Z2PkCM+5uHIcRa" +
                "DOAXUgO4I4jmc/Noa3IY1P/ayHqp/1UAtnPTjEhGTebdf9aj3r" +
                "oebOjHgf4RjaqJh6cQ2+P8ubI7WgwrVjvFwcxm8geAyo+SfFRM" +
                "KQJJ479dWm8jAJcFYaxMYUOW1ANE8aBq9QUfXzwH7abc5FjJ/7" +
                "5Xb8+AfKiQeE81ANhg4O9gA5Rs1h9ba60y7OFCdK8hTAmBRsHy" +
                "VcX/303VuHz7H5MY+UeHgkuO/R9dizNgmpjToubVpqyMN4LU3A" +
                "LcB+KsE/kM4K0Bmqja+tL/cY6ib/LNrjfHBee/gWqK6bojNWH+" +
                "6VE1Ez0L5rJYtObvKxBm45w2NhBj4MwL0QV0mqW/a3gWrIgfUF" +
                "1X6jdcuUOlhkESJzCuAfZq0jgqO8bZD6VeNdeMRz2SdvvrAx9L" +
                "ahnqvNgDaeGtcZx5iWOrvOp3j3L0Rnf5s+azNod+lAOJyFEzqK" +
                "uzuN8fMm/MXKO5hOdQZci9eeGXW5LA4k7h4hIefZUneq5upXgW" +
                "e77YxPJpyvQcTZf+Q+K9/erRRmVQW7DVajmMu5pnidW+1HmM9F" +
                "YliECrPAgl/c34XvAtr0yFGz9kp4P9UMDABhJH8NFVSPROJ7kf" +
                "ooWp0Vumw9PAlJDlLXBNU+mdr77NLC0rDOGIKrRtUehRApmNuS" +
                "+0ySPlgNVe7nwzBu2T8+4fV18jxRTY7tHB/K9IG5C93IWQE5aN" +
                "K7OqjlU7K7lJEm8vwFzydI1ru0TchdlKQ7izj3r0J3xgL6sO5x" +
                "ZY1PVA1Myq+BEd2tKnkSgB/A7yCVL7CQHEeX6wvlYLlOoQ8s4X" +
                "2XsKYiivuSnkuK8oeBHHVQXZPsbihLGB/tRMOkvDB215mRrC8F" +
                "xO/Wg9k0ykNaRhuJc0sc4+x8l8L7FfYgnEwb1uo2P7joexMBtx" +
                "OOkeJqiUn5ph7uQBbd5QXnu9HxicbheW+5v+N2rxrYd28u8qDO" +
                "t6gx37Jco8jhx2hnSwHuC6s5yWV2aJuZWvyeCjdnJcJaDt5mI7" +
                "s5PTOiqnxuMcnnVgy6uaRADo2VS4rI53bEI4n2ble8jNzNAjxI" +
                "bgt4V8PYecsDYu7N8OS2kHs8eHdHkHwUbW9iyVHI98JwsAQpx8" +
                "HLgyA1nII6z+NyDprNLceeTWZx+P3eJ0OpMcPyy0g8CMd6qJw+" +
                "+R9pPBiwX+TOYN9hG1rOFQA8Nul7zRAOFj7PiHwvp6cNSx/Wd5" +
                "+BMSxyBpPEY5uYeY/7XiGXD4/dL/jn49wYUjN8wkKbJCwXAPGQ" +
                "tDumoDasuzUo5y+wcxM0zpzEs+XIfbBd69wBJ5P3dmaE9h0Z8p" +
                "r2E8KPHa+5gGOrns5SReTjVBd7nHj/sjF7U6exnlgfrO8IR0Ti" +
                "mqD68Ny7dhCOiPU9oUis1+t9nlw52oszdTy3Q7xDCUbwntpLTh" +
                "tavCzgIdPYvue8+0n6wrSsmEjMNYkuzZhrTLt/DGgDvYuFowCf" +
                "CXEXJBwe+a7Qe8ziuAL0XQRup+PzcfMOreG7VlfV0netYlwBPj" +
                "4WXu9x9zBo0EdZB2eO3e1MiGGxepuTF+/0W0eEnN3+P48PgFtI" +
                "NQxBPKSGcdRyzaT37C2C2YC4ibY3IfqIziE62nQa53ZynrpfzB" +
                "+ARRv2JlGe0YUBesSiUBvmmRrKfXFI7k+R7lRB8HN/Z1gw3xtQ" +
                "Q95s8yjPz6vHjtSHhaOO3d5NyrmcRJ2h3BaTW8bxoeO7BUX3gb" +
                "h8nZenBe9P6I9ng7gvGYdmI/jenvlMPDMRWe+H4THovCe+Tu3c" +
                "Ogw/o+qQA+2n/wJf5gRp");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            sigmap2 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                sigmap2[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] sigmap3 = null;

    protected static void sigmap3Init()
    {
        try
        {
            final int rows = 76;
            final int cols = 9;
            final int compressedBytes = 368;
            final int uncompressedBytes = 2737;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNpTlUhlZGFlYPRoYGxgkPzAwOAkcUGRQYEjgIGhQJCpU4EBBh" +
                "jBZAMDAnAwoAPy1KABBWyCJNt1x3eSVusjLzFVxoRoMSHZSO+O" +
                "8Jogp5aQVx0nNN/YTWBpgOhwAJrTwMHABNEkyMAgAaJFGASAJB" +
                "O6lQ343MzIgB8QY9cgc88d38larYuihFxBYShg6+ndEF9j5NgS" +
                "BgrDt3ITWRqIdQ72uEMDGSBRRg4GxgYkNRwMLGD3QEECqhpEvC" +
                "Opgbi9AZoaWIBIgHBqwgYIm0NsWiU+fChUo0CEdgVS4qiB1mpU" +
                "iSl/iMoXRMQFtczBnoo5aJMHiSjHiLFrMLqHQDgrCrgwAs0B5X" +
                "cGBs4DDEDXNTAC9QEdIyDAwKFEfJlArXxBHTV3eIFhuMiLiZUx" +
                "wVtMQBZYrobXCDmAwvCAZo4cJAyJUUNS+dOAXKqRqYYK+Z1a6Y" +
                "cYc+hpF5FlOME6hVppg3rph7AaqsYFIfcAANWo4L0=");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            sigmap3 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                sigmap3[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int lookupSigmap(int row, int col)
    {
        if (row <= 1082)
            return sigmap[row][col];
        else if (row >= 1083 && row <= 2165)
            return sigmap1[row-1083][col];
        else if (row >= 2166 && row <= 3248)
            return sigmap2[row-2166][col];
        else if (row >= 3249)
            return sigmap3[row-3249][col];
        else
            throw new IllegalArgumentException("Unexpected location requested in sigmap3 lookup");
    }

    protected static int[][] value = null;

    protected static void valueInit()
    {
        try
        {
            final int rows = 57;
            final int cols = 85;
            final int compressedBytes = 4596;
            final int uncompressedBytes = 19381;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNqtWw2wVVUVvmnjD/AQ9SkQT/QpApKkVEIgwX73vXcJxqwQxF" +
                "F5Rhpq6CBQggj6zjn7nPs76JjxOzmNIJo1NdU0U0kZij/9a439" +
                "WPZjOipOZun01MZqnb3OOmutfe7DxpE7d+29/r71rf3uOWefey" +
                "6VrkpX9HilK3019pdKwebgEdTAPjQ4utJVO0B6+go/WemqP4Fz" +
                "u77SZQ6HvHsqXaVSpSueA/71rYtB7ql0BQ+jFXB+HH0+Q3wsfE" +
                "+yhuyQsS+VgxeksiT+cT2cZzjL2VoqlY8ffEoyk1m1SqoNdqMV" +
                "EQk3ldEYjkdf1tGbjtWsUqm6pFSK/pD1NVvWlTXZFv0s+jnhm0" +
                "VmEUoco+eCn6CW6uFos8j+mPzpK55hFjWe4njzbrOotT/zzUll" +
                "6wFAmZp6oxcxM/qLzTCj5xuXRV9gNPujzP5XF/8srSlHRC85+W" +
                "oq679kZrCmY8KjOY67cPEHVU9P4yx6wflehvftOi96Buc1hxjP" +
                "glkc/cN+Cr3x7HY12Bb9DRD+Hv0zX01rrJ2QShi7jY2es5NRg/" +
                "lQ+A1jk1F2pLEYCTEzjW1+miLgdRhE7EM/rGlmtbNSPXrRjnM4" +
                "f6mvzxCft9P7ZnI2rCna/wrzk6NnQU6yp9n3UzXwvOTkq6kMF+" +
                "Z5XcbWp4Yj0nnObBrjRgfJbqeA9nQ6t1NhTVPfy8amHPK8iWB7" +
                "Bue1G1MJa2obc2BNPwPesaDPzuvOkBUz20nR32BdYU3t6Zl3j9" +
                "kTj0sljKfCuyM+DTWYnx7NMXuqH4rfB/Oj0ZZmxKMowuz5Yaof" +
                "A+9J8J5G1nIzPhP0ozL8IxsHM8QxpVKylrOThzJ7J7xPiE8COT" +
                "YeH0+Ip+QcJjs53cnRufVYs6e+Nhzp5sdltuMZNz4R3hPd7OSU" +
                "c8qjVIq7neWMlB92knc0guco0xy7muaEy35+4ZEVT43fm3uvNd" +
                "eixNHVv5b0sMNcW1/CForRc9vBembZhkhkabyIEXE/rOljjFa9" +
                "TFfOj31VT0uclUr1XeFxHCe9sgvmy2yA39bhO2I2drP26hq6Vt" +
                "wT9+bZHaYDZTpmHXXgKxqKLoXxbtIpluO1Dd+pLDdkJMgMI+6H" +
                "a9RBzpTYak1VPS0ptv7PIjPpl9xYooX4+R0hYs7ibs/boblJW7" +
                "amiLfFbEGJo4vYQnp0jdlSX452GUMRrGt7uSaxSqXmaxRdKvUs" +
                "4ezqRr9y3o/A15Jmjcnh0nQerZKRPkuNTJZyrX1HOsfe6vfl86" +
                "CeJGvYO0XBMfa/vNtI/wU2eE94THAyrOnGoLv+7eiG4FTYG00B" +
                "TwK2L6exwQzQqsEHortdxkx4fwjec5w2tzqB0IJy0B/d0xrDdZ" +
                "NLnf2CYEmwtPpmcGFwEVcParyXClYF18TXO+t1Tq6D9/pgQ9AR" +
                "jHbX/dfDS7LI8UG2owlOgfek6F6IjNO9VADnOPsfl30OM4hfSC" +
                "4J5mcZlYznwuCjwcdK6p/9mt7hBcuJXfzbnOVn5NoFa4PPwsqu" +
                "MWtQ4uhWew3p0RazpnkxWyhGz+F8uoZxUlntRySytI6liFKp77" +
                "+MVjtLV87/3qqeloTTOBCu4jjplV0wX2Zj1kgOsqNwv2Rj79de" +
                "XUPXkt2aVWYVShydZxXp4fVmVfNUtlCMnksdZXUZIpGFI2BN32" +
                "S0xkocww0Uz1k+JtfB2MZQeAPHSa+uyNmMIjnIDmBNxTrYV7VX" +
                "19C1ZLd9i/sWo8TRdb2Y9OjrfYubdbTLGIpgXdurt0osxnTH/m" +
                "Wc3Wj4lWlVJb6WNCu/FtZ99j4rH5ksyUD7jsL9Mse+4ffl86Ce" +
                "JGtY2aqposTRrXaV9Ogbptpsol3GUATr2l45SmIxpqv7Lc5ubP" +
                "Ur559Uga8lzcpD4a0+e5+Vj0yWlEO7juBzKnLid/l9+TyoJ8ka" +
                "5E6zEyWOzrOT9OgJs7P5A7TLGIpgXdurr0ksxnSf06s4u/Erv3" +
                "LOTuBrSbPm4+FWn73PykcmS3Jl+450TnyE35fPg3qSrEGuMCtQ" +
                "4ug8K0iPfm9WNGezhWL0XOooa5MQiSwcAWv6MqM1O3TlnJuqpy" +
                "XhtA4Pt3Gc9OqKnM0o0ZPDd8Rs4g9qr66ha8luzWqzGiWOzrOa" +
                "9MoIs7r5DFsoRs+ljrJ+CiKRhSMQk161b+rK+ZqqeloSTmtieB" +
                "fHSa+uyNmMIjm078Ct6Uzt1TV0Ldltuj+N94bj4zsHn6CdVro/" +
                "TS5K96flWUF362iwwLXf7U93w/tMtxOb4WSb/WmwJ5jbuArGu3" +
                "h/KterZynvTytjaH+K/wafFLvBdH+6e7j9aXBn64Ph/Vlktj+F" +
                "2SnwdvvTQdhB4v4085wjOUBf8zP7Ifan8dXhOMHnEtqfCr6/w/" +
                "1pHoP7011mF0oc3WrvIr18jtkVPoh2GUMROIf9qWdHFMKCPdnD" +
                "bK++m7PDA37l/JMq8LWkWflf4UM+e+nnHJ8N5J7TviO4Romc8J" +
                "FiX34lZizw+02/nZBKGLtNP+wgJqNm+uF+/ynT3xppR5IFYmam" +
                "MThPx/Sd7GMdpZ2VjoA1Di2cYaf3fZPRmp2caU92Ge67Po5gTN" +
                "MfLsxZuM9l+BTHgW2azKKKdgpl26mMJTnYiboj6gs+p58D71j2" +
                "gjZDVsxsJ1G8PR2z3bfbE5LZSTnotN2ttema5t+GD0V/TGd2JN" +
                "yDgTWZB7Ez02M/eB9/A5/MT/bxt+V4/rCzAoNrSt+EJ3Oz+73p" +
                "fQ/xt+XNE/B7fkAcSNcUMt2aBlcEV6ae1jo6HyU9la5wIX23nq" +
                "3pc5B3YnAD1rDTgpOA52nEITgj/Z7fTqH7yXRN833kQ9m9cC/Y" +
                "JzIf79jfFCyzY/nYh1g44yXnBtkVKVgZ3Ag2WNMAv8c6Pfuev2" +
                "IqKHF0f+EK6ZXjtIVi9Fzq0kqZjODYH8dozcW6cn4MqXpaEg6s" +
                "6escJ726ImczStIcviNmg93rurKirCW7NbvNbpQ4Os9ufPXO7J" +
                "1JFh1DEaxrO+UQPmeXSikmvZrL/cr5qgp8LTk+/LfP3mflI5MF" +
                "+yp2pHPiLxX78isxY4FfNmWUODpPmfToz9pCMXoudWmlTEZw16" +
                "huRmvu15VzbqqeloQD7I7lOOnVFTmbUaI/Dd8Rs4nv1V5dQ9eS" +
                "3ZoNZgNKHJ1nA76iob4fkUXHUATO4brv2SmH8DkbPqePcHbzAb" +
                "9yvqoCX0uOj07z2Wt/EZks2FexI50TP1jsy6/EjAX+NrMNJY7O" +
                "s430vkfJomMognVtpxzC52z4nFY4u/l7v3LOTuBryfHRJJ+9z8" +
                "pHJgv2VexI58S/KfblV2LGAr9u6ihxdJ46vnrP6j2LLDqGIljX" +
                "dsohfM6Gz+lZnN3q8yvn7AS+lhwfTfHZ+6x8ZLJgX8WOdE5yXr" +
                "EvvxIzFvidphMljs7TSXp0Lll0jJ7LN6NkVTolpruHOYHwTWfr" +
                "Pl1Z5zE3zZNio7k+e+3XPUlL/MXhO+Kc+A7tLfLgWrKi/AWH/y" +
                "sQOPYf4V9xFH+1wXtSf3+qf1Gi93/VnzNC637an/p7w3a/Q5FW" +
                "t6b9mr3+bUh6bzrc71Cwr/YdMZPkFe3116K4p832pwvMApQ4ut" +
                "VeQHpU0RaK0XOpSytlMoL7nJ7IaK3f6sr531vV05JwgN0Cs8CP" +
                "ZL/Pl9mYBcmZw3fEbJLp2qtr6FqyWzNgBlDi6DwDpNvR2kIxeg" +
                "7X/QHGIWtWZUAiOKYBo7X+oCvna6rqaUk4sKbdHCe9uiJnM0oy" +
                "OHxHzKb/Su3VNXQt2a1ZZ9ahxNF51pFeOUZbKEbPpc6W6OOUiT" +
                "g4A8sHNJqsnK+pF6F5Umz0CY6TXsmZ+TEbs474tOuI2VRP0V5d" +
                "Q9di3iCPNEeiTMfMcyTp9sPsJZuM1zZ8S82OI1yukNSKCLK6ZE" +
                "ARmifFRlf7zKRfcmOJlr6tRfayDv7rvwo6GOvXlTXZxrzh/u6x" +
                "8HF5LeGzsLujvh6YJ9ndtZVn9PAXFK+vSOF6yAjEN4o/ldcct6" +
                "YNrhUeyMYNxWsUMwk/V7yGuTWtMpJ/zcFrlEYS16htFB1tl9jh" +
                "frkOUa14/fWvUaU2/8yTw7/Qe+iY9hFkGd5T1KjaW7GiWOi5eW" +
                "gWg90UScjt4jhCduyO/e3a234tEJd5gxxlRqFMx8wzinRr2Es2" +
                "GY8jXKNyO+bLN9loluwuIsjqkgEj6Ojsc3SLz0z6JV+WaEnu1B" +
                "w0H2JR69BeievXYt7Fow1/25MfCx3wfjDfr80L9uOzE/mZT+bn" +
                "T0V2hKPSYz+YK47PCxzOw0luK4tdMh372TObB9uxSY/94IdJjz" +
                "6+ggd6dkQ7/DOPPtLx2C/+q3T17Ciet/DYd5/PX2Tcd0p/cEnW" +
                "77nyDND22L/F3IISR7KhXt5LFh1DETiHz6lnpxzC52xg/CedrS" +
                "tLXhyheVJ89D0fQ/uLyGTBvood6ZzklWJffiVmLPCvM9ehxNF5" +
                "riPdLtQWitFzqUsrZTKCO6KO0Giycs7Ni9A8KbYymuOkV1fkbE" +
                "axHxm+I2ZTu1p7dQ1dS3Zr9pq9KHF0nr34iob6XiCLjqEI1rWd" +
                "cgifs+F+9wWdrSvnq6oiNE+Kj37tY/isfGSyYF/FjnRO7ZpiX3" +
                "4lZsz4fE4IOvVda6Wr9+zesytd9jC9n+DnUf7+Qp9l5O5F/us9" +
                "W+6t+HmUd768kjKD1cX7bNTs4enzKK7Bz6OC2/l+v3jmDOalfd" +
                "HzKLnXw/Npfo1aHyyT51OFcTk9j2pzv7/JbEKJo1vtTfjqXdm7" +
                "kiw6hiJY13bKIXzOhjVdqbN15fwvriI0T4q35/oYPisfmSzYV7" +
                "EjndNzRrEvvxIzFvg7zA6UODrPDtLtUrLoGIpgXdsph/A5G/76" +
                "J+psXTlnpyI0T4q3c30Mn5WPTBa7pH1HOqe2qdiXX4kZC/xe04" +
                "sSR+fpxVfv9N7p2kIxei51aaVMRnCf0+kaTVbOuXkRmifF2nkc" +
                "J726Imczis+hXQfwOZ2mvbqGriW7jZYWn27zPi9apm3+vVq00j" +
                "0570CtZ3t2bxpm3gv970PdX39Ju11du/PSoe9Ne7bb+e2+1eQz" +
                "+fD3pj3b/Sp+Zcf02fb3psVvTzU7c4e5AyWOZEO9Mt7cURmPdh" +
                "lDETiH/alnRxTG52xgep7O1pUlL47QPHGW3Juy0xjSzzk+G8j9" +
                "cvuOdE7ttWJffiVmLPBDE6LE0XlC0u15JrTno13GUATOYU3d2L" +
                "OVrRKLMR3TAZ2tK+fsVITmSdXsx3wM6eccnw0ybdeRzqle7vfr" +
                "86CeJGuQV5grUOLoPFeQXhmlLRSj56QnX9VWymQEd8x+R6PJyj" +
                "k3L0LzpNiUXRHLr8jZjAKf02E7Yjb1ydqra+hasltzs7kZJY7O" +
                "czPp9hNk0TEUwbq2Uw7hczbc776os3XlfFVVhOZJ8Xaxj+Gz8p" +
                "HJknJo15HOqa8r9uVXYsYCf76ZjxJH55lPet9BbaEYPZe6tFIm" +
                "I7j7qIMaTVbOuXkRmifF2k0cJ726Imczis+hXQewphu0V9fQtW" +
                "S3ZqPZiBJH59lIet/zZNExFMG6tlMO4XM2YspsXTlfVRWheVK8" +
                "PcLH8Fn5yGTBvood6Zz6PcW+/ErMmPH9p7n6ObBd9s4/i04e9Z" +
                "/9vt1n0Xbf230Wnfxfz6LrTw93z/0Wz6K3m+0ocXSrvZ10eylZ" +
                "dAxFsK7tlEP4nA3XqAM6W1fO/+IqQvOkePt9H8Nn5SOTJfll+4" +
                "50Tv2lYl9+JWYs8NeatShxdJ61pNvl2kIxei51aaVMRnDXqK9o" +
                "NFk55+ZFaJ4Ua4c4Tnp1Rc5mFJ9Duw5gTV/WXl1D15LdmtvMbS" +
                "hxdJ7bSLcDZNExFMG6tlMO4XM2HPv/1tm6cr6qKkLzpPjwfh/D" +
                "Z+UjkyV5o31HOqf+arEvvxIzZvxD/7an/PF3/nza89137Hx62d" +
                "s9n2Jfb3U+bcx7m7/tucnchBJHt9o3kV45giw6hiJY13bKIXzO" +
                "hvuoy3W2rpz/xVWE5knxKTuN4bPykcmCfRU70jnJK8W+/ErMWO" +
                "CPMCNQpmPmGUG6XcFessl4bcO31giXK9TqRQRZXTJgBB2d/R+G" +
                "+3xm0i+5sUSL/WSRvayTfU5Xaa/E9Wsxb5CbzWaUODrPZtLL55" +
                "NFx1AE69pOOXYcazSr/kZn68r5qqoIzZPi40d9DJ+Vj0wW7KvY" +
                "kc5pbDSb7Vi/rq7EjBm/9D9VvY8M");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value1 = null;

    protected static void value1Init()
    {
        try
        {
            final int rows = 57;
            final int cols = 85;
            final int compressedBytes = 3785;
            final int uncompressedBytes = 19381;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNqdXG2MXGUVXj8KrU0TaahCUjRINJGA4AcQSpW3szMTlu3+gm" +
                "QJVVPAtrSUyo/SWqtJZ/fOzszugin90GqbYk3ZxOAPiUCxCK0i" +
                "iWhA+VI0mAj8MRHFBEUjMb73PfPcc55z72ylu5nzvuec5zznOW" +
                "93Znbv3m1YEBYMDYUFIa3yIV7uNx7SLGIWzzF5sAde7dB9sMxg" +
                "u1sFysBoQbXf8Mps3mpTKxE7V3kiqJh+hLOW1/dS3dFuC9vEyp" +
                "oy2+A3z0KEMUCoz3HUgF+rh4ZW/ZyruXNxqoRgncBP3eE5vCrP" +
                "jIjMVZ6Ia6b/XJ7Ld1LFyj95JN83l+efyOQ78bLNHENGEbrKfm" +
                "J7rHqdo9whPONzngWd0GfizjJf/jGxRlmhT5U1l+86n5XrBNlt" +
                "vovvnM70b9Xz2s8q9WnKue7nh4Z2vRDmxAMizE1uz20en/xKfK" +
                "7tby4Pc62LuLp9t9TlH5PX52eao8BQoGYRCXPtvYhOfcGqiKfw" +
                "+zyvVa0t6ezubC5vf4sV5xoj0102ZhG7fhcf5yfMd2y+vc/sD8" +
                "i+3VOO9j3tPfBmLuyuiZFv9rWskbr2dKr9Rnt37PCS7VtMfUu4" +
                "RaysiIlf+2y4ZepXGgGG99a3UeVXRNw9x2y2c6HOIVgnsJ2rFW" +
                "ez3FGrlSWfq2qiiZNWzcxnOMs9uBed5kgYEStryozAr10VRrID" +
                "GgGG99kS9TXa75IinduAiO9RP1G2zkbuXJwp9WMLnsi6SXE2a6" +
                "dQvaomjORzVU0Uz9SoqV3AWe7Bvey04VA4JFbWlDkEP9uCCGOA" +
                "UJ/jqAG/Vsfd01zNnYtTJQTrBL7T8hxelWdGJLu9eiKumbmsPJ" +
                "fvpIoN/1SYEitrykzBr30OEcYAoT7HUQN+rY67X3M1dy7UEYJ1" +
                "At/peA6vyjMjInOVJ+KamcvLc/lOqtjwHw1HxcqaMkfhZ1vD0f" +
                "R66jBAqM9xYQGXcqZn1OtczZ0LdYRgncBPPe05vCrPjEiuoWqi" +
                "qadszcyW8ly+kyo2/BvCBrGypswG+LWDHAGG99a3UVQqQ9o9y2" +
                "y2c6HNIVgnsJ2u4myWO2q1suRzDZpI1cwc5iz34F522rAj7BAr" +
                "a8rsgJ9tQ4QxQKjPcdSAX6vj7jdczZ2LUyUE6wS+M+s5vCrPjE" +
                "h2Z/VEXDPz/fJcvpMqNvyt0BIra8q04Gc7Qqv2psQtBgj1OS4s" +
                "4FLO9Nx/k6u5c6GOEKwTeGGyHF6VZ0Ykr6yaSBnTmT5Znst3Us" +
                "WGf1fYJVbWlNkFP9uJCGOAUJ/jqAG/Vsfdb7maOxfqCME6gZ94" +
                "3HN4VZ4Zkeyr1RNxzcyL5bl8J1Vs+BeGhWLztZ9ZCL82olnELJ" +
                "5j8mAPvNohvFBmsN2tAmVgdP/19G6vzOatNrUSsXOVJ4KK2Y9w" +
                "1vL6Xqo72tVhtVhZU2Y1/OxujgDDe+vbKCqVIe2eZzbbuThTh2" +
                "CdwHbeVpzNcketVpbsrsETqZrZcc5yD+5lpw3rw3qxsqbMevi1" +
                "GzkCDO+tb6OoVIa0e5HZbOfiTB2CdQLbfZ/ibJY7arWy5HMNmk" +
                "jVzN7KWe7Bvey0YX/YL1bWlNkPv3YDIowBQn2Oowb8Wh1P4jGu" +
                "5s7FqRKCdQLfXew5vCrPjIjMVZ6Ia6rm8p1UseHvhq5YWVOmC7" +
                "92LHTTz/sOA4T6HK8ds1zKmd73j3E1dy7UEYJ1Ap9923N4VZ4Z" +
                "kVxD1UTx531TU57L68BMVnW0+8I+sbKmzD74teOIMAYI9TleO2" +
                "65lDOd6XGu5s6FOkKwTuC7Z3kOr8ozIyJzlSfimvJcXgdmsqqj" +
                "vTfcK1bWlLkXfnYfIowBQn2OZ3OWSznTc/9xrubOhTpCsE7gu0" +
                "s9h1flmRHJjlZPxDXlubwOzGRVDw3Vr6tfJ1bWPCNevs9+gAhj" +
                "gFCf46gBv1bHkzjB1dwZ6hjBOoGfvMJzeFWeGZHs/uqJuKZqLt" +
                "9JFSt/mA7TYmVNpz0Nv3Y9IowBQvbZEh9HDfi1Op7pL7maOxf/" +
                "4oRgncA3P+o5OF9mRkTmKk/ENchUq7GzWtXR1kNdrKwpU4e/6p" +
                "VQT+9RdY15PPs2Ci7lTK+n5ypbZyN3LrRRP7bgSdek6x6pea9X" +
                "1YR6PlfVRPE9qs7qbZZ7cC87bVgX1omVNWXWwW+ezZGwTiIWz7" +
                "5Y1CGS+7KLZ3ohs9nOxZmuUw3eKg+qLVLzrFFj0GfrgJa4quEs" +
                "9+BedtowGkbFypoyo/BXvcaRMJo94PHsi80eljpEsoeAiM/9tw" +
                "3bj7hzcaajqsFb8ETWBxVns1az6lM1YTSfy9YBnf2Q1XCWe3Av" +
                "O23YHDaLlTVlNsPPHuFI2Dwx7vHs2ygqlSH9fv/HyuY7F2fqEK" +
                "wT2IkbFGez3FGrlSU7xnVVEwjeZrkH97LThp1hp1hZU2Yn/FV/" +
                "QQTReKY7FYEafSBquZQznelDXM2di1MlBOsEPp6p4/CqPDMiMp" +
                "fH+pqquXwnVWz4Z8OsWFlTZhZ+dhwRxgChPscb11ou5cx3jWu5" +
                "mjsX6gjBOoHvft1zeFWeGZFcQ9VEXANUtRqNmTOV7EyYEStrys" +
                "zAb54ZZtLvoxwGCPU53lhhuZQzKV3B1dy5UEcI1gl8Z6Xn8Ko8" +
                "MyK5hqqJ4vu+qQGqWo3GzJlKdlFYJDZf+5lF8LPHNCuf8blv8L" +
                "rqgz3waodVx8sMtrtVoAyMLl5PF3mk5q02tRLJ7i+rt31Uh81a" +
                "Xt9LdUc7GSbFypoyk/CzE4gwBgj1OZ5NWy7lTN9VLuNq7lxMQw" +
                "jWCXxviefwqjwzIp2zqyfimmzGz+V1YCarOtqbw81iZU2Zm+Fn" +
                "JzkCDO+tL7YxInWIKCI+o0aYzXYutDkE6wS2d5bibJY7ajXrGz" +
                "SRqpEpuK/taHvZacPasFasrCmzFn72U44Aw3vri23UpA4RRUSl" +
                "NWaznYszdQjWCWzvYsXZLHfUatY3aCJVI1NwX9vR9rLT4u5Bvl" +
                "cSfq2mWfmcPIfvSgzvKt9PyBG9S1F2qx5lhubyXdfZ7nw/olVo" +
                "laW7CM/luxb5Dsb8XknWpCx2Ln+fJd85abN2Et/L3mnZ/XJ3S/" +
                "eO7u1DQ62zy/eC1hrFXYsf699fOp7fK9n6xFDlR/s1uVcyIkJV" +
                "Ps+sergq0/oieV9qmXso8vtPqz4mbojID7T+W1SdFx8XFN7H5f" +
                "7Tqo9aY/J4wgxX3ddqdIyb/Zpid1N/Xd/qf3faup2eZUvDUrGy" +
                "IiZ+9vewdOJnErcY3tuHslj+iSdREZ+xF4E/cj/BnblOtbFOYC" +
                "cPePWc55lsJHujeqL4vRSp4WxZh/ayHU/x3K83l+fXpQY/U9Jd" +
                "x0vKz32+77h3lXboXaIMvSvluT/ovur5n/u9Faze3708z3O/Xj" +
                "3RxMlBz/1qHVWvWf6ubD9P9o/qO7t5X349bVxTvoscu8Y1/jXw" +
                "dM80++fpnmmu4dSvp0D5d4ZTnWnYHXaLlTV9Be+Gn/0LEcYAoT" +
                "7HG6OWSzmT0lGu5s7Fs4gQrBP47N+ew6vyzIjkGqom4hqgqtVo" +
                "zDz3Jbslfs4lm9Z0x/0W+LWNKaoRYNI+X9N+Tn2N9O/eT5HepQ" +
                "WC2CwT/pqgCmGtqFB2q0yrCqzqnVOWNFfFRNArDDpJVQ+rhacN" +
                "8UOsrCkX4Gdvh9C7ViPA8N76Ngou5Uyvgp9Utt4Idy7+vakfW/" +
                "BEdWeE4JGa93pVTQjZf6oniu9RgdXbLPfgXnbaMBbGxMqaMmPw" +
                "2+8JY701GgGG99kS9TXa7zJmOdOZjilb70buXJwp9WMLnnSmYx" +
                "6pea9X1YSx9rurJ4pnOsbqbZZ7cC87bVgcFovN135mMfz2As0i" +
                "ZvEckwd74NUOvbVlBtvdKlAGRvff9zd4ZTZvtamVSPu9ZfW2j+" +
                "qwWcvre6nuaLeGrWJlTZmt8Bvf5QgwvLe+jaJSGdJJfI3ZbOdi" +
                "FodgncD2blWczXJHrVaWfK5BE7F6m+Ue3MtOG5aFZWJlTZll8J" +
                "vnIMIY3tuH2EbxW1CJaEX8d6qDv9zZnCohWCewuTrm4DzPZCO5" +
                "hkETaQ1Q1Wq4F3XcG/aKlTVl9sJvX4IIY4BQn+OoAb9WDw3VX+" +
                "Bq7lyoIwTrBL59qefwqjwzIrmGqom4pmou30kVG/6NYaNYWVNm" +
                "I/zmEo4Aw3vr2ygqlSH9rP1BZrOdC20OwTqB7W1TnM1yR61Wln" +
                "yuQROxepvlHtyLpy1+ani1uEbwi2L3VHx3PaDZdHXkIostXdfo" +
                "/w3vYETnQ92Xse9dmX5uf8LjG69aP7+G0v1Tmam3ovtHi8x31h" +
                "t8DaVzHjDZXyP3K0Wnk4xTtuZyvYaCqPZm9eFIOCJWVsTEb386" +
                "HOn9QeIWA4T6HBcW5dfqeBIHuZo7W12KYJ3Ax++lHIdX5ZkRaX" +
                "+qeqL4vZSpqZrLd1LFdlr+6/L01flM8XfOB+MZvFx9TWHw1+n8" +
                "H/VnzVfajeXufL0xXwdd68vO8NdrmGuer9MPV/+NuP86nf9KoL" +
                "mSy38heTAcFCsrYuI3HggH089RDgOE+hwXFuXX6niOh7iaO1td" +
                "imCdwMevU8fhVXlmRPK5qiaKX6empmou30kVG/7tYbtYWVNmO/" +
                "z2ZYgwBgjZx5+jXBw14NfqeKaPcjV3LtQRgnUC33vLc3C+zFwo" +
                "vr96Iq5BplqNndWqPtW1vvZV/8+1Pnv91D+Py9f6es+Xf3dyet" +
                "f6ps887eunu9/p707e0bW+w+GwWFnTaR+G314ZDqd7JhwGCPU5" +
                "LizgUs50pi9xNXcu/sUJwTqB76z0HF6VZ0akdk/1RPG5b2qq5v" +
                "KdVLHh3xQ2iZU1ZTbJ5/DVw1dzJI95PPtiUZevliGPSMxWaOdC" +
                "m0OwTmCnFyrOZrmjVrM+W1eeADrsvNyDe6nu/m/m7ouPI+Z9P5" +
                "N1eHx4nN8BW88Np9/xneI3YyuHxwUxPO4yz2YX+9h8/8dM+3vz" +
                "9Zm+wr9mWR78HzNVH9AQv+tszv/Ormr1+1P+LWlZfdgT9oiVFb" +
                "H8c/Kt4ZvCntoJiUssu1z3WqOPPuoK8fN6y5nmuUmrm+/3na0u" +
                "1cY6ga+d8Oq9Ks+MSK6Bp5a9MkKFRZZ1YCY+zaH/AQ8RI5I=");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value1 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value1[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value2 = null;

    protected static void value2Init()
    {
        try
        {
            final int rows = 57;
            final int cols = 85;
            final int compressedBytes = 4124;
            final int uncompressedBytes = 19381;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNqNW2uQVMUVnqpYJhULEYkuyAJB80CNVmmMiYKkd+/MFEYFXB" +
                "bBgKD4wiSiKPEBxtDD3J11BhT9oWWlLMVawB/5FWPKpKJkNZpK" +
                "VR4/NA+rBB+RhxrQ+AASU6l097nnnsftO+tO9eOc/s53vu7tuf" +
                "duczETzIRKxUwwoYUfsMyEZGmy1ExoD6ENPo6nloqvEeVb9GEv" +
                "xsCzcwWI4DVhm0dzZZqLa6OadGn1PA/p4EjOq3ORblenJoUa2j" +
                "CSwie5ILnApO0HwI8+6lMMFUSB7dGc03vAB5/2/TpzPhvGL2vC" +
                "uzVV6rUqzYwePguuXMZoZFEHzo6rrlTqvfVesGuv4Qj4fKk+CK" +
                "P13tpr9d6hOd6yX/NY9FcqQ9+GFgvUwNachPxDs5G9+iDmgIh6" +
                "74aFPDto8RlQWcjSh2jyt38ukb6Hyry1YQb2URlpoAias8TAeL" +
                "MH+3ZpNpNZqBVy87UDDrPVbIUa2rDaW9GuPmS2tv8Bfo5BBNnS" +
                "DyzIRZxhPg/JaJk5/40LhNSJeLdPFYdWpZnR4zXEZiRjYvPSmU" +
                "gx8den1qdCjR+yKpXG2/Wp7T3x0W4+9BRHGu/07SSrfb+OyX7f" +
                "U7t/PMKvaXcVjf2IRGbE9W/RXFID6eCjZbPneN+zx9nxrp5gj7" +
                "dfyHd5jz3JTrHTkznJHDvD2efZk+1X7FftTHua87jvvj3TnmXP" +
                "du3XXTnHlXNd+ZYr54f42Q5lXNvn2n5bC77v2IvtRe566jkHnL" +
                "3QDtpFrr3MLnH1bXY5/33ba+z19vv2B3a1699kb3b1Gntrvdfe" +
                "bu+w4+yxgOp81p5gT7ST7GTba6c5zFT7RXuK/ZL9cuA4dcMMe7" +
                "przwjWN0I9y7orvfUa3BXLVm1i666d68qF9hI737UL7KVMx2KH" +
                "vNy137XL7FJ7RfCtsFfZlfZqe629zt5gvxd8N2b4W+xat1u3mC" +
                "1QQxt28Ba00wvRIzGIIFv6MQb5KbpS6fuNjJaZ82+RQEidiO98" +
                "TnNoVZoZPf33xmckY2Lz0plIMeNfZVZBDW0YWQWfxCTGrEovIo" +
                "/3aby0oQaU25UGPIQATvzozLk2hZA6EZteTDg+KjNStNTH4xDd" +
                "GJVq5HxlDpmLdLt6hVkBNbRhZAXa6TzpQYzsc5t7MZIYwj4dlW" +
                "w8c76mCiF1IrZzOuH4qMxI0cTSuqB8RlI9H5U5ZC4+WzPXzIUa" +
                "2jAyF+10lfQgRva5zb0YSQxhTZ+RbDxzvqYKIXUitnMW4fiozE" +
                "jRxNL8afmMpHo+KnPIXHy2ZrPZDDW0YWQz2vVj0CMxiCBb+usT" +
                "ORdxhnvjRBktM+erKhBSJ+I7Z2sOrUozo8driM1IxhTnpXXgnL" +
                "hqV/eZPqihDSN9aKdrpAcxss9t7sVIYvC91nLJxjPn2hRC6kRs" +
                "Yyfh+KjMSNHE0rqifEZSPR+VOWQuPtvm/krlnv/c829nL4LitB" +
                "6Hc0tvcdbzAb0Ia/8sBZaLfrcS+WkeYIh/BsYXabR/F/Ubv22+" +
                "5zDvED7Ttojbzbedxk90lv5djRckkmZQ5NCxzX2AaR7k/sYoMf" +
                "HW9+xSnYMylGUyi6E45vxJNXVPWxuHcBTqsKaLK2P8lCP6nh/e" +
                "RWsax5vFOn74jSLTxtbwbo6kGcQ56Ce9lWcdfrOwprAOPZwtW1" +
                "OWgzLITMM3Da8evnn4xsaAcc/evjiueXkO9/zbvjpELcE6rOkS" +
                "sS/HBezCLOYSwkf3CPvLvpNnknizpDyeeJpHA7IxnzgorhsHat" +
                "CIfE2Dv3U1Z8vWlOWgDMVM8N0Pf/0PBuZleW73d0zzYTqD8ON+" +
                "TYunDfpHIjqXsX36HPMPxvHF84zid99FL5JIOsUBywyWrunqWF" +
                "a2poN63AxmazpIXnnKE/up7cXRRo7oX1ccDedSe8da03JE/7rh" +
                "XWPha3t1fOy775h2c6TvcWvDjHINPCt992m+RVWwptxLuaVa/O" +
                "67/TMSdlEHe+G687N8d2WjjcV+TTNrpHxNy8fiI9LbN9KNO99V" +
                "SyTS97jl59JdQ/csUgWtKUXHefz+9d/9RkpnKHR+mq6v9/rvPj" +
                "8fhTMUPM2U3zo6P5VcnauY0mfpvLKzAs9P4QyFIu31kv2eT4rn" +
                "p50rHe5E0mGn1nvtKaSh/Pw0XZdFJPL8FL/7dIbC11R/w+11dn" +
                "3smlWdVp2W7d/8GwA+X/rXwmh1Wu1N9AEWLLRxDNHERpj8yvgu" +
                "+pGD8MTvM3B25Ed8WNM1Eul7oAwVU26pAueFaNKhvvtvipNstV" +
                "KQm3uAw1xqsrMtk59xgc+X/tvAizbH6FgqFMMx+XxuQz+iJatU" +
                "wZkQjZnSu2NIiY4x07zID7zpj7o+H15a9Ehfpm3ADGT2QD4SfL" +
                "6kj4IXbY7RsVQohmPyfXoI/YiWrFIFZ0I0ZurcHkNKdIzZPcs8" +
                "rWctdZSs6UDRI32ZtgVmQWYvyEeCz5f6SeBFm2N0LBWK4Zj8mn" +
                "MS+hEtWaUKzoRo6LWuBaaYDq1PMrsnz2v0rKWOkjVdUPRIH9jV" +
                "3mp2laV7FPh8SXfAaLV343j0BeyZYKGNY4h2CMO5xPfudfQjR8" +
                "Avx0g855fsyI94z5M+4e9R9k702KlVd49CtD2V55Yq+l/PIhLK" +
                "UVQq71FM3VV0jwLfxmPFyk2uToYa2jAyGe30qepkf98nTPIE9S" +
                "mGCqLA9mjO6T3gg09nUGfO9TN+WRO+s0ir16o0M3r4LLjyxiiP" +
                "8Sg5X60DZ8dVu9063+BfdvPzHRx8vqS/MvP9mqLNMTqWCnixpp" +
                "jse7ca/WZ+ZwW0nFWq4EwYhZn8s1QRKdExZqfhRj1r4NXPUuqb" +
                "Pr/okT6wq9Or07Mngz35agdfKFdUp/u/96vTa3vA1zwXsGBhbI" +
                "6fDlbzm8CWPEIY/EkewRzV6Z2/Q4vZSYvPwNmRH/HwHopE+h4o" +
                "Q8WUW6rwumgepEeuafJIbQ+olRy4UpCbeyCjmWeycwxD5xnB50" +
                "v/cjMv7NN5VAiNsc1xfBy9WPu2s5uUtSeh38zrvAotZ5UqOBNG" +
                "YabOrhhSomPMMC/pB94x9um8okf6wK5OqU6BGtqw2lPQTp+tTg" +
                "nXU4VBBNnSDyzI5Wb/NvnNORTd2acz53uC8cua8J39Wr1WpZnR" +
                "kz4Tn5G7nrKY2Lx0JlJM/LGTpEb+b+39K+Wofa37OUzZuRT/aS" +
                "8dGx97zyZyErKy+7lUlzOUlZ9uHuxv5WVFb9m5lJloJkINLfrA" +
                "7r8SPRIj+7wQC+enCLema5G/mFnGEULqRGy6U3PIcTkn7oF5xW" +
                "fE1cjRog7KJTKON+Oh9m02Mh7t/mvM+ObDaCOW8NIHRVrISxmS" +
                "VynSXU8ZM9Mm8smasJ1dWhkf59qoBo+fl1bvP41RqUSOcl6di3" +
                "S7Pb2x/N0etxOeszP89dSe7Ep4twfPpbq/28OemtW7PW6f3kHv" +
                "9nT2wbs9+MzP4iLv9rhavtuz37/b47zRd3v8uVTZuz3paPzdns" +
                "aofrfHlcK7Pa6od3tyfHi3J3YFo+tS+jt9lUu2y3M9fbKXPYVs" +
                "ly29i5hs9wzF65j20Ild8fwQfzat1aeNdEoJayqZ6BTT66JTw+" +
                "LZJKovmy+eOcavx2aWmQW1b8kHdvp7GoVP8hjHU0vF14jyLfqw" +
                "F2Pg2bkCREidiN30Q65Mc3FtVJMurZ7nydb0MYnkvDoXX818Fn" +
                "PMHOpDcWv6RzHTOThCCN/Cv/FhZMmz3RzM0P4Jz1UWCTbliTGn" +
                "fyImQJJCyUE1jKd/4Io4t9QWny/GxuYh5jDbzKY+FLfHT8B/Ny" +
                "UML4jmkeJMbRKPhdFkHeXatLksEmzKo8ez9/lncxWYQyqTNYwn" +
                "67gi4nb3KLYOrVvM7GZPbL4Qy7GxH3GPcjO14dk1/TNfU3ePui" +
                "95HO5Rrpydec9hLOdn35rHZUv3qOYZjmHA3put6QP5eOEeFepw" +
                "jwq9NW4m/9VrCveoMJ7fo1yb3aMAH33/1OmyW/w9Knjn0prye5" +
                "RTutiVcI+if4+S96hP/XzbzHPcoDHJtvJnZXZ13yZb9jf0tqKv" +
                "/B7V/bl800PFOyS/65Q98+MsxnrmT7YRMqa3218mSbgemo+hTc" +
                "bV/oX+PrePmucldL38GLHFeO5FT2yE2Nx34E7N7Md9jyJkLs7b" +
                "PF8y+z7YVFM2Pou+B4ib69F/76OqGIcvZRly1Ic585rc92QcQ9" +
                "g4Q7eR9qPM/2RZLNnlrJtG5Kjvg83rmBrMW84d1mFt9xk5nq7x" +
                "/ceaw9Qv9jKmw7Ll7/Ygg47R+PZjUY2Hy2w9wtZ0G4xiRt8HNK" +
                "9juXjEp1uTmF5zuHu8Q3yU/37W5/8Wf18cQ9g4Q7eR9k72NPRy" +
                "WSzZ5aybtstR3web1zE1OK9y7rAOd3WfkflojPgBcyTv0ztoG/" +
                "h937Ecka1gWAheeAdNKWD4+lFsVZ6OIaQdy0Vn0n40fwftCKJ5" +
                "HVODGjRCXk8bjfLckKt8PEMdyrnyd2LSv6o1PaSxcYZuI+0X2J" +
                "r+uiyW7HLWsKaHZAzYvI6pSf8S51Zrel/3GZlD8fHkg+QDqKFF" +
                "H9jpK8kHrdvBn3l3sH4eQwVRmb0DMCzDDvBRtMzMdRFC6kR86w" +
                "7NoVVp5hy3Q+eGfmNUxOzQ89U6cHZyNePnGfn7UvvkWYZ7Xhuh" +
                "57PGU7H3pcJJywj0kxHNn4wAg/7/prEnQHlKI9+X8mXT6zEk5S" +
                "t/X8rrKp4v65XwSpORbufsjV/EztKTA8kBqKENXAfQTvejR2IQ" +
                "Qbb0YwzyU7TbXXfJaJk5n49ASJ1SKefQqjQzelrr4zOSMbF56U" +
                "ykmPjZ/4t+K7JP36n3tu4O/y/6rWyf9ABWnpLpfVrvAbZ6j/7t" +
                "O09PbJ9SduD3Gbrt0/Db+bFE+h4q81b5PvW6+Dzi7/X5WdR7iv" +
                "sUtUJu7sn26cHkINTQhtU+iHb6HnokBhFkSz/GID9Fu/wTZLTM" +
                "nP/GBULqlEo5h1almdHjNcRmJGNi89KZEvF/LMBm+3RvZJ9+mP" +
                "///b38WgVW+T7lCH0Vqh0f3aeF96THvp5qpO9hXnhPumyf1o6n" +
                "iPLrqValVwpyc0+2T99P3oca2rDa76OdHkGPxCCCbOnHGOSnaP" +
                "eN/aWMlpnz37hASJ1SKefQqjQzelpPx2ckY2Lz0plIMeM/kmTP" +
                "rbX8jXnw+ZL+D0aTI7U30AdY9GMEL74mRKKei1svox/RgBf74w" +
                "0fz9mJm9RppO9hXrAot1TReokiaM5J4QleqtIrBbm5h3OYy/2H" +
                "LCiVytBnpA9HCIEtr2VPZ6h9Xo8V8YilPLEYzor6eIQcIZ+5vP" +
                "Z5jdCZ5UrI+fJPVP1p5jSofUs+sIeOolH0cbz0QeFWcxLyUobW" +
                "34oMPDtXgAips6g0xsW1UQ2e6oNF9TwP8Td7dF6ek3xiNWeamV" +
                "D7NhuZifbQMTSKPo6XPijSQl7K0H6xyMCzcwXEINFFXJGLa6Ma" +
                "PK1Xiup5HuLno5xX5yI9eewy/yELilvT46QPRwiBLa9lT2do7d" +
                "ZjRTxiKU8shrOiPh4hR8hnlrVe0gidWa6EnC//RGf7f9jZjWo=");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value2 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value2[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value3 = null;

    protected static void value3Init()
    {
        try
        {
            final int rows = 57;
            final int cols = 85;
            final int compressedBytes = 3159;
            final int uncompressedBytes = 19381;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrNXGuMXVUVHox2Oj9aWqK1DLe2Q4ug9JGKUFLUOefeO1Pait" +
                "WiFMFiQKUKqPhKSfwz+8703htmlET9gxVMTMigRjFqa9P0R4mU" +
                "RGPEQItAhT4sRGP0hz98xErd+6yzznrsfR5zO209N/u11rfW+t" +
                "buOXufc+fc9vW5I7rdffrSw/Vg1K5JGWoIgS2vZa9PRej8Wet8" +
                "PGIpTsiGe0V+3EJqSBbd3vmTRujIciZkvvwTzjaRrXIfGkGxc7" +
                "pMylBDCGx5LXs6QvNhrfPxiKU4IRvuFflxC6khWbSq+bBG6Mhy" +
                "JmS+/BPONpGtdB8aQbFzOiRlqCEEtryWPR2huVvrfDxiKU7Ihn" +
                "tFftxCakgWrWzu1ggdWc6EzJd/wtkmsu3uQyModk6vlDLUEAJb" +
                "XsuejtA5o3U+HrEUJ2TDvSI/biE1JIu2d17XCB1ZzoTMl3/C2b" +
                "pjtOY+NIJi5/QqKUMNIbDltezpCPUxrfPxiKU4IRvuFflxi7Eh" +
                "nx/o62M6io4sZ0Lmyz/hbJN5vs19aATFzulaKUMNIVw7MQ9H0k" +
                "c4Qr1f63w8YilOyIZ7RX7cQmpIFt1W79cIHVnOhMyXf8LZJrLV" +
                "7kMjKH19cUPKotXtUdQiHlpey17yb7ORIjz4K9K1N4TxiKU4Up" +
                "9a30hekR+3kBqSRashL45IPTb5PFB8mS//hNjDYRaaS8ybs9HX" +
                "bLksOacHMtnbbbnKfH90wNj12Fgf5l2p5t1J/YOkXp/hI3GNDp" +
                "hN5v1mczKnvzZbSZ7h7xB8Pml2JO3nbPl80vtC4MofMG8xi8zj" +
                "if5ttiwxy8xy216RSN4B175J9mRzbVLfYN5r6/dBXNM0ydyaG5" +
                "nHD5oPMR7bbPmoLfY8NOn9kvm4udPcZT5hPmXuNp9JZZ/1Vy69" +
                "nuJ4fCeuOOMP8NVK9vWaE1qf2Ho6ID3Yde/mvPWXr+JkJZlKX1" +
                "w/NqQ5ZevpgJ+FjBNaT/XKLblIPv7qns3pd8I7wFnN6dz/gzmd" +
                "Wzyn4T0qf07HH+F8osFoEGpok9VhEMftzaTlGOqH7XnhPpNr/4" +
                "hvibpsHRPxZM2xmhnXa74oc3V7k2ato6F/rtVzwWMRH1vXohrU" +
                "0CaaGo7j9aTlGOqH7XnhPpM5/Y9vibpsrkQ8WXOsZsb1mi/KXM" +
                "3z8jPCPGQWOoaMRXx4HvFjeIW4Hhy7TnOt07e20YhwdLR2cnzo" +
                "CGukNH6Mj1tfCXtq3SqRrufKrv/CaGyojEM+S59FyDrsx4ybi2" +
                "nfNz9LZG81g+Yys9Re+zcZ2Dsvt+VKu+//3LbJvm/Wun3fXAP7" +
                "vrnOlutx3zfvYf7rZiRps31/8iK375ubzYfNR2x7i7nV3/fteI" +
                "e519wH+7653+375ku23mkeMPPM/Axl931bX2pqtO+bFbDvu/XU" +
                "XB3e9+Pfm2HY980o7Ptmo7nJbLEt2/ctSuz7Zjvs+7Zk+765R7" +
                "D+ovmyPVvnR/OhjjKmMHLj9lbSoozjpQyKHKFfihA/7Xvg0TkD" +
                "8iDRPs73xblRDZL4mM+ex0nn9JjUcr86FvEpfTa9t+qzqe7nP5" +
                "tO1s7/sym/+3B1fKTKs2l8pLdn02httBbqaC2Xwbj5F9KijOOl" +
                "DIocoV+KUL/a98CjcwbkQaLBj8+M6zk3qkHC8/Iz4jy4lvvVsc" +
                "RsronWQO3aVLMGx+2HSIsyjpcyKHKEfilCfaXvgUfnDMiDRIMf" +
                "nxnXc25Ug6T9dZ89j0M8uJb71bGId9mzafOv6tl0L+5R+tk07e" +
                "c8m4o9KqZnU6bv4dkU9qgqz6bmF3KPgrz8Z1O5R/X6bErHyGv+" +
                "2tb8m9SihrDhVa4IMTmaF53ijrwm7f3VlJgQ0vVcGb8HRvn3Up" +
                "hXcR55epRS7DI/fX2Np9N2XWOdvf6/4Wuq2edo183cztdUYVLg" +
                "b13frBzjP6n6b9J4Kvvu6zlbDpMWNfLfxX1/Khg/VXie3p0Xne" +
                "Lq85Q0XDKxJec8PQgj3yrb0Z+pcn7Fz/R2njb2NPZADS3KYNx+" +
                "pbGnfQzkHIMIGks5eCH/XDP5aWktvXJeHCH70nsYSVrNprEnfj" +
                "mckbRBlGbDIxFj5n9/Y3807Wpo7d6Vjty4/Q9Xg5xjEAF9lJE8" +
                "moYx+LLzuIM05J8s0AvowVoiZJ8z5cw4knJLst2PXl0dvxDOiD" +
                "wmc/qCzldGpFzTXT/VsjP6eDbbh7Jr/19SixrCBlerQ0WIyfuD" +
                "19NxHnfkuLQnjZZwpOvxkW+VXdXPVskjfjasRynFVmz3NvZCDS" +
                "3KYNz+N0okBhHQn5in5WiD/rlmclxaS6+cF0fIvvQeRpJWs2ns" +
                "jf8QzkjaIEqz4ZGIMc82uxfL7k8bB7HX0WdHqjGrC8/Tg0XayU" +
                "7wTvMOaQf3p/keMyaLmM0SW5ZX4RG/mFo0Cs/TF822Ir29P/1q" +
                "1TsEuhvsXFT0VN55sMw+OKffLo87O0fBd32/q2JfjupMBs+pfY" +
                "19UEOLMhh33oASiUEEjaUcbdA/10z+WFpLr5wXR8i+9B5Gklaz" +
                "aeyLXwpnJG0QpdnwSMSYZ5s9W2zwz5f6o72cSSXn6RP5dsShzG" +
                "MZE+cp/zzVefV6hPk268069qTMlc5ikOKYY7iN1BOK+8/m9Jco" +
                "BwuOCFtyDhIfQkp0yLM9A5/XWYf8xs/rOPmzJ2ZuuDkMNbSJZh" +
                "jH9QPN4c71IOcYRNBYyusHuC/ymZwjB6S1jJyxEwjJUzLlPjQr" +
                "7RkljkMoI2nj56V5YE6ctb1PnRPNgdq16b3rHBx3NpIWZRwvZV" +
                "DkCP1ShMlDvgcenTMgDxLt43xfnBvVIJnY57Pnccg/13K/Ohbx" +
                "EXOcfV/UOJmd/a+qHe1kpe9QThZpJk8XrE+js7XnO0/5PCivah" +
                "n1yrc5ksU5ld0r3Kfm5FTI0vsO5VTunFrN1BurcDjrOR0p4hEf" +
                "LefKUb3ybTazOCey3k/VnJyoNKcncufUaqaWVeFw1nPaLOIRf6" +
                "+cK0edPd9GdmV0xvI0hXP6apHnqTf1nacjn0d8uBzDUTOMe7hx" +
                "GGpoUQbjTgslEoMI6NvnfSVHG/TPNVPzpLWMzHkRQvKUTLkPqf" +
                "c9oyT+bTgjaYOoMBueq5zNkmfT8dm953eaqeXn6zzNv+fvXlwl" +
                "o+6CWbxm0m+tx//ZuEtrJq6rYj+R/m3Ct/dl5/Daz/0Wnzg0Sv" +
                "6SNFtsG39M202NTfZcPco1TlJujyiNdpoqHmY3j4BmUzkmnEHP" +
                "XLJnsu7cPE01+5BmasV5m9NcHhP7ZpbRzI5oUbQIamhRBuPmx0" +
                "jLMdR3n4l52p4X7tP1ptb7HlDHWXGE5ElYzYzrNV+ebbxUZ62j" +
                "JXvUUqnVc8FjidlcHC2GGtpEsxjHzTtJyzHUD9vzwn0mc3qDb4" +
                "m6bK5EPFlzrGbG9Zovylwd1zRrHS2Z05rU6rngsYhP2fv83SvO" +
                "wbvnq2bn3fP6qt7fPY+XVHn3HFHyDbzy9/nZHe603+uulzK0gB" +
                "HhAvfL0zPTSGk8XeQ7D+l6fFTwPf90Gcs8Fm4GyDrsR77Tm8qy" +
                "d3q7w2ao/Rv2Tu9LZ/9Ob/3vs/VOb3dxr+/0Wg7Bd3pbTxa9L+" +
                "Xe6TVHy9/pbQ40B6CGNnmKHcBxN0KJxCCCxlKONuifrK3PQWkt" +
                "I2dP0gIheUqm3IdmpT2jpHtpOCNpE8pLRyLG5F++7eqtp/E5WE" +
                "9P87XoAv2W5/Ts/pZH8oFr37bha7+prv1XZuHaP2O2mpcv8LV/" +
                "ptdrv9L7/AuiBVBH2dMtjNy4O0JalHG8lEGRI/RLEbpLfA88Om" +
                "dAHiTax/m+ODeqQdKt+ex5HPLPtdyvjkV87I7VH/dDDW2yi/Xj" +
                "uLsh7u8uJQliZJ+PuRR9kc/kPH1deuORsx1XISRPxHaXEY5rZU" +
                "SyJi+aA6JbT0o2UitjyFgyW75uwNHK3gTobvbvJCQ2fLS+WfBt" +
                "z1Do/sRfT6t9E5a3+hV/L9V5ror31reK9aO1sB7XUyGbMIO2du" +
                "up+H2UjTIXeZjkVwHmmnRUuJ7KyN3kuz69ngaY2fU07SVvrYV+" +
                "H5Xq7Xqa9pbZsoLm1K2nqeZaweFyk/6906R/UcLfRxUdJvC/SY" +
                "TW0/ScXQgF5zSL/QF1F7xQY2d6tHZP3eLdWwc84rgoUuu7Uq/Z" +
                "5dt231nuvVwfLyzSq2ufnqO29HbtF/4NZ9eFv/aRQ8m1/3hv13" +
                "4645dASX39MOv9KIwjbA6bJwrW09XF0UnG2yLmeezybburZu49" +
                "pM3R/w8t6YTU");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value3 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value3[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value4 = null;

    protected static void value4Init()
    {
        try
        {
            final int rows = 57;
            final int cols = 85;
            final int compressedBytes = 2840;
            final int uncompressedBytes = 19381;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrVW32MHVUVf5oYY8xKSxBl3QULFhSXpvSbpQszj5mFIgR1pZ" +
                "+pRsputVBArBEl6t6ZbjuvPFpxNRXxTy1Q/9Si+JW0/mVSovEP" +
                "jAVriPXbRKyQQsI23pn77pxz7vfb99roNPfrnN8953fPu3PvzJ" +
                "1to1Fe8QdFElf2/br2gwa5JA6w3V/FMlVisijbPk9Yr7Kz9y2u" +
                "7d66Sevt/7Rem3mDysaHsg3QApzLml8zPqRK46ddtutffCNFlr" +
                "UyzcyJ1vQiHwe3FxsL6O2yE4+I1OH6ZP17ftSMA2z3V/KA2zvI" +
                "cGkd94idnb2v5OC2nj3r1scjLj1byNNFdWt3HdMJBffvbDE7zc" +
                "slWLp7IDyi2ZVJW/P+Ck8fV2TbO+XLDluXcf3FqM8wT1fw9Kpo" +
                "2+ep5MBucrNlG5zaKfawI+IXiNTh+khd+6UZB1jLaI871tOVbu" +
                "8gw6WLuY2dvW+xIsR69pxbH19g1o8PlasZlLIu2sWdUiulpjpO" +
                "smayL2rJo9TC+ND0BO1DOai5ypTawvrpRcAGj3J8SOVAbdCVno" +
                "4NoykXsJ8sTBaKXJTVqBfKdrFeSihGIqBN5bKPtA+9uc1VtDf1" +
                "XN+dBEF5UqbYhspKtSwlxTXmEdE+pnGpnoAx2Gc5q+YvuxCtp+" +
                "9ig+w9jK9Wxd2sWpPY5Txdxd7PzvBypFxP2bWMPxOx5Tzx+4it" +
                "4mkNT6PsNZ6vRWtOk6VVeSu7jX2oiulaxnc+NsE+xu7k5Xq2sd" +
                "Jr6ym7h93L7uO1+xnfUdhn2Gd5/nn2EBtg76hR7yzXU3YJG2KX" +
                "luspey+7gr2PLZbzlPFnHXZNhaxWHHY9G+P5DcX17EZeJuwmNs" +
                "7LW3hax25nd/Dyw+wjdD1lm3jazNMWtrWSfYKnu9g2NsnX00+z" +
                "HYT1g2yXfo/iGc/HP6XPbJyEFPYo9Q5W7+Xqt1xiQun3PmVitg" +
                "ys1XsZ9ijjvb+ErkvmOBRj+njNq4ztSrM00/b97aCFHEqXNeue" +
                "O+LHAxe/H0CWNdzyc3B7yX5j1kMUfPGAqKOY7tLnjXs+mudd8T" +
                "mot7f48f4Z4L6/RMv+LJU9HzKO7Le2u8N0l6CYT6fTIhellIl2" +
                "8RCVSAyt4zaWgn1AcJubqDXsGbNSbQIasIDDWuoReoOVvTvtI6" +
                "LssZb6oL7oaGt+M+lM5/c5Uf9SL4AWcigd96QVUWz244GL3w8g" +
                "yxpu2W3sfThkHNmLZj1EwRePdHfaueezk/X494EWcijt71EUQd" +
                "ay68zezVy8MUXIsoZbfg4+L2Y9RMEcD31PJ/v+oyH7vn33Nu77" +
                "a/4H9v01Ifu+/k6jP/P71ng9dsVX6djofqD6MY1ft5+sxjGW71" +
                "G+eNGxuCMr5eI9yvSbJavVGRHyZOSfC533qOXJcpGLsvK4XLaL" +
                "r0kJxUgEtKlc9pH2oTe3OUl7U8/1qAmC8qRMsQ2VlWpZSvbuNI" +
                "+I9jGNS/UEjJH90WRUW20qWZniPUIr24AFua7HFnX77a1SLnqY" +
                "GNCemIMJT5GYo8q3Pv3YQ227eGA/Ppm0kY1m/F0yuzqrzr3Yl8" +
                "rn06wT/Uw5Q8pvN+6Py6w7582aZFX7+HxPXrN1SnuJBx9Znz2+" +
                "YOmxgqC+yCXNRg+X8fz0HiWmW7LJCmEcTb455Py0/ZzRu+X8tH" +
                "zft9rabj4/5fnXXeen/H1/R6/np2yb7/y0RlZforKdNev7lKjt" +
                "MvXq6kz6/vbzjfN0ZfdaTz5Xh/QPQwV+3ahnZ/ENJaZP9G69/Y" +
                "f+8+xWH68IiqkBxV4Ivu/Jt5PsJ3Xtp+q3E55O2+79oLnzs/Yf" +
                "+/XtpNI7vp1kP7ZGa2nIvR8v7eXbSXlF60XiXF6qrTaxVuZQuq" +
                "1Z5ukpPz5a7/egI2EEPhtyXDZEJw6nqG3VB3hws433ke+mB3Vt" +
                "z/f+n87XempnWxRB99Sf+8Gi+Jay7z+h3vvZf7rZowzPUq+0/2" +
                "q69y18vu0c8788MbGuG/GyoN9kWe/PUvlR7VnqF7X2ymqPOiZi" +
                "6v8WzSJrTP/mf5Zid8OzlCNm5Zeci9lZ/Vmqan3AEdOVQevpSr" +
                "bBFFP2SXU9ZTstFp7Sa8WvePo1yKKz5d+hRGdFC3Aua2EaKo2f" +
                "MqGis0pMN1JkWcMtFa97c43AxgL39tkp3/6j6jQD1tP0ID0riC" +
                "ZMJwemeWo/j2j//Xyd80cT1jO8g26WnTjMgV7aiib85/zIzun6" +
                "96//ZiJ93IwBLI4pSPM322zzmL7m9q62VY29X1kXbZyb+shxuW" +
                "279MKXu39zwFRvDthONfWYSmzT81bVfn2+K75tNtg82r9HNYPe" +
                "/JoDjR4vYJy/pa69lWJm/lHl/wy9M41Mj4XEy3TymW9191NPcG" +
                "0xHR+SHOZzYkpPpkNHjWL6tkZjz/EQH13E9Gi/nj/33ujWO+ap" +
                "hUN2tH/jrPauI3ot/blIUiZ9iRbgXNb8murvT49QjMs2ZoeRZQ" +
                "23HH9/esTH0sUCervsxAtE0p5P22YcYOfxHvWG2zvIcGkd9wI7" +
                "O3vf4pEQ6/nb3fp4QXgc8gvrmXBMpHo9fT18xbFhkmfP9Tsp/c" +
                "ZnPLHycMgfrPKLer73D+m14gSVxVOkdQgkdmsqBmtAT6XxIR1l" +
                "80N5AyeumfKN1e3FxgJ6++zEh/Va8TKVxZP5u1HrcCnxWVMxWA" +
                "N6Ko0P6yjDbLqEIis+NSeumbRG67CNSwgL6O2zk9YnfNn3ZK31" +
                "JqqNP6VipcRuzY7BeowvW7Tt2KcQsqyVSfhLX7L7zQd1lgbUsF" +
                "kvpeDbzza/rI7pGmX0d5nWLt+3E9+aq1sNXPcXuf3Z7eaXBtm/" +
                "vNf11PSNr7VW2S9vMZ1LOU6OOudSxTo4l0qO+M+lwr7x5Yvd3/" +
                "hs51LsBsnB/43PdtYX/o2vM5N/X8d0mzKjtBO4OOBMjmKSH83H" +
                "Rle77XafXZ1Dv6+c/G+0fCmcoeQjjf/jy/4slV997r3TU7K8/l" +
                "7YmjLjenl3S55xe6drostTvtr8vh/wfPpMyEqfX+fWd/O+L9fT" +
                "/Ew+1v0TvjemP5zPe0N/56nO4VxfcO+3dvTfenqib2tWPN+Y9o" +
                "+DZ6yPpY91uNa7YevLoIUcSpc167v24348cAlnLWzgloPDN8PG" +
                "YdZDFHzxSPen+zsxrZ98Wl8BLeRQOthYEemLfrzk0pr2+4GeZQ" +
                "23/Bzc48hvM+shCuZ4xK24JXJRSploN1dRicTQOm5jKdgHhG4N" +
                "e8asVJuABizgsJZ6hN5gpRyXbUSUPdZSH9QXHm0ynAxrK3glK1" +
                "PxHaGVbcCKlmxTPbZosi/l0ga1qvfEHCjehKRok2U+A09Q2yoP" +
                "y8427JN1xjSYDIpclJVmULaL70oJxUgEtKlc9pH2oTe2b/Jcsy" +
                "MIypMyxTZUVqplKdmz3zwi2sc0LtUTMMajbTSiJ0VOS1mTeSWZ" +
                "q+pzEon7Q4lt2TT2HvKfPieiOdwPs8NskcU52tb90h60LrlQCe" +
                "0t+1M5WnnzNO/cGfA96g7QQg6lY++wIpJTfjxw8fsBZFnDLT8H" +
                "nxezHqLgi0c6m85qZ33wvzdnIYfSwWa20dVF8cDF3w+QZQ237D" +
                "bqs75Zz1nfrIsreKC4aDQaFXk0imWi3doHWinDeFHuHgC56I+T" +
                "lMla8jvdAvaOGYAFitZxui3MF3IhUTlQPpgH1mK7qi8czbr3WD" +
                "QGdZHyM60DZKRjUiOTROOe5qvsK7TJSezL1lO0wY/ZMlgSSGBI" +
                "bUAu9MlJzIiOQB2Pabyir2kcyow+kB6g6yl/398EWsihdFmzrm" +
                "V/8eOBi98PIMsabvk5+LyY9RAFSzz+C+3NYCY=");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value4 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value4[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value5 = null;

    protected static void value5Init()
    {
        try
        {
            final int rows = 57;
            final int cols = 85;
            final int compressedBytes = 3145;
            final int uncompressedBytes = 19381;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNq1W2usXFUVHhMxPHI11kctpdBSDDbVBuojirXOnTNDfBYVBd" +
                "T4FqtifODzh4nd3Jty7v1hKiEx/CIGE2MMqA1JbVoKFcMPMEZE" +
                "qBGMb/xh5dLc3prKH885a9ZZ37fOOmfuLZeZnL33Wutb3/rWvj" +
                "PT2TPT/vH+8V6vf7w/nns9tcr13D72KIbXGEevZiJjrzf8M2Ox" +
                "st48IuJhXKSC9aIarwHRrJ774xqmhbvNTmYnZZS5jIiVnZz579" +
                "wt6mGMIsxmv+Yov2UXe/pXzubKuluMYJ2sFDm8Ks+snlJD1BHn" +
                "RH35SqYY+JeyJRllriJLas/dph7GKMJs9muO8lt2saePczZXrt" +
                "URgnWyUuTwqjyzekoNUUecE/XlK5li4D+VnZJR5ipySu7F4/RO" +
                "9TBGEWazX3OU37KLPX2Ss7lyrY4QrFPxc3d5Dq/KM6un1BB1xD" +
                "lRX76SKTb+Ky+48oKeu4mvvOb2S1Rtw5rfx5mxyT/8k/oVvefq" +
                "XnCzTNSgee1Ii+7Z5PWaBuY2PV23KM4+4Ugz6UVij3KNpNl0fj" +
                "FeVOzp3WlT5bl4lKdLi/kOxabLK/9rxxlvKK43FtcVxfXDtMPY" +
                "0iCN3J4eq/xXp/enDxTzNek6ri5WUeGG9IUxx5er8avF9c30rT" +
                "SVXuiQ69IFY1Ubi+sSiGyte3o9a0hvHWdcOY6/Pb07XeV3jFWl" +
                "j7C3qvB59KQb09eKR+titiijzNUjeFHtuYPqYYwizGa/5ii/ZR" +
                "f9/JGzuXL9LCIE62SlyOFVeWb1lBqijjgn6stXMsXAv5AtyChz" +
                "FVmQe/F6+kv1MEYRZrNfc5Tfsos9/Sdnc+VaHSFYJytFDq/KM6" +
                "un1BB1xDlRX76SKQb+E9kJGWWuIifUnntIPYxRhNns1xzlt+xi" +
                "T//A2Vy5VkcI1slKkcOr8szqKTVEHXFO1JevZIqB/3R22r+KiC" +
                "87XTxOfy1RseVSjI0cV0YdPf/wb+pXdFMBZ6IGy2tDokav1zQw" +
                "d5cOrDPJ5zn6T+tq5qx6dX3MrtiZT/dWfBv+pYuxaftIe165Fh" +
                "vHKEc1dHN3xaXWxPxngj39bIwxbMzQFRk+sZxcs9tZfbRci41j" +
                "lKMaurknddR/pmUftvS3yFjO5hN77rcWVR/i2ScXW8prFYZ/bz" +
                "JgdVRgDIxu4ppcqM1G8XgNrAd1YBR5fS3czfF6a3k3S66i/4fY" +
                "pxFD6Iwjr5oVfKzpU6zViXKQVfVhBkfM198qfSHCV+ad4H7x3t" +
                "4Rnwjguf/FGDPpxLGcE0g3ou0E1JVnpygZ92xqy8l/sjxd7fHm" +
                "ibF5DsW4nQDzH/tzYbT2Z9fobIoVmEHOptF50xCsk5UyF8blbI" +
                "qaFLf3u+0d4fk2OnNzpehsOn7sXljezZKreO4/yD6NGEJnHHnV" +
                "rOBjTZ9irU6Ug6yqDzM4Yr7+hdIXInxl3gnuF++RejnvpxenNe" +
                "mltW9tOj+tTxdl27PtadPsbeV5v7guTa+aKc/cry6ubenytF3O" +
                "++l1fN4vxh3APz7vp3ekd6V3Vu/htqf3Nc/76aO8W2l3ed5PxW" +
                "tP+lJ53k9faZ738zvTy9LLC29x3k9FV2lD2pg2p0vSK/VxWp73" +
                "02vsvJ/enN5SjDsLDcV5Pw1TVp7309v0vH/T0fSe9N7iNe/GsY" +
                "pri+uDxfWh4vqwnPfTx4rrk+lT6fr0mfQ5Oe/Xqqvz/nifN5Z3" +
                "s+Qq/t1/mH0aMYTOOPLKVxg94GNNvGKtTpSDrKoPMzhivv7G0Q" +
                "Me4SvzTnC/eA/Vr++vl1Fm9Yk994hFEWPrOB8v5KweXT9tZmoM" +
                "VSGCdRrWK8O414vd5nd51b6a8mPU7wXWot3c0N8go8xVZIPac4" +
                "9ZFDG2jvPxQs5qT3/ezNRYvVdUj0fEemUY93rVV475z7xqX035" +
                "Mer3AmuZnvEpdQrnbErfSxWrb/R6s2/SeK83/Qlb27+M3ucZ8d" +
                "/GbArR+X5hxeqMaDl5V4jZK6KaVq/kNS+ytq1vOuoZWUukrFmh" +
                "f1n/MhnL2Xxizz/PoupDPPvkYkt5sUKTAaujAmNgdBPX5EJtNo" +
                "onP9BUj3WMH6PI62vhbkY3eM//bY5MNz4zKT2zEx5XnLX3ju74" +
                "s78pXztv/ovec3jLDmQHZJRZfWKP7ssOjO4TP2IUYTb7hcX4MZ" +
                "If5mxmRV2I4LXgS3XM4VVhT9htflfcEedEfflKphi7xdv886tI" +
                "/aoyf5b7Cxxd1t+pEzV4avl58y9oiyxHSTumXcOqPE4PZgdllF" +
                "l9Ys9flh0cLIkfMYowm/3CYvwYGSxxNrOiLkTwWvDChBxeFfaE" +
                "3ZaZUUfGqCp8X76SKQb+Q9khGWWuIofUHvxIPYxRhNns1xzl9x" +
                "HMZtZanUPwmtljpEW9muyQ9NXsiHOivnwlU4zddn0aM7pfru7P" +
                "aWanVvIp1OjBM//0amW3ts+lujSsynP/SHZERpnVJ/bo3uzI6F" +
                "7xI0YRZrNfWIwfI/nvOJtZURcieC34Uh1zeFXYE3abPxx3xDlR" +
                "X76SKcZu67/dnP1mov43Ypqj6ajHRrfqNxOtiNGvQm+BT/eb1V" +
                "0B8wxZrtCarGFSlTiuXqvAuOye7B4ZZVaf2POZehijCLPZrznK" +
                "j5HRbzibWVEXInjN7DHSol5Ndk+pIeqIc6K+fCVTjN12vuf/zu" +
                "q/4o0WV+t1dGbPGb+eLvae41s2Nb25OHVsDs77s+68v1mx3adg" +
                "Pu9zhM77vxdWrC4IrcCVmud9i5syzO0+7zPCn/dNVdzp9Oa4Qn" +
                "Y4OyyjzOoTe/4a9TBGEWazX3OUHyOjJc5m1lqfQ/Ca2WOkRb2a" +
                "7HCpIeqIc6K+fCVTjN1WO36xXOPnVP2KOzPv/m4Xe+wZPPdPNc" +
                "7nAaPakyph3Ktrz1UNK2GPonHcvpUa9v03Z8Xj9LqV//40/o4P" +
                "nu/H8Ds//v2paXi2vz8tmdp/f5o/tjq/Px32o+/4hruGu2SUuU" +
                "Luknu2O9utHsYowmz2a47yW3bx/NjN2Vy5VksI1slKkcOr8szq" +
                "kb6aHXFO1JevZIqx2/r5sG+0b/yM31c/979nURtt7niGtyLy45" +
                "PxpmVyHUOWK7Q6NPx7eX3EcduFeD+GO4c7ZZRZfWN7QT2MUYSs" +
                "Z6e8X3OU37KRP6qMugzBOlkpcnC8yaye/PG4I87RSKwGe3W7OR" +
                "gOxvagjlS+6loUb20DBnM4bijkd69EA62CiDgTNTA+QjI6Yra+" +
                "sIM2pVgn6qLLHt0yuqXxnv/7FrXR5o5nTSticOlkvGmZXMeQ5Q" +
                "qtyRomVYnjtgvxfmSPZo/KKLP6xB4+rR7GKMJs9muO8vsIZnNl" +
                "1GUI1slKkcOr8szqyZ+IO+KcqC9fyRRjt8Ep5dh43pZtiyMTzm" +
                "XHuiKe8zk8H7br2LayjlZ266/tr5VRZvWJPX2tRRFj6zgfL+TU" +
                "mj7TYqYKEazTsF4Zxr1e7Bb7aurRPrgLX4Nr0W6u66+TUeYqsk" +
                "7t6essihhbx/l4lWP+D0VYHDMtpgisxyNivTKMe73qK0fsq6lH" +
                "++AufA2uZXrolXdv8LnU7c1o0wpfx1sRgy2T8aO9kys0keUKrc" +
                "kaJlWJ4+q1Cowbnjs8V0aZ1Sd2/i/1MEYRZrNfc5TfspE/qoy6" +
                "DME6WSlyeFWeWT35k3FHnBP15SuZYuy23v2bg8fpD5rRphX+LV" +
                "sR+VOT8aObJ1doIssVWu0cNz+yvD7iuHqtAuOmz5k+R0aZ1Sf2" +
                "4Cr2KIbXaKPX+A3RZMPKqMpzGtqwhsMoV7RsYyn7auuI1WOUa3" +
                "At2s3zps+TUeYqcp7a+X/Yoxheo41ezTQG82EG4mME6zSs4TDK" +
                "FS3bWGY/3t4Rq8co1+Ba2O302dNnyyhzFTlb7fwEexTDa7TRq5" +
                "nGYD7MQHyMYJ2GNRxGuaJlG8vsDe0dsXqMcg2uxd3W70vq/406" +
                "c3f9venXW97VLqz6++SFNru7FkbLtdg4Rjkz+5fDPXOgPS61Ju" +
                "1Dv/63Y+aienVwEnblt8H5y2E0u7sWRsu12DhGOfnp5fXRHpda" +
                "cXy4ZrhGRpnVJ/bogHoYowiz2a85ym/ZyB9VRl2GYJ2sFDm8Ks" +
                "+sHumr2RHnRH35SqYYu61eB14iV+O9lPu2WnGGPYPH6Ssa3+sE" +
                "jGpPqoRxr649N//fytmjaHd8dOvo1nKUta4GO2KMIWz26yZ/lN" +
                "GW6ZXEzMypNbgOjxKXvjSbO/D9cL/KoPew2f8DfN18xA==");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value5 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value5[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value6 = null;

    protected static void value6Init()
    {
        try
        {
            final int rows = 57;
            final int cols = 85;
            final int compressedBytes = 2769;
            final int uncompressedBytes = 19381;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrNW22MHVUZvtJqpLWRELRGWlBatDbSxhUxtsXMubN7VfCHSI" +
                "gpJGhs1bZut9tF24ZfzJ2T0L2gMTEmGuIPo/+qqQb0R/vD8MuU" +
                "gNGEwMYuxI8qXQ1+YIkxauOcee97349zzszc2V3oTOY973ner+" +
                "edO9/ddjqw9Db1NnVGOmydzslXOmwBH76hN48MLy42FBGLhDnV" +
                "CWeWObEGYg+9W2aiDuxhHi070P3IfjEDrp2KJbs2u26kf63Yrn" +
                "da97vCZ1v+WCHfX2w7im1iiN7KPHZF89+RfTK7c6h/OmC/X82/" +
                "VMqZYjtSakeDWd+WvX2o3VBsm7N3ZVuK8Wa+T7NbSvmhUu7Obi" +
                "/kR6GvbDJLy/HjLOOnsrvY7DPFtrfY7i22+0rkfPbZQn4+25d9" +
                "IftidjDEaiqbykDCiBjMu3dIBH2kzuccpfzk4WfjlTkrnZO8yZ" +
                "f8uFVWpGjK4vqKdSTZc6usIWvJbvV5zY/0wfEm577dMM653936" +
                "+p/73a1Nzv3ulti1bpxzP3tcnvuDTJ37Xwmd+7RPm5z73ZuugH" +
                "P/prbnfjHWnvuTE5MTIGFEDOaDHBHpgx40lzjGYH6KLvbp9TJa" +
                "Vua8yEPylEx5Ds1KZ0bk5H/CHcmYUF+6EjFm+XdP7vb2c4m5bf" +
                "ANsOKcfAnvdOwGaecZ/fzdGxGHiBADGck5hPylJ+eo+RIHmbuK" +
                "B69Th2GO/q7+nkJu76doyX/R/yBoJ//XabD0J6KWj3nIbZ3WS/" +
                "8Tar6jxj+JWeyDkYhbPaTbWfZi3LXU4ix93Lc2WabSdraVXeJs" +
                "m3FYDtP81+rKP9qng+94v9s/G7Hp8eM0f2pU6Vf9S2hb/pI/XX" +
                "Oc/r2OX30XTY/T/Jn8l4Ff9aoRl9HRmT8bOQKuqq5gB3Hb/Jom" +
                "GWleXYtbnQ5zLkMxD1/XJLd9JG6HWmH75PrJ9XxEHeaD76EV0Z" +
                "AuNyen9oTygza1B/PrTJoXeXCpmfIc0s4z8y4dv3hHlB29yEPu" +
                "C8lF78VeudKsM5x175NYj1l7o8gOm3F/HdsL2DpBjGftRWN4Vl" +
                "6jF8jBsd6wr14gdy/AnPPAKr3ajtS14cbRM8dedcS/IV9wsvO6" +
                "LOPWjfvrvlaN8WlfG3zfx2jmpNlXl037yEyx/GGvUJzkTZwKy7" +
                "66XqurxFhQdF0e8yNfG/zAx2gmsVi2Zhadvyp3zNNpfAbvplXV" +
                "qqvEWFB0szz5S/xZan6tZ391jGedvwyfpX43Qi72LwU9/1DKPw" +
                "Ysfy3lpfGepfI/82ep/PfDcamUBWrVk2t+IZDjH02f+fOXi+1v" +
                "+Stqz++EbXj3+rmvST/yHXfRGf3qhPExlovbNbt4LLKo66Pabn" +
                "bWxScv+8/8+X/rfMd+v3ysWUaax2vll6XV6TDnMlQr/3ezPuJ2" +
                "qBW3T865932aTc7h+76z+L7LW5afAZeHt7V93185DrEl3QhSjq" +
                "jZNzpJmJb+yHP5tXxMV4e1jnG60b7JZ8s5uHsUn+uOMQ/7FZ7U" +
                "nCQP3TfG+90mi/bN/e3JYrIIs2QRjlNCYENdSn+UkeqcWfQxGY" +
                "Fr5TlZMnLHKXmCxjm445TPZbzuLMxJWnXfGO93m94NUo4cQR9j" +
                "CQFpLM5olJHgoy3iWm9ldVgrj9O7NWvUkJPTODeuScbxSj4P3h" +
                "WvKHHmfyimOQmaOahxh6A/xUndHAzX4XYZ69baK9Yh9KUo4uQ0" +
                "c7A6NsRFemk79a1rqyu59006s3iPGvy03RW6Ox+9b4z1TXrws8" +
                "p7UOtv0jF+/jfplk83Vfv06UYZvO/83W/z76ft96mKXbHv/Miv" +
                "fp8u7zt/OlOeh19HjTC7nlsJRz89aj1UpwpNZ9zajC95gsa5I9" +
                "t4teoqPgvddzxP+haQcuQI+pi1bgSJmD/KSF3Lx3R1WAPvLWs5" +
                "4zBbzsH587numOeJcZJW3TfG626ThWQBJUfKZ+PPtTvmkz9FLQ" +
                "sr91xdlytuj/FbOXbJC/YaJ5MXYAYjR2BDXUp/lJG6lo/JCFyr" +
                "GRMHyZZzcM/8fC7jdWdhTtKq+8Z43W3lPeq5dveo5MevxT3KXt" +
                "v2HoX8VuceNXV56jJIGBGD+eB5RKQPeoBuN2g8ucBzUU6qSdGy" +
                "MudFHpKnZMpzSLufGZHkQrgjGYOWMBvC9N4sr63rQMqRI+hDvi" +
                "T9UUaq6/46H9PVYa28I5eMyvd9xZZzKN/31/EYrcuIECdp1X1j" +
                "vN9t8iJIOXIEfcwaN4JEzB9lpDrrXvQxXR3WwH1/DWccZss5OH" +
                "8+1x3zPDFO0qr7xni/23QzSDlyBH1MTghIk+OMRhkJPtoi9lUu" +
                "q8NaeZxu1qxRQ05O49y4JhnHK/k8eFe8osRLfT9IOXIEfcwhGQ" +
                "EI+euc9h08yvca7tNDsjqslft0v2aNGnJyGucWYsfzhKuk++1G" +
                "n7tfUeI19/2Fdvf97reu7Ps+8lu9d1Nb/v1uesCW/85o35MeGP" +
                "7bxL9AcxI08+XhLzTCHYL+OGodo3wL2Yvq5V8V2K12S3rArZWM" +
                "NxEHqo48oV56wI32vaOYbT4HN9obosepx4L6JrvdjIi9mV15l+" +
                "z7nEyWCj6Pwgg4aE4SpqU/ykh13V/yMRmBa+Uz/xJxkGw5B/Oo" +
                "nMt43VmYk7TqvjHe7za9GqQcOYI+5gghIM0RnNEoI8FHW8Rxek" +
                "RWh7Xyenq1Zo0acnIa58Y1yTheyefBu+IVJc78Z5tpNENMj75/" +
                "uE4MTWfdWvtdahZ9h+f2dqdxxvG/mQgxDnlpu+47nie9B6QcOY" +
                "I+5gQhIM0JnNEoI8FndFW7hbxGx+kJWR3Wym7v0axRQ05O49y4" +
                "JhnHK/k8eFe8osTL68B5u8PJ5DzMYOQIbAXbY4SANMdwRqOMBB" +
                "9tEfv0mKwOa+X19DxxkGyRk9M4N65JxvFKPg/eFa8o8XL/7gUp" +
                "R46gjzlMCEhzGGc0ykjw0dXEPj0sq8NaeZzu1axRQ05O49xkXc" +
                "44XsnnwbviFSVe7N0zyRmUHCmfT3/T6FvhGQ/5YXPfZXyjPNPW" +
                "HuO3cuySi3ank8lFmMHIEdiKI2CaEJBmGmc0ykjw0RZxnE7L6r" +
                "BWMyYOki1ychrnxjXJOF7J58G74hUlXvMetdjy++lPVuM9ysvV" +
                "+t/4kN/qvUel60HKkSPoY2YJAWlmcUajjAQfbRHH6aysDmsd4z" +
                "Bb5OQ0zo1rknG8ks+Dd8UrSpz5z9Vr5iifOekQ9ErnQtkoyreQ" +
                "naN2Ip1La/9KDDlIjsjJaeZodWyIi/TSdvsBGR3Lk5xNzqLkSH" +
                "nu/7bR9fis943im819l3EfONvWHuO3Uuwqr6cvtbuezl8T9b0i" +
                "rqfzb216PW35a59LzqHkSLlPLzbL4CGno5ZzPtaeeVt7crpxJy" +
                "3ZpveDlCNH0Mcct7fxCIfgjMbiuvNh8gAfXU1cT4/L6rDWMQ6z" +
                "RU5O49xkXc44XsnnwbviFSXO/KdjmpOgma9q3CHon06HslGUby" +
                "G7jHVr7ZEwjb4URZycJuuGOFRX8VlQ37q2F7sBpBxRsx9xEjAz" +
                "gz4ozQzOaOS5IIrXIq/RPp2R1WGt7LZkZHf5bJGT0zg3rknGHF" +
                "d/J+3x4F3xihIfXjWeKJhQ5kdQG6j/WWxKT+4rrTxf2BJbTHQe" +
                "j+29U1rNyNtURpoRPzMmK12rNv6Urw0uScw8IGanCIlncz6DV8" +
                "MWtGvUnPK9YnUkb+JUWB6o67W6SowFRUfy/B/oj1zu");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value6 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value6[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value7 = null;

    protected static void value7Init()
    {
        try
        {
            final int rows = 57;
            final int cols = 85;
            final int compressedBytes = 2333;
            final int uncompressedBytes = 19381;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrdW0uMFEUYHo1vRVTYBR+giKigrBFfCTFxe3ZmCfGi4PuBGh" +
                "EVURB3D0h87EyX0rbR6IELESN40LhqJMaD8SAJ8WRiojEe8OAL" +
                "fEVFITGRA/ZM9d///1f9Vd0z9C6b7U29/uf3V1dVV/X2VCqNxY" +
                "1ro2mNBY2BSnqNhI0rdC0+rmJc0RkV62osMin9b6WcJZbs1ZWu" +
                "r8ZSo92XI9/v4gA+S+NKi1LtHGc42O7THkefXmb16fRCfbp7gv" +
                "fp7rHsU7zqm+uboU8z2k6dWlzMsXRi3uOSaGxxeZexeBHv5JKt" +
                "Gm25bfTvKRaHzMdeKNoflUptDdTia7q7P/1vV8bhQpyd8scDXz" +
                "BdJ3OcxktluYCtAOEUC/MO53y8MM870mgp2nqQ8010bl3A57Oe" +
                "zw+m5+qP2rX4ek4L1rPWKFLc1kwZykE+pwajtpTLD8eNmBLO+r" +
                "xY/V5cKFBbtlNbVVulc10CTbfj5UDhMiCh6+EUkw46YB+1k6fc" +
                "mVybe6a4UILj5EipDc63LQPl+ZVyRFwHODIaGivvTbxGsnEc3p" +
                "aN0xWVSvUrcoeGUtk+SrHnvnFfh9hKcbvEH1nBaSMP5o/RFrKR" +
                "XqIzK0lz0R/3a2q2NQZkembxVp//kVUjG8V1fLg2rHNdAk2345" +
                "W14eqXmk5lQALbnK6toH3UpvYlzxQXSnCcIF/90rRhojItA6Wl" +
                "KUWEFgGFGZfpCRHTaLN1+xDUmsdmY2qlsbYfMmVlC8U5Lgls+3" +
                "Upt1XXbZpLOpv+K4bLH1H/IZlfv6F+g85bJdJ0u7oduUCj8pym" +
                "UysHPbDPPdgWqHeKAC1waVvOtkWxYa4pNC4bD8VBudSu6Yv2pr" +
                "hOZHup6pvW/mV/kacx6AnnqAPl7QDDB3LOUfvy8BU4u3V9jgr6" +
                "dLLm/kOyXNBX6u5YsAhtt6fqVJNvonPrRhflSRThB30+ftCrkz" +
                "VOb5blgt5S+1SwCO08T5RvonPrQlydWJe4Mr+6uLpY57oEmm5H" +
                "8zgFZHidtikV7aOEbY16pqhMmyiNsihHudwjaqOVcNQdEUdPud" +
                "wH98WjFdapx7Pa+rJPweETZVmqnp7ra133uod3td71Jbn8ru/h" +
                "Qhasd31R38R+1xdNHft3fUGPTuld/JTWsIVyKOsYB58W4+m6ZB" +
                "HaPk8tbco30bl1AYMpYeL2xxn0yPwwClX4YvgMoz2baR0sNJ+t" +
                "s2n4glO2GT5f2jryXA7/aWdvFIsrPqwn70KdUlsvZ/S9shzKOu" +
                "bWJQa6jxyoP7S9oydail4WcL6Jzq0LceXF4ecHC/P0073pz3Qv" +
                "FfzY3Z4/tfW7Xk+b32eUX+Q9f7Ptp7lH4PzZzmWtz52ef6N7/u" +
                "YPaflrO99nx9X8SbDxd9H1tPlHkv5q/mP0+AU6pf12TjYSFsly" +
                "KFvK/lSwCO08T5RvonPrhqOdW5e4Pn4wQ6d0pQ7tGpdD2Y53QG" +
                "Ged6TR0mWL8k10bl1AkReHnx/M8PGD5Tql4zRbm+PVshzKys8o" +
                "b6/+4PeONFpKdhqvc76JzqebJ9Gepf1+frDcxw9m6mSeTcPtsh" +
                "zKdrzH3JLnHWm0FG29zvkmOrdu+Ea+9Xx+MNPNr+7SOS+hBjmX" +
                "xdwuqS0Xx60Bf96xvoujo2jNqGxfHBelN3ZyLyYOMyrQN321e3" +
                "yZTuldzL7SiIeNO7PMlO187g9ssO63ZRHbJl3SdaFz6wIGv/Vo" +
                "mZ8fOPm+s2l1oLuzacaZoGdTV1xlf4dC7LyTjcD3rZWoUJ+E6b" +
                "cWA1uFMbK1csSvYhhaUmWhDT/O7udgZVJeYxtXbVptGi2hrtvR" +
                "GuACVarTBDXJPtC5BVuHYzBzE6lpi/pByzTK2rRwnzsijt6MjU" +
                "pzLGYvVir1ofoQ1nVKnlHs3YqWoQmkqaZ8tXQ1N3qU+nJp6jb6" +
                "kS2jJS2JCLkNzDV/00KKiEdgxiPFq3WlOIyn2Ad2LQ5tGrY4zW" +
                "WtGMe077OdoVNcslWTonB783txoUBt2U7tqdpTOtcl0NL2WqBw" +
                "GZDQ9XCKSQcdsI/ayThdy7W5Z4oLJThOjpTa4HzbMlCix+SIuA" +
                "5wZDQ0Vt6b0jWYvUMJPyt/BY/WjdeT6Nk5Ls6mheP9VMSzafT4" +
                "5HzuhwfH1r53z796ku75VxfZ84dfdLfnHzxq8Cid6xJouh0NA4" +
                "XLgAS2OR10wD5qU/uSZ4oLJThOjpTaMFGZloESDckRcR0pLtMT" +
                "IqbReuf+k5Nz7kcbjtzcrz88Oee+K66yzvuNxe5xWt92mPdrif" +
                "B8+K60J803OfxvnX26rdB6urv7PnWP02jj5Byn6pi8Pg33dj9O" +
                "q9dVr9O5LoGm2/FLnAIyvE7blIr2USIZI5dza9QzRWXaRGmURT" +
                "nK5R5RG62YGKQItDzlch/cF49WmDPZ/5vjV8pfv+szK0f8GlsM" +
                "9a31rTrXJdB0O36VU0CG12mbUtE+SiS1+dwa9UxRmTZRGmVRjn" +
                "K5R9QmVua7I+LoKZf74L5otOrkJKV3TZ2fpClqLjyj1Lz4tXaZ" +
                "rFvqxHR9ScawOsVYnaYm6YIkLSC0S5N0gl5P1fFx6lOd1jhQv0" +
                "pY36YnqUfNSvIZ6kx1troo47R/o6baZ0l1akZtf5env5lQ6a+K" +
                "FXnLpnqTNLv1zYQ6l1DbZ1U1P4ndwKBOcq7JBddTdbHKvr0ZnD" +
                "04W+e6BJpux58AhcuABLY5HXTAPmon8Szi2twzxYUSHCdHSm2Y" +
                "qEzLQGlhkCLiOlJcpidETKP17fnjr637tr+jJ7Xx3Ff1ZJyW9v" +
                "7C/W1P6t35PX9xDIXHaaAGirxDaf4b/1hon9jB/02TPu2ZAH3a" +
                "M95PRTJO9xYZp+4+FcfprAnQp7PGdpx6v5P+q/w9f73r31eUeD" +
                "btHduzafYfgffsWvynTcMW0KRxyuXzOaZ9t76px3FLUbi9+b24" +
                "UKC2bEetVMsU+S2vWq7uzPZSd8f7isx9lf4yRt1HaPfjOFXZb9" +
                "vULcncP8sxe+5IS+cvydRNvrmvyG+D1V3S3FftX+Sqe5JxamBQ" +
                "Nx723F+h7vWeV3fYtQ5PvB69eqnfrnZ50hl3DPitZPX9LvvUo1" +
                "c/bwL06XlHrk/VI10+kY/2xDNnAvTpuGOovmvXurUgxHPZBOjT" +
                "scXwP4akC4w=");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value7 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value7[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value8 = null;

    protected static void value8Init()
    {
        try
        {
            final int rows = 57;
            final int cols = 85;
            final int compressedBytes = 2108;
            final int uncompressedBytes = 19381;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrdW0uIHFUUbQZFF45u/OCHDIk4CYmEkM8ExEUnVYWM4iKKmY" +
                "AZhGQTFfxAIFkIOt1dSSapJIz/YPxEVNR8FKLobnChbly5cGdA" +
                "BNeSKB1wY72+feve897rnu5Jv+4iXdT73Hfuuefe1KerelKpuJ" +
                "/GFR4du1xZ1id9ofNa8mBl5J/ha9j+rTtaLkM5P+mLQ6/pN+5o" +
                "uQwhP41Xll3Tl4KeBTuSHdSaXmw0P3ZFVtmm8WijHWfMKxGSFS" +
                "6Djq4VCAOiXZzLpbVJ28atcNXrOMKvVzWvHUtXM94T76GWerNC" +
                "MzPObmILYhghc7SzD/OLdx73PvTGyJwNIlAnKtUctiqbmS1Ggy" +
                "8j9PHlZUcSxTpbz7F7iUfZzQHOjAdKcI8auoakuNtntwVgX1uC" +
                "mg5dQ326qOkdg2HM7lT5TJWgpkPSsO1rd5Td49pkhrZObL2t2P" +
                "zduDshzciXRedo3aN0UiHevfGYz1yjqGmA605ydx/H92TXb0Rz" +
                "S3xjenUQGpbxTa1ee6j2cKVSW1uL0iPZaqppbVM7pzUO/rDLUd" +
                "vYib32iGOZSrYuV2tt2pqvXwJf7VjTDhpqmyHXQ7ll+2DqXDtb" +
                "HCcJxHi7VXHruS490Rjv4d8uK/LZnL7Zq5K54hk3fdfLeryHyO" +
                "/B7C3S0Bqfatvm1frr6RuW/zttLU+350db7cl0oa971ERR00d7" +
                "ep4Z7+vcnyjBPWroGmrHipo+HqCm95egpkPXoO5RjwWo6aoS1H" +
                "TVCGv6hHMMX+mnpvY9Kk1q/ySrB/Y9+pclzre/O9a0Zw293qPS" +
                "bWnUdf1iUdMnAxynm0pwnA5dQ1q8qcueCpDPvSWoaXAN9XUw26" +
                "DuUbsD5HN7CWoaVEM8E89QSz3b2vNn2IIYRtC4MW7b2Yf5xTvP" +
                "Zz16Y2StSxCoE5VqDlx3mdliNPgyQh9e8avRuWI1ux+n6feV6/" +
                "JTH/57qeIJLXs2wHk3WYJzP6iGeH+8n1rq2Ubz7Dm2IIYRMkc7" +
                "+zB/9rzYk7vQGyNrXYJAnahUc9iqbGa2GA2+jNDHl5cdSRQr/k" +
                "bcoJb61kqD59lrbEEMI2SOdvZhfvHOa7oOvTFyoQ4QqBOVag5b" +
                "lc3MFqPBlxH6+PKyI4linW3X7/wBfl9MNvTxru/lQOf+hqDn/r" +
                "54H7XUs43m2WG2IIYRNM7v+5adfZhfvPN81qA3Rta6BIE6Uanm" +
                "wHWXmS1Ggy8j9OEVvxqdK1Yzf/oq3p/KcVq8P82c54Ffr/X96f" +
                "zc6N+fHvmtl/en/TybQmUX4gVqqWcbzZOLbEEMI2SOdvZhfvHW" +
                "/L7IWpcgUCcq1Ry2KpuZLZSXmxH6+PKyI8ULbjW7X0/n69fn99" +
                "N0Y9Dr6WQ8SS31bKP5fIMtiGGEzNHOPswv3prfF1nrEgTqRKWa" +
                "w1ZlM7Olcc6fEfr48rIjxZNuNb11XueOBnjPvaUE3/kDa+h6j+" +
                "rpN5c+f+O7dfT3qE4arN/4fh/cb3xyPU0vBThGxktwnA5dQ/3G" +
                "oqZ/DJ59/sToa3rkwghr+meAu+K50dd0+Bq4pvVm+leAfC6UoK" +
                "ZBNSQHkgPUml5sZot2RbtklW0aL73spmWU6dnGIx+Djq4VMAJ1" +
                "ukp9XFqbtKLLVq/jCD9WwI4pNqjmweQgtaZvrxykLdod7ZZVtm" +
                "m89LKbllGmZxuPfAw6ulbACNTpKvVxaW3Sii5bvY4j/FgBO6bY" +
                "RE+eYTNqUkt9K+tme5vNt7alsM6qceEjO6Pa81nCSAS2iTdG5m" +
                "wQgTpRqeawVdnMBW7Wjm1HK1TM2nExkigW/ngsHnOuNi1bPBZN" +
                "R9O0SnOyMUZavU7ejDL+yG84OQajXQWiAmMR2sYjUlY10laB3J" +
                "11RNM6jk+lX0mXe9TlAPeHr0pwjwqqIb4hvoFa6tlmtnoz2ssW" +
                "tjamZCw+srdRW2lu/DVn619+L3pjZK1LEKgTlWoOW5XNzBbKy8" +
                "baPjbS1cE5YTW7PZvWm1lPf9XY37Np/MXon007aRjM+1Pvm7B/" +
                "29eS/FOpNE7B9SXqhYFRLpo4B/REdny5np001H7oFXkN3/mbAa" +
                "42H5TgehpUQ7Iz2Umt6cVmtvzcn5dVxmg89Y1xsZO/3tnGo/gj" +
                "l0FH1wqEAdEuzuXSeqUli60B9WgdelXz2rGgmjPJDLVJ8VcqND" +
                "Pz7Kissk3jqc9rWtjJX+9s41H8vsugo2sFwoBoF+dyab3SksXW" +
                "gHq0Dr2qee1Yood9qxNmkxnt+XF6Em28wjutmL+VpBly6I9EiM" +
                "9XPX/5jTbGSpyq96/FhZX1aQ9cEVt1Ij5vI+zIWAnMV2/+bFu2" +
                "lWaTGe15TU+hjVd45xVscWRHiM/Yay6esRLH56NZWZ/2wBWxVV" +
                "fGZ2yEHRkrgfnqzZ9t13vUfwHuD2dLcI8KqiG5mlyllja20Tg7" +
                "jas0Ywv3+fVUrTFCkMLZyudjl4HXtCqNsHW6uE5ZuCpMa2tAPZ" +
                "wHZmHHwFhQzWbSpJa21kp7nJ/7H+Iqzdji68lf75qzVdPPXU9e" +
                "K2oF8bDVWFuZm4WrwrS2BtTDeWAWdgyMJXqWOvcPhTjvPivBuT" +
                "90DfLbyaGxAPmcLkFNg2qobqluodb0YjNbfu5/J6uM0XjqG+Ni" +
                "J3+9s41H8Zcug46uFQgDol2cy6X1SksWWwPq0Tr0qua1Y0E1p6" +
                "pT1FaL/9NOMzPPfpRVtmk82mjHWZvnJ4kQf+Iy6OhagTAg2sW5" +
                "XFqbtGSxNaAerUOval47luipVKLFaJFa6lvvDRZpy4/Tn9mCGE" +
                "bIHO3sw/x6Jf4UvZG1eHdhIXCM7H6krNpqokWjwZcR+vjysiOJ" +
                "YpXt/72jCWY=");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value8 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value8[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value9 = null;

    protected static void value9Init()
    {
        try
        {
            final int rows = 57;
            final int cols = 85;
            final int compressedBytes = 2127;
            final int uncompressedBytes = 19381;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrdW1+IVGUUX5IIt52i0EgxygoDJUlXRU3IO3NnxdjaDTUtqJ" +
                "5KqKCo3KKS9s7ODs42D0b4UpCFUA/50INQLy4FYVRKvkRE9ZqK" +
                "RVKaUVDdmTPnnvP77vfN3J29d3eYO9zvzzm/c87vfPfe7977zU" +
                "xfX3wrnKR64nLte7sGt3IuCYo3/wOXprXdzDe3PzeHdLbS5tKW" +
                "sFxZKrAkKJcGm2P6cyIPa52abTHJBv+jjpluN/qr2+C3OsfUwa" +
                "G0LibJpzPKE1fyeVq5Kv1j6B/pm/dt7jmoMV2YQT6H0/JU6Z9/" +
                "DtY553ThNJVUs4z61TdZghhGSB/lbMP+TY22Rq+al0ZgG73bka" +
                "I12RROl4/aM0IbW15mJGGss41vQZlb+Wf6enLLOq/SZveYVg/N" +
                "0ve2LJmXv2uj/9GlmXw+yT2q/ENn9yh/2B/mFsrqe34fSbmvMd" +
                "oG9YLS/uNxyUIj7JaaA+JtSETbPEteOgMXUx3HPXqtsw3vTTdH" +
                "5+lbjrvDkqyvzsqNCXHXqfaicF+axGry5abF4jber2+pvcEx9g" +
                "W/wC2U1ffaGZJyX2OkLOdQLyjtPzpOh1lOFhpht9QcEG9DItrm" +
                "OeTwjpm1zW98rNyjp/vBRHBtfD4NwqMdhGdr7adgeUNya7jfEe" +
                "6VCLOmUQ7SmAYbwvbGcG/MzcEW5SsfFI0xfa8h3xHsDHaF9YPB" +
                "Hsed8qng6Wbr2UYZzoHBi8FLQS64xkAuCZY1W7eE++1KsypqrQ" +
                "cO7wb3NOVDzXp7cF8w0npMg0cssieh91zwQjiyI/4IlVQ3RnuE" +
                "+7VfWYIYRlA7PE8NOduwf7HW/m2RoyMOCOSJTLUP1Mc9s8Q7Ys" +
                "8IbRhlZ6Nz1azbzqfv9+Z8Wv4k0/l01B+lkmqWUb/2G0sQwwhq" +
                "h+epIWcb9i/W4dFfjtYYWfMSBPJEptoH6uOeWVLnYMsIbVhjZ6" +
                "NzxdEcWjbUnIt8jzUkq++1C6TlPmO5RWU5h3pBaf/RuX+U5WQx" +
                "tGx8R8TJs1lqDmynN0RSu+5pfLnJN+LwIfoWPq22uN73UMY+3O" +
                "tS4ZheTH9dylva6XyQ3rqUi0N261LBN9yq/R6+wy5oOzvlZrTC" +
                "sCCtObdzTy7LdLgVzhXOUUk1y6hfu8QSxDCC2uWcKWcb9m9qtD" +
                "VG1rwEgTyRqfaB+rhnllSP2zNCG9bY2eiMcDRbrfWFd7bb0r+j" +
                "V6fnfw3lwPn5W0PJP9abayiuvNJZQymeKZ6hkj4so3b+cdRSjy" +
                "W2muz1rn1yTNNSdMJKI0yecZwriziLeqnzivPhPDALMwbG0qPZ" +
                "5tpfkcG1/3kXXPtrsvRePFs8SyV9WEbt6qeopR5LbDXZ61375J" +
                "impeiElUaYPOM4VxZxFvVy8rjJ2oymmeq4OqKOpUfTz/k5Kqlu" +
                "PMnmuF/7kyWIYYT0Uc427F+sw+P0JVpj5OhpGhDIE5lqHyYr0z" +
                "NLDqyxZ4Q2trzMSD48R5p967W/MtG9YkbPp/5d83/tZ8vB6/f6" +
                "qaSaZc3+QZQwBtvlnPRFqv2LRdybjqxZmT4FLVjBaS1GFGvl5a" +
                "A7I2SvtRgDY2G2lvP0WPQedbk3v+OrrEqCmvg4xYjRe7T3Rm+O" +
                "abK8KinOEJXB6Dz9O/35NEWe6zq2XJsItb7DI5bzclRSzTLqV0" +
                "+hhDHY1n0tFf+CiHvTkTUr06egBSs4rcWIYi1eJr91Z4TstRZj" +
                "YCwYzQFvgEqqG5oB7tf+QQljsB3eowbED0ubUQa0B5FpC423I5" +
                "AnY6tnBKe1GFGslZf97ozUOOxHLcbAWJhtq+9OKht7dD4N0kNZ" +
                "7Ea9USqpZhn1TQljsK37Wir+BRH3piNrVqZPQQtWcFqLEcVaeR" +
                "l3Z6TGYRy1GCM+ShInWo1aFN1z9rRdtV49izvL/VaPjxr9vQnX" +
                "z9X3bMFN4Z5oddJ7rWlRaI0KdreM/UTwStKs1XvUA7157VfPJk" +
                "Kd6/AtbbG/mEqqWUZ9kSCGEdJHubbUPiWmWGNkzUsQyBOZah8m" +
                "K9MzS7xX7RmhDaPsbHSuOJrRCs+h4qF6SW1uGWtYEUYjNLLo/B" +
                "1g3dZm4bI0mdg9o0+OgXGwJL1X0taYgfS8Ujxf9sCfxDPezsir" +
                "l9LT+XaHfNv8XPtp5ZV8U/Pp7gzy6YLns+qlOX/neyhqPZzBmG" +
                "7qgnX+83MdsfR6lP9wN7/vz+K4Ds91RPne1Lt7lkcn/juUi+nx" +
                "nGjzH7XSBeeYJs4rtd+hyO/5/4rF+KMXxrRy71yPqVz7vbpV/5" +
                "u/az+DuXqF/29qvvZ2apkeh1Zbcaw4Jm3a7Ri9k1TuUTYrsSVt" +
                "EWK5LJvYMd1qxZqQwhB9SNnEIaMx+zhgrvFItjycxz9aTfF/ye" +
                "BJrQv+x5ZFXknHNItt6ooueD69M0vv+cH8IJVUs4z6poQx2NZ9" +
                "LRX/ggjPkWPoTUfWrEyfghas4LQWI4q1eDE52DLg7DGujmiOks" +
                "SZ/Xk6s2d+7+sueObPlMPWfVv3UUk1y6hvShiDbd3XUvEviDCf" +
                "L9CbjqxZmT4FLVjBaS1GFGvxYnKwZcDZY1wd0RwlidO4l+2qf6" +
                "RHO9xhdxFKtIygWpfYMiN4p0xdHM9YiWOz0V6Zn7aIM2O9dyqO" +
                "wMg4Epiv/tiznePn07en+tPyNdHxU+bUwiyvfX/an6aSapZRXy" +
                "SIYYT0Ua4ttc/GeXoSrTGy5iUI5IlMtQ+TlemZJXUOtozQxpaX" +
                "Gcmfjo9mfSvkdF3I8bsp9cubCuoeVLDcj+Iy9IgaLZu62rSp6w" +
                "tt73lNZptbs0CpKwvdLn3WOrfW2Uur1f+jEl7PM/t/1Fcdzxvp" +
                "/T/KwSHD/0dl+75/ogve909kOZ/2/Q90bQ7o");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value9 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value9[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value10 = null;

    protected static void value10Init()
    {
        try
        {
            final int rows = 57;
            final int cols = 85;
            final int compressedBytes = 1430;
            final int uncompressedBytes = 19381;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrdW0tvHEUQ3hO3cMgJKSckMAYpCAdkYYS87d0ZWUi5IPEHkI" +
                "g4wI+AYdlEw4EbUXAMUbgQLOBCwJHWsdd2vJaNxAGClBM3uICE" +
                "BFwJnpl0uvq1XV3T82B7pRpvVfVXX5Wrp3t27U5HH+8OOpWNZC" +
                "7aC4b1JnVmOA7YUWlN16L9TuOjfg68pv2F/kKnk54O26cZZtN9" +
                "auOQjNvWpyuXMTUNxXLlMr2mGKZhx+DzB7/Npf7SybsrYdEzzD" +
                "AjfTQ0hzr6NP5Yi3qBgihwwvWpe+2/87ibT/17VHw1DKLAqbOm" +
                "GD4N1HRNy+Mvr6xXVZzk73A83/veEf1Pa03X0Bn0gtd0PVCfrr" +
                "esT9fr7tPkgyrRe783fz6tn4NvTQenGqnLL1XPTZ+g4rMRG3EJ" +
                "NT4IeIsvtn1cvOBCstvTJ/F8/wfPpnPpU83fTy++MVvP+70bwf" +
                "b9f8lr/0aV9YtG0aiQxZXrivfZz727hR76cA/xXtYXMwW+mH2S" +
                "z115thwZ8hIeMk/uXyBBDJWVisw12UxTRgKRs1DzUiNFI72a9f" +
                "dp+nTzfZrOz9Tan4uPW3A+PW57Tf3OUsPPmj+fpgvV4icvJS+f" +
                "yGeSvqhp8rwXwjnXsynQLA4/IjN9RXn/rMOfWWtqYZy8IPXKD/" +
                "Rn0/58IeUr/2l4JZNCp0r9CrH0WP15MwcRvXi5GJvZQg4qJzVj" +
                "iGPjJFvVvPl8W7b13E/bMeIvw3n5rv20G37tl7hLBVv7l36tfO" +
                "0/Vkj5CjXcR/gKqV/lmWosXadGL14uxma2kIPKSc1Yziyv6Vjl" +
                "JPNQ8+bzbdmCnfnT2Vjhw0+sqxr1HBWXeNrqv1ZI+Qo13Ef4Cq" +
                "lf5ZlqLF2nRi9eLsZmtpCDyknNWM7MzEm2qnnz+WosdsSOuIQa" +
                "j8+ljvAWX+xpaC4kuz19ETMjXXLHaMf5tA17VPwVZo/KvGh7FL" +
                "vH7nEJNT4IeIsf8vS5Liy7Pf4aMyPzovCN78f3C1lcua54LzSy" +
                "D/cQ72U9nAkxRUwxW44MeQkPmafMFGKorFTkh34b5oyUORt6Xm" +
                "okwRhmO23t916dzbVvy0te+7km0Pem1Z6l0l6npmE/S106O1uf" +
                "oYTr08Fp8mcoUdV92tsupHyFGu4jfIXUr/JMNZauU6MXLxdjM1" +
                "vIQeWkZixnZuYkW9W8+Xw1Fttkm1xCjcd+rHmzL+y+fth+cbF2" +
                "G79Q3NgtdotLqPFB0DQb06OFYk612/iF4lbF/TRetfou2m3ezO" +
                "ln/lXUmZ/MlN1hd7iEGh8EvMUXmxbXZe99iOdLYnbADriEmjz2" +
                "NRyCxvmazQLjlK7pAdVuy8vEl8Rsm21zCTXZWPlDO4W8bUKwYz" +
                "849XyrY+f6b0rVdJtq1/OyzXDFsOAcskMuoSaP/RsOAW+BcUr3" +
                "6SHVbsvLxJfEbJftcgk1eex/cAh4C4xTuqa7VLstLxPf1uz7Xf" +
                "tzVNwNVdMS+34Xte93yZ/17bAdLqEmv5e/hUPAW2Cc0n26Q7Xb" +
                "8jLxbU2fLk/p0+UW9Okyqk+XyX26z/a5hJp8l9/EIeAtME7pPt" +
                "2n2tPv8HxJzLbYFpdQk6+R53AIeAuMU7qmW1S7LS8TXxKzMRtz" +
                "CTV57HM4BLwFxild0zHVbsvLxJfEbI/tcQk1+RqZ4BDwFhindE" +
                "33qPbBJp4vidltdptLqPFBwFt8sWlxXfaVM5gZmReNLZuwCZdQ" +
                "k6M+gkPAW2Cc0jWdUO3WPp34xsCO8t9HxedptrBjyt/2nC+bhe" +
                "O3ecyOuYSa/F7+Og4Bb4Fx/Jli47rstrxMfNty5n9oMZz5o5/J" +
                "Z/xg30XbOIT6jq/Kmhp8F6Ofmn+OsnHQa0pcUTfZTS6hJt+j3s" +
                "ch+EYLxZxqt+UVjlv4PWraiH5sfo+qjwO1ps38X3SZmg6vVxr4" +
                "P2Ywd04=");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value10 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value10[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value11 = null;

    protected static void value11Init()
    {
        try
        {
            final int rows = 57;
            final int cols = 85;
            final int compressedBytes = 2129;
            final int uncompressedBytes = 19381;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrdW0uIHGUQHgQFkfVgBHPy6MGD4IMNMR52u6dNgswK5pBjDq" +
                "4sKIJByGzwlXR6Z2NH8KBI3BwkhBy8eFoSXBAMiIewUeJNDDlI" +
                "hIAPhHjcoNNdW1P11d//THdPz06wh/0fVV999dU/Pf2a2VaLt/" +
                "AN3yhrxYb2ot6OcSv2YGz2ao3YWINkR53DGIoUF6Gs39bt45m7" +
                "NneNW23JtvSPVolNIkd7dJ6qm40cxeT3n369vN5aSjfnNrnVli" +
                "oM5T1VuYexjWLy++efLhORoeqpjZ+PX+i3T8YhW04m8bOVGJ7x" +
                "evY7ltn0tbprGh8086dG4Od8ntVFT8RzjiWopzU8Qi322sKYVi" +
                "uZ1RE6Xvo+ag8i3GyuVbLTa5TiYrUSZxVoRq3Yn8nVYevmeJtr" +
                "kvtpAXY2XWo1tO3EflrzKHVp7hK32pIfUV6y6PRuEcOoHL2DLn" +
                "du3z+e8rp+t66ylZRUdnnuMrfakueeK8dQNVtTyuv6fXU1py18" +
                "nFrsedS7lLVis63bay43l2uz2ek1SnGxWq3BarIVax6fJvTauj" +
                "neV61sJxMeBV/3P+2vVn+Xsrg6vmpb8kDdSJ+G+EpDR/oh56jo" +
                "bPPXUr3Ppn8t5asLz1HJj/WvpZwzyneD485t5xx1tAxD+rbZoz" +
                "zH/mS9NZXNrWtb91uTytj7vuJncKbS5+5Ga+pbOQ3jK53/1x0V" +
                "eWXGtqI1LeIY5rH8/ngbh7qHV2F9w7P4VEi0j2en702Dm80cT4" +
                "Ob9Y+nPg3N3JsGF4OL1FLPNppbC2NwnMzIXKyaXyL6o1vIpjNr" +
                "VZZT0MyjcdqLGSVasdzyVyRq0vfRizncVeLoaCPaoJb6/Ky4wX" +
                "OxIIYRNE5mrF1Has48722MxsyDMzMgUCeNgtuueu0Xr1WTxRZX" +
                "hDHsKVaja9WqR12fTmJL4+mfo3z3+w2d429UO572fhn7+vTn6V" +
                "+fzh8pczzNUHWOp9FWtEUt9WyjubUwpniuI6lNdgsPj8ITGI2Z" +
                "RRUyufyIc1VZfbpaqwErYPY0ibaSx9y68GV1e96tR4fs17+O+8" +
                "l48f7pf/Z3XsOw42l4vtRTmfM+dPxEOYZSx4LaT2LLV1FP7Y5/" +
                "d/JxU8fT3m91j6eT/e6kfb19nVrq2UZzsbA1/FLGEiN/jKJ5ht" +
                "acmYVsEo2ZtS5BoE5UqjmsKsvMFl2FVo4xGQrrtTq4OrOaS+0l" +
                "aqnPPUs8Fwtbk1kZS4z8baP20Dz8Qnh4RDaJxswDdYBAnahUc1" +
                "hVlpktmQbMbbOxUo10dXBNWrXnPPRn63++9X4vhfqrHnv7WPsY" +
                "tdSzjeZiQQwjaJzMWLuO1Jz5u/8uRmNmrUsQqBOVag70u8xsyT" +
                "QUVYQx6Rm3LptJFCv+5fYytdTnnmWei4Wt4QUZS4z8MYrmGVpz" +
                "5mt6AaMx80AdIFAnKtUcVpVlZouuQivHmAyF9VodXJNW3W+77S" +
                "611OeeLs/FghhG0Li/nxq7jtScudIuRmPmgTpAoE5UqjnQ7zKz" +
                "JewWV4Qx7ClWo2vVqj1XFPAsP9j0z7xPuzZ96GAz2GzqqFifyR" +
                "fp1lonR9AJOtRSzzaaWwtjcKzn2ir8gujvp+8hm86sVVlOQQtW" +
                "cNqLGSVaWKyGogq4esyrM9pVkjwj99Or/pn3nbrqQwdXyzFUyd" +
                "JcpFtrnRztxfYitdSzjeZiQQwjZI52Hak587PpWYzGzFqXIFAn" +
                "KtUcVpVlZsvpr4orwpiiumym9qK7mlXv9+M7le4mzb3pShT/k6" +
                "41tZ+eGnGsi//2Rn5auoKS96Yr8yuDe/voUHSI2qwXG8/FyzaN" +
                "Rxv94Ux4BqNdLoPOrhUIA6JdnMultUm7jdvlqtd5hB9XwObUqy" +
                "T8he/NO5O8h4kemv591M5rmPCaPngPrOlENUTr0Tq11LMte4Wd" +
                "sBOtJ5+LJbNZPM6pJVT/uqlDFkEQJ79sZq3KcgqasemG4LQXM0" +
                "o06tNxjI6voBqsF3NgLr2ag33zFTVe2H4mOx/O95V/A89p5717" +
                "9/aRJO4IykUTZzMb/wYtfrnyM2mPBvc3aHXUBnuDvdRSzzaaWw" +
                "tjcKzn2ir8guhfb6whm86sVVlOQQtWcNqLGSVaWKyGogq4esyr" +
                "M9pVkjz+/bT0/jJj99Oh72Lj30VX30+DiX4f3j7ePk4t9WyjuV" +
                "gQwwiZo11Has68npMYjZm1LkGgTlSqOawqy8yWTENRRRhTVJfN" +
                "JIqFPzgXnKOW+rzqc4P5MloYg2M911aOFIZ8tIxsOvNgPzII1C" +
                "lYwWkvZpRoxbLsrwjVay/mwFy62mAtWKOW+tyzNph30cIYHOu5" +
                "tnKkMOSjLrLpzIO1MgjUKVjBaS9mlGjF0i2uKP0W1aAXc2AuXW" +
                "24FTrf8pON7NKKDe3Wz4zcWv70B7YzOiz8nYHYtQaJ8yG1RqtX" +
                "vjdF7mE6dJ5RNj9HfAa+ldls9gge3GmK6fTupjU083v+4EBwgF" +
                "rq2UZza2EMjvVcW4VfEP3RXWTTmbUqyylowQpOezGjRCuWu/6K" +
                "UL32Yg53lSRPtd9MrD489m8mrjf1m4kx/o/vzTK/mejdae5/JP" +
                "CzH97X7Ge/Ob76TL7IZrQlHyYrju3E5K6Hk1PpT41x9epGrk70" +
                "KdFk1jSK/Gvq9zW9pquPVNdXHVW8pslHyQfNrmmS+te03dupNU" +
                "3e9949ltKQnLmX1jTaN2Q/3Tf9NS2noa7SqEMvtI29pp1h+Rp7" +
                "9tupr6IJfm/cAr3QNna1C8PyNbamC/VVNMFf/1qq7LM+YPhkyJ" +
                "Ow1el/dzJZDcHh4DC11LON5tbCGBzrubYKvyD69RxFNp1Zq7Kc" +
                "ghas4LQXM0q0sFgNRRVw9ZhXZ7SrNMjzH8xk2xU=");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value11 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value11[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value12 = null;

    protected static void value12Init()
    {
        try
        {
            final int rows = 57;
            final int cols = 85;
            final int compressedBytes = 1703;
            final int uncompressedBytes = 19381;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrdXM9rXFUUHlQEF9FCEVwVI6XG1kq1JY1ScObOG4s7oSCICk" +
                "ILdl8EF6VJXyZ9bWbRhVKpCllGEP0DhCxcSBSEgpsqXWXTTQIl" +
                "WrfizJycOee77975ee+8l7ySe9895zvf+c7NfW/eu0laqeSPtF" +
                "WZ8Ei/8PtWH1QKP26enHbG68143Ok3SbB60rVxI5Ooc5qdzV5O" +
                "z2VH0+NpPTuzZ3slPT0KR3PGyz6fs5xIXg+mfW6A/w3vnA7UkJ" +
                "3a60+HXqe1t23feLeFG+967OeLufbzde3dl/4LdAW9lZ5rt+11" +
                "KnM62jpNveshzc1ZOt8Y+36aWt+Z9LUB+KrP49OQnoHr717bYv" +
                "bD/TQg19j309pH4VDDHWYlfxaeu0iu8HUB+7JZppZ6ttHYtjDG" +
                "PdaREq8ZJCdGSB43AnUyvvWE4PKqbH262trH/SpgHR0U1qtz5G" +
                "dJ8rTPNqnFns+4Ray0+V5z+Tz+CP7XdyVsojqtFrntcV6xW49o" +
                "sfPqaI63c8V45s9qlVIftQ/DoRzf8TWzRi31bKOxbWEMnuuxtg" +
                "q/IPJsOrNWZXMKWrCC017MKNHC0nrSX5GoaT2FXsyRnyXJ43iG" +
                "/65ywI/aJ+FQ7qO6rvvqenYex+zXWFe8jxE92vbOITum43flcP" +
                "Ejzh0lVl8V/asbnCOfwVwwF6ilnm00ti2MwXM91lbhF0T7inoO" +
                "2XTm3jVtIVCnYAWnvZhRooUlu+SvCNVrL+bIz5LkcXxGrce87h" +
                "qbxV/7sTX4303rC/WF9ro6BJ/oz076btrhLPrd1KcB302z9/bH" +
                "u2l7jfwaimnl+gD/YnwN5dhDaR0ufp1ml4ZZp12LiT+nk1/7rR" +
                "f2y5ze+Ge8OZ3+nnTjt1BX2Ph70oM1hN6TTr+H55r7gXdt7hfP" +
                "5IsMW2vjSuOKnNOXG6O/GK0j/fzkNbs6ly+SxpLHzSxMhBSFyC" +
                "Et+c2uVoQV2PW46qVYVx2D76dmqLfU0e6nE1w/we6nvrrifUZl" +
                "H0T92ckvxc+pT0OYOTVXzVVqqWcbjTvnrZfEwhg812NtFX5BtM" +
                "8eIZvOrFXZnIIWrOC0FzNKtGJ55K6oNYtq0Is5MBdWO/m7qf9z" +
                "37lGfi/Bu2lUDcm15Bq11LONxmJBDCNkjHYdqTm79dzDaMysdQ" +
                "kCdaJSzWGrspnZ0tHgqghjXHXZmUSx4l9KlqilvutZ4rFYEMMI" +
                "GaNdR2rO7pw+xmjM3FMHCNSJSjWHrcpmZktHg6sijHHVZWcSxY" +
                "p/MVmklvquZ7E33mULYhhB580Z284xzC/Rmt+VuacOEKgTlWoO" +
                "9OeZe7hdd0UYwx63Gl2rVl2p1HfqO9RS39212eFx8jdbEMMIGa" +
                "OdY5jf9uhozNzbOQIE6kSlmsNWZTOzherKV4QxrrrsTKJY8W/X" +
                "t6mlvuvZ5nHymC2IYQSdN2dsO8cwv+3R0Zi5pw4QqBOVag7055" +
                "nZQnXlK8IY9rjV6Iq06kF7fcm/B/PnUdOvq/4nn7Ve9XmGix/e" +
                "E7cO+7j5MK6ifu+mtfcP5rupr65Q76bT3ucvw7E61M9vWidjrN" +
                "MY7/tmrgR7KHOD1mnz4fT2+QPM6VaYOTVbE8zp1jDXfvbpvpnT" +
                "2RKs09lp7/XFndP658XPqU+DtU4/K+OcOrDz5kiwJ5bx1+mRYd" +
                "fpuMfyCRidkt+VzPb1E8CS9y6zfHzaWkb9/dP8/mnaZ3/SHC1+" +
                "tuNqMFVTpZZ6ttHYtjAGz/VYW4VfEO2zY8imM2tVNqegBSs47c" +
                "WMEq1YjvkrQvXaiznysyR5Jl+nDoZ+6/TFEqzTqBqSNEmppZ5t" +
                "NBYLYhghY7Q3appLOLt70jWMxsxalyBQJyrVHLYqm5ktHQ2uij" +
                "CGUW41epZwNqf/btpaKH6drj4T+RNppGeplT+GeZYyX5Z7D4X1" +
                "xXnmT24nt6mlnm00FgtiGCFjtJu7mks4JadEY2atSxCoE5VqDl" +
                "uVzcwWc9ddEcbk67J1SE16Num4dTjstV8t+V9ZTEPfrecDz+mD" +
                "ks/p1PVNPqfmq3LPaVx95rK5TC31bKOxbWEMnusxtdUfKY4tgs" +
                "iz6cxalc0paMEKTnsxo0SjPl9FWg16MUd+liRPjN+TNndK/rl/" +
                "Z6h9qVL9jUT1h5LfT6Pqy7512Hr/b4HZaD+jvzki49cU510jG6" +
                "G0N58e+463EXNOGxcbF6mlnm00Nj+hhTF4rsfUchxbBEGcmk1n" +
                "1qpsTkELVnDaixklGvX5KtJq0Is5MBdW61hp8Le9nd8/Dfqp+F" +
                "cJPvc9GtKfg9D/DxulTVw=");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value12 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value12[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value13 = null;

    protected static void value13Init()
    {
        try
        {
            final int rows = 57;
            final int cols = 85;
            final int compressedBytes = 1178;
            final int uncompressedBytes = 19381;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtW89r1EAUThF6K4J4EcEuHioIerBLDyLFJGbFlv4LXhQFq1" +
                "j8UZBKpXGprcGr4KlUBQ+9VPwLerKlUlu8iAdBDy2FIoIee7Dr" +
                "9O17b5Jps7uTTHbolCaZN99773vfziTZ3WwwE8w4TjAT7O4dB3" +
                "pwHJ1EC2D4Me1TK8TCmLUj7yuPRjNDkxF0C3E4jo7yjOiNUWQO" +
                "gA4XOBs+ynPwXLzaeAu/wZE36zTVmvXLq+XPL4ycNmjeETO+rW" +
                "rq/nKsbPnX1fo8jbxiazp92OA83bR0nm4a1HTbUk2322/t73le" +
                "GDCv6bPrRde02tXQNXdd03V/3Yxvc22imp2mYY+3oUnTjfBm87" +
                "52zVP/ifm1nz+HjDV9WABNH1qm6WgBNB01q6m3qu4pz1erKrS3" +
                "mi5CI1n0ecZr1cVW0nRF3VNyXlGhvZV0ERrJos8zXqsutpKmy+" +
                "qekvOyCu0tp4vQSBZ9nuGCjNLFNuN7/mHz59OpM+bOp9EdO9+b" +
                "RrcNanrXTk2nH5nTtHLcTk3zrws1DSxd+/nXRTQdtlTTYXOaRv" +
                "ctvUbda//zafTg4HxaXyMjlq79EYOaWnovlXVd4fnwws72dOiD" +
                "ZaIa9u7mvpUqwjnlyOWYpa8Fplek/tl98BeVmirqCssxi6d7nm" +
                "ZyfRgzP0/NfR/lD/qDOxo8pmM1y/4NUHG0iKmnVTub9UzPQRfb" +
                "uqaeH5v5fqq1AKgkf78Az1Ok5+C3xdovQnO/60Ola3t9bxr+ae" +
                "jViV+j/urj+fTTPtl/K9VaS6XpWrtcowoxT7/oQx1ouqtWSR/q" +
                "QNNdtU7oQxVBU/dYATTNnUPG9/yT5jWdnjOrqd+pN7q+eM1HUn" +
                "nq4RZsBVtiK/ZgE/1LV8HCMfy42oXHgObx0SMejWbmfojgPBEr" +
                "x+DjvCZqiV6oK+Ls6b+sBedCq02Yp0fhyLshj03+bPm99qL5tR" +
                "+vq73Pp4W47o/pQ6XU9Ef9ejJrp6aTmT6rHcwH82Ir9mATfdkC" +
                "GH5M+9SK8RERj0YzU1ZyTEQjFnF0lGdEb4zi9qkrQjY1FB3lOe" +
                "IqYZ695ql7Ktv5Er02tPYzrcsb98bFVuzBJvrRW24BDD+mfWrF" +
                "+IiIR6OZKSs5JqIRizg6yjOiN0aJ3qgr4uzpKM/Bc/FqE+bpu/" +
                "rr2WPpNSrTuirdlW6xFXuwiT5aOAYQ2Od26kljYk705pkpL0Rw" +
                "npwpjSGzkiODxe1Nroj7ACqZDa1VUrNUKYmt2P8fKUHfHQALxw" +
                "AC+9wOPhAfvWn8pMx1dgzBeXKmNIbMSo4MFlFXvCLuk1SXnKnC" +
                "PruS+9jwM2m3X/u9bzE+k+7P+2xDNB2yVNMhc5pm8B6tEJpGh1" +
                "Ld6c3lo2nr30e5iwWYp6k5tMf3Ue5SAe6lluxa+9F78/M02+dQ" +
                "go6gQ2zFHmy1P7/X7w06nl8TdooBBPa5XUTB+OjtOLWY1Jtnpr" +
                "wQwXlypjSGzEqODJYah6SKwgXqk1SXnAkZ02rV89Qv+2XHqb7S" +
                "+yr6ZW2fFnzQzYH/PqqF1bjHc33Rx1QRGnqub/Jl00y1Pdc3pf" +
                "j9Pn+ur/q5yWvUPxJa7gU=");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value13 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value13[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value14 = null;

    protected static void value14Init()
    {
        try
        {
            final int rows = 57;
            final int cols = 85;
            final int compressedBytes = 1632;
            final int uncompressedBytes = 19381;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtXM+LHFUQblzWHHQUoqCX4CkBAwaMssKukfTrmQmBHCSQBY" +
                "WcEvCUQ4gnD4K9k51kZ/6B3EIuwrp4MSReDHjzEN2DC4JBPEUk" +
                "HkyIKMlBnd6amqp6P3pf97ze7gzp4Ov36n311Vc11TPTvYOduc" +
                "5cFHXmOuNzFOEK5zhyDCJoLe3ck3Nms8F16S0jR+NDIvioK+Uc" +
                "uiqdGS39s/aMpI8tLz0SKebZmkc6iCo8Bl9HtR/9s7sd8bNeMX" +
                "yv5Y9ND4RSqX5NPyrv6/Xqf1NfTYsc6QG1HoyrfE3Xm96nxWra" +
                "+b7+mobT0Ixrf+3V+mvafjBbfRqQq3yfrodDPa3p+NPnu9nqU7" +
                "W/AZ9R+2espvsaUNN91VYwXUzfHY0H04Rqmr41jn3Li+GwoXns" +
                "lx4zsAt+nNY4x7X1oR3wR501dWhI3zYsqoo+HdwK3Kd3GtCnd5" +
                "p+7ddzHzXVZ9QP9dZU9mn6sFDW5rX/l/o5lM6V2ztEv+/sU28N" +
                "4a99da3ktXWt2X1aNq8ptE79XCru74bOwe1m67P3afxnGMbeDX" +
                "uf9q7X06eh8ipV00czWtNHNdb0n9m8Nw2fV4Ga3iv5Tvdjw/v0" +
                "Xn01Lf3psdXsPr38YpX1U+fVeRjhjDZY6xbEyDlfcyvxEyKKkl" +
                "8kG4/MVemchCYs4fiujEjexKJrsGUw6oyf5K6MYVaJ4lhe/8+r" +
                "u48axb0bqBvu1uNbMuJX5qxYTfP8kjSq/ahWg9pQGzDCGW2w1i" +
                "2IkfNei9Zk5fzkMcrnY8nGI3NVOiehCUs4visjkjex6Bo4Wqrn" +
                "uzKGWSWKY6nzl+as4CuV4zf4r/4+3f2/78t7U7XpXjlruulCq0" +
                "0/hiJRwnmauYZSq26as7IMzTziT8OhPCtyw5yVZWjmcfl3H9Tw" +
                "lXLsnZOdkzBmZ7LBWr1Ju2jjeGmD/+QKeXkEk4FH5wqIQaJNnM" +
                "nFtdEIFp6XqYfr4LucV4/Fq9k+0z4DI5yzHVhl88G/aJEYRNBa" +
                "2tEH+cmb89siYzYSIXVKpZxDV6Uzo2Xtpj0j6WPLS49Einm27n" +
                "vTpJ20o2hNoDKLx/e/tgsNnIGedj1b+vupQ0P6rS8y953vnDoH" +
                "I5zRlv1LTiQnpCWz6Xi5hhH9sjNnyCxg4x4UmavSOQlNWMLxXR" +
                "mRvKU+7mdmgPw8XxlDxuLVzO3TbtIddcMV8bp1vfqg60IDZ5hj" +
                "+EzpPu1692kgtSt7J6pfimbyGO6t9N70groAI5zRBuv4E2lBjJ" +
                "zzNbcSPyFMNh6Zq9I5CU1YwvFdGZG8iSXLy5WRVM93ZQwZS2ab" +
                "26cvP+3TUneiOb/tiY8bd8ovWBgOO7nHf99fnfCkC3x/9Vghpc" +
                "F+22PmNfYQv+3pv1/27/u5NT0yozU94lPT1YfT/mZCPec3o5W0" +
                "yX0bPm+u493c0psjYW1ntOeaH8Wlgrx34lGWWeSwKScujyNvR+" +
                "f3bQ+l6VYeCmhPeXPbvV08edf+8B2d72K/zLXPLAtTvPMHu/Yv" +
                "/eZz7V9cnfraf95vRitpk/s2fN5cx7u5pTdHwtrOaM81P4pLBX" +
                "m7ePL61LN/nrg+jT/06dMMVcHn/gezee2beYW89uOr8VUY4Yw2" +
                "WOsWxMh5r0X7ZDX5uY2wPDJXxRF2Ho7juyYzZQOr4evujKR6vq" +
                "vH0KtEcSyv1hfj5weLyaL+rG/6I+Os+3BpMJ+hhDoujZ+cJEvJ" +
                "kv5cKkA+S8HuMA+G1lBdTVdeK/hsuBU9YcfwjSrZO3s6e2CEM9" +
                "pgTRaJQQTMey3dzj05J8UkbxmZ6yKE1CmVcg65bzKjZXjInpH0" +
                "wR27Gp6rrGY8H8/DCOftd9p5XGdztUUWxMg53+dW5JKMaktiee" +
                "TJZ4+GsPFIJpsKqZer0TUgGuxcvcxPxuBV4tm2T7VPwQjnbAdW" +
                "OMeRYxBBa2nnnpwTIktvGRmrKhFSp1TKOXRVOjNaOhv2jKQPou" +
                "xqeK5c9Whcbi/DCOftnWVcx/fRIjGIoLW0ow/ykzfnt0WeqBMI" +
                "qVMq5Ry6Kp0ZLZCXmZH0seWlRyLFPNvc7/yPZ/Q+6rHPd/5tS/" +
                "j7KOP/HTB8r0xN2W/PRU2L/fY8YE0fVFtTy3epCXP8RzSTR7V5" +
                "qdPqNIxwRhus47+lBTFyztfcSvyEMNl4ZK5K5yQ0YQnHd2VE8i" +
                "aWLC9XRlI935UxZCyR7f+rLY/R");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value14 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value14[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value15 = null;

    protected static void value15Init()
    {
        try
        {
            final int rows = 57;
            final int cols = 85;
            final int compressedBytes = 2586;
            final int uncompressedBytes = 19381;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrdWz1vHNcVXYOkgBSkgDTqLQYSXKRIzEgIXOzuzEgxEEsikI" +
                "JqEgRiY8EJkBRBaDAxRK1Fyp7CZT6QNKkS56sw0vsHSIBcpIw7" +
                "CWJlRE2sKpy5e+ecc98bfmlpy5rFvDf3vHPPPfeJ2o/hcnxjfG" +
                "MwGN8YT+fBwCO/ZsQ5es0xo54Jheaq/r6qcWU/IoNH11Eer2pF" +
                "ZENle72/I3XPq1oj3SXUSY/br0p/9/qjvsNZKXt873AKR6kyu8" +
                "y01+PUKN8s37TRZscsBqIcZyBWnDNZs/05/YFma2X2BYb6VKes" +
                "EV1FZUe21/MdaU6ur1gJjkn/ZnnTRpvblZse12uOKMcZiBX3HN" +
                "dHNuvnKnfuhKE+1SlrRFdR2ZG7G/mONCfXV6wEx9C/9PKll220" +
                "uVmxqLmurzuiHGcgVtxzXB/ZrJ+r7O6UoT7VKWtEV1HZkfd/nu" +
                "9Ic3J9xUpwDP3qanXVxma2w6IWf8tQ5qTXevoILWg6xgpQ4oM8" +
                "hDE6jVpcJ3rqeG/1d6TueTXuhXrhbre+u/Xa3vjKVuErtyZb35" +
                "5yfnaY5+Stb/WuXE6Q7xz/NWnr9RB/8wD+sG+lr6+tVxNkPJvX" +
                "09tT78WV4spg8J68O2iQgw9npWzTnM0xOXXczD4PWx8flnnsPa" +
                "2KKqlRHcpz1cfOaX7xx+E9HMdtea48Z6PNjjWP4lpxrTw3+a3h" +
                "juEaOTidZXHDZs0GMQzZWpl9gaE+nV//KGpEV1HZEe6CnW99zD" +
                "mRmfrw7nQ3938+HW/M/vn0cJon+3za52E2z6flcrmcxwzHCAyI" +
                "x7rOiql+/WPHXUNV00z2oPwcU9k55cHg7oeqHX307dVB2LSn8+" +
                "V5G21uV8573FwXLxnOHGcgVtwyXQua7TPUS5qtlTt3wlCfzjcl" +
                "1oiuorIjTWauIyi6i9hXrATH0K82q00bm3n6TmMTcbVZdLFzwV" +
                "fMTo2g41dFRoGrswMoKHv6+pE4U+fwhtGQ6MGvi4E60VXWjbXg" +
                "u+d1f8GvdorBC3mMfjg7Vubd79vV2zY2MzCLR+ex6hjzbZ4sAr" +
                "d8Ph1DhVSBq7MDKCg75aVa7BejIdxX6od98CrrxlqymxvVho1V" +
                "91poUROPvoFVx5ivmJ0auS5XSBW4OjuAgrJTXqrF3jAawn2lft" +
                "gHr7JurAU/g8Hw6fCpjTY3KxY116PvKeIcveZ1Rj2TFVM1ruxH" +
                "ZOR10rp9yp7jSNNXX0fqXvvTGvCi3ZZz5Vzy/qDFmnN02VY9Lu" +
                "fqnzgHI69HRYvuvJ6rUc7duWxz9p3LHKtDO+UrE6vM1Bzvizvo" +
                "88F1DsJcY7/3/KPXXsx7KH19zeg9/6nylI02O2Zx/VNHlOMMxI" +
                "p7jusjm/VzldkXGOpTnbJGdBWVHdl5mO9Ic3J9xUrlqcxuLpQL" +
                "Ntrcrix4DEQ5zkCsOGeyZvs56pearZU7d8JQn+qUNaKrqOzI9n" +
                "q+I83J9RUrwTH0q+vVdRubefrqdR0xVh1jvmJ2agQdv6o3UwWu" +
                "zg6goOyUl2qxN4yGbK+n7rkO9HUHYk3eJdJfq9ZsbObpyhriam" +
                "38b4+dC75idmoEHb9q9KICV2cHUFD29E5I4kydwxtGQ6IHvzZF" +
                "9sGrrBtrwXf3nmS5eSCyU97dLBsLq86wmUe9ihXGnw0z9yIUcy" +
                "7qDLP3N6Dq/jhDV4ANl8efRUasrDuh/fIj322LnW0eiOxUhrGw" +
                "6ozh2cmiR6rRVyGupZhzUSeXw6rujzN0BdjwbP2ryIiVdSe0X3" +
                "70d9T7eX/1UL/DWPyqfd6vf32S6tXn1ec22sMxu67f0VWLHMnN" +
                "ls8na3rNmIk1uGJG9Jny+rpIXTTjzsPoOlZjp1yXK3It2c2n1V" +
                "Mb7dGuyDWvWuRIbkYOmNBsf0ZupZm+1u2V1NORudFZ2kXqohnf" +
                "+1p0Hau5vu6AVtRdgv7w4vCijc08fXa4iBirjjHf5skicOSACc" +
                "12T2+nClydHUBB2Skv1WK/GA2pt9SD+mEfugOxJu8S6V8YXrCx" +
                "macrFzwe/RerjjFfMTs1cl2ukCpwdXYABWWnvFSLvWE0hPtK/b" +
                "APXmXdWAt+en7/2vtdtuJQ33Ir7tXvHus3xPdm+1rRr3f39In+" +
                "pvuT4hMbbXbMYiDKcQZixTkzahafaraqsi9m6LWq55lYjW72xk" +
                "/zHWlOfSftK1aCY+42s8/3e/8F7h/mvVR/frv6n6OpP8PPy/2j" +
                "e5jNsd+9vkMqHOleX/3+l3+vb3v9JO/1FQ+KBzba7JjFzfV4ih" +
                "s2Jj5yCuEgZ0w6vjJ+oNmsYlz1kLKLoJ5nojfT427H2drFg7Hs" +
                "wzjpV/cLu6S7mTtuTU70M8wHX/7nqL6f09kc5RvlG36lWHNWvz" +
                "PUY+Zwjq6D5Vr1b3J1LQMOcplay9jMSpnKzimjL+4g1U099++e" +
                "7NxquWqjze3Kqj2Kq8VVRxw1xBmeg9NZFjds1mwQw5CtlTt3wl" +
                "Cf6pQ1oquo7Ah3wc41JzJTH94du94bV8qVZPdbzHCMwBRP11kx" +
                "1a9/77hl5BxoJnvI8ZXJHqNf/N9X7f18cJ2DMNModotdG21un2" +
                "l3PW6vFwxnjjMQK26ZrqWaxYJma+Xu2V4Y6rOrthA1oquo3PEW" +
                "ejpa4JxcX7ESHJP+o+KRjTa3K488bq/nDWeOMxArbpmupZrFvG" +
                "Zr5c6dMNRnV20+akRXUbnjzfd0NM85ub5iJTiGfrVb7dpoj/YZ" +
                "vLuu/6CrFjmSmy2fT9acvkIkmVhzBtfTkbnRWdpF6qIZ669H17" +
                "EaO+W6XJFrwc/e+Lh6bKM92pXuuv6jrlrkSG62fD5Zc1o5ycSa" +
                "M7iejsyNztIuUhfNuLenj1MG12GnXJcrci342XtWXSqXbLS5fa" +
                "Zd8ni04YhynIFYcc9xfWSzfq5y92wvDPWpTlkjuorKjlhfaUea" +
                "k+srVoJj0j9dnrbR5nbltMejS44oxxmIFfcc10c26+cqd+6EoT" +
                "7VKWtEV1HZEesr7Uhzcn3FSqXc4Yoxjp1fdO97/vRifv905+GJ" +
                "frt1abRko82OWRwR5+j1ZBExUNZHxt6/019UjSuzq6gJNrjg8a" +
                "pWRDZU6j/3d6TueVVrpLvUZa+OVm20uV1Z9TgiztFrjhn1TCi0" +
                "e/qhqnHlbq8CQ32CCx6vakVkQ2V7vb8jdc+rWiPdJdQ52r2+7a" +
                "Vnvtf3z6/Kvb7ta7P720i/L1WsFCvxbyNncKd4ZVZK7946YP2d" +
                "o3pI/zby+bknPdrp/zl9Bqez+/7pzknekz6ZPR3Xz/eeur/99/" +
                "TOkxn+XfRHR+Mf7bs99d+eg+/2/PWLrnjCe/qP52BP/36i70+v" +
                "jK7YaLNj0/iJIs7Ra44Zhf7kX85QtclHWpldRU3UARc8XmXP8C" +
                "fdPunvSN3zqtbQWtxteaY8Y6PN7SesMx6P/ueIcpyBWHHPcX1k" +
                "s36ucvcpTxjqU52yRnQVlR2xvtKONCfXV6wEx9Tt/wE0b6J4");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value15 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value15[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value16 = null;

    protected static void value16Init()
    {
        try
        {
            final int rows = 57;
            final int cols = 85;
            final int compressedBytes = 2122;
            final int uncompressedBytes = 19381;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrdW71vHFUQvwYaFCmiCESioUFKCiQCChJycZe9k53SLlKkig" +
                "QWShGiJEpPzqdgJUY0oUgCQvwNuEByihgKKJCQ+KjjxESAIz6F" +
                "CB+Shddz45n5zby3m/NebLJPNzNv3nz8fu927/Y2Tndfd1+r1d" +
                "3XHepWi2elvXCTPTaGI2Ru/ZxDHqnZaun6UedWy8ZwBR1tkeoa" +
                "iAors+fy2ZiRzYl4YSdBLPV7l3qXSJIuV2jGtvZwjLX1XHs5Uy" +
                "qU1sKntpruzAdGWJwSK3F61XaUbKny1mtpRha9XrU9/C5JH3/M" +
                "PcbW/DutBzg677Ue4rGdbuNF2p3oTpAkzb5yFJPFZHfi0qvkZ5" +
                "/YkiMvjqJ5Ga1rlh7ySbbtrHFJhMXJ8YPHsQaiwsrs0Sw08v6y" +
                "zsFIj4PZ2d1stfqv9Dd0/2C/2DpPP+u/OHw/e3Xel/6h5Mok6Y" +
                "tHtzyH9frFyQd5//tHYf58RXw7eZ4mePVfcp4jo5ypfk8vDHhP" +
                "u382s6fKc3j0a6q5PU3xampP/XFhQLqYKDZ2u7z2mzyKidaOHy" +
                "kM/eUmu/TO986LTa84Rr84enhFXYuyJFcyfBx6aC59fEbnmq5E" +
                "kYLQ1hBJ65zra1tsKb6UG/GovvY7f2D0/IePwrXveUXX/vwH27" +
                "32O4veilZlFsXlauRWsH6udiqytPIscC3fJYVCsqvqdD7yVrQq" +
                "syguVyO3gvVztVORpZVngWv5LikUkl2vzua34k1vNXY/fHM31G" +
                "qeV2XH4bdf94vu8rhq72yt5nlVdvzEW83X3slazfPKHXPfy/1p" +
                "+M04632DPYla94b69pbnh/5zC18Hkaub8m6w8vOmDL+n+68nWa" +
                "yV8s1nh7M7Q/3jpvy11Vr4CuK/C2r8VnvPftp4/TL3e517/rpH" +
                "ak/Dfbi+8G1j7//6qJkL34z1Ol/qLpEkzT6ai8fGcATZgz3o15" +
                "m65uZvmBWbbTtrXBJhcVqkuoZd95XZU2KIGNkcXonRaK52N7O/" +
                "TaeLafxtWnpq/PabTkVTzWaOweMj/zadrvvbdFS0vY9JWs0WSx" +
                "sr0mtdy/fyPuxOowqxRqfRIis794gjv1REj83m/BTb4DydKqbc" +
                "+zZV6zyYSkVHNXfgGcpU85GQt5+k1WwNrpZSfCi91rV8L+/D7j" +
                "SqEBf75xc9Wo0BMSFjrpO69j0O5M35nm1xgKTV2sMxEivSa5sJ" +
                "u3HA+7A7jeyeHkDUbGkMiAkZW2YxJruKvDnfs23fIWm19nCMxI" +
                "r02mbaQ6rH/lLTyO2pIEK0GgNiQsaWWYzJriJvzvds24skrdYe" +
                "jpFYkV7bTMC52A6f4ejuNLJ7uoio2dIYEBMytsxiTHYVeXO+Z9" +
                "teIWm19nCMxIr02mYCzhXvw+40snu6gqjZ0hgQEzK2zGJMdhV5" +
                "c75n214labX2cIzEivTaZgLOVe/D7jSye7qKqNnSGBATMrbMYk" +
                "x2FXlzPvbKPufv1vq9+f97zp/g1dy/8RXPkLRaezhGYkV6bTOx" +
                "l/dhdxpViGO0GgNiQsaWWYzJriJvzk+yPZWySik+64802qk+KW" +
                "9xqhyVZ8IpjpWsmEW6W76LR4G8q+oU5+pZMmMfah8f90l5B4eK" +
                "c+l8xGAxphDH3fJdPIrBCzY7Xad9m6TV2sMxEivSa5uJvbwPu9" +
                "PIfkfdRtRsaQyICRlbZjEmu4q8OR97Zf8O5a/d/B01eHLU76gU" +
                "r+a+ozrvkrRaezhGYkV6bTOxl/dhdxpViGO0GgNiQsaWWYzJri" +
                "JvzvdsixMkrdYejtk4Nw7rDJ0veiPqZRthPqVOhJ9dqjuN7Cfd" +
                "CUTNluQhAl1RI0538jiQN+d7tp0rJK3WHo6RWJFe20x47694H3" +
                "ankT1PryBqtjQGxISMLbMYk11F3pzv2RazJK3WHo7BDD2zMWQP" +
                "ng7e/dnwnFDdaWTP01lEzZbkWYwRuogZYho8lePN+dhrK/4kWn" +
                "P3ySqlrEoE+1CjneqT8hYny1F5L3WSYyUrYpHrlu/iUSDvdJ32" +
                "PZJWaw/HSKxIr20m9vI+7E4jey91D1GzpTEgJmRsmcWY7Cry5v" +
                "wU2+J0yho8oX3iZx9qtFN9Ut7idDkqz9PTHCtZMYt0t3wXjwJ5" +
                "p+sUx0harT0cI7EivbaZcTfvle40smyPIWq2NAbE5DHYjAiTXU" +
                "XenO/Ztm+RtFp7OEZiRXptM+F6uuV92J1G9tq/hajZ0hgQEzK2" +
                "zGJMdhV5c75n214jabX2cIzEivTaZgLONe/D7jSye7qGqNnSGB" +
                "ATMrbMYkx2FXlzfoptcaaeJTP2ofbxcZ+UtzhTjsrP0zMcO/yd" +
                "cbC0UojjbvkuHgXyTtcpjpO0Wns4RmJFem0z427eK91pZNkeR9" +
                "RsaQyIyWOwGREmu4q8OT/NVp6hbD1r+Kf1SB7j51XsJWk1W4Or" +
                "pRQfSq91Ld/L+7A7jSrExd75JY9WY0BMyJjrqKdQy4jJ4kDenG" +
                "/988Fft/Yvb/ddOvL27j5Px40v9/y09wZGz3++e56fjv5vfJ5X" +
                "9Px08OUY/h/fTDHjrrqZWp8mM6noqObDP+pjGA1t9jn//d38nH" +
                "8b/9/0/sM5Tzvr3opWZdZZr1Ot3grW76zXRW1x51ngWr5LCoVk" +
                "16tj76Ue8F15v8a5dr2p67dOt3HkVh+9pd4SSdLso7l4bAxHyL" +
                "x8da5Hmbqm9JRs21njkgiLk7t59IgKK0tuzMjmaG4ejeZqd7Pi" +
                "nv/vR/Sef6y8ejd6N0iSZh/Nu/+yx8ZwhMytn3O4vmTr+lFnjU" +
                "siLE6LVNdAVFiZPcTLM7I5ES/sJIg129x52jv7aJ6n4+aVu5eq" +
                "WWGH7qUu3h31XiqZ0czfTPwHIn2gmg==");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value16 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value16[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value17 = null;

    protected static void value17Init()
    {
        try
        {
            final int rows = 57;
            final int cols = 85;
            final int compressedBytes = 1257;
            final int uncompressedBytes = 19381;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtWj1vG0cQZWMkhUt1URPASCEDQWAbgmEkgUAeFaRzoU5VCs" +
                "H/wRQS+e5CmLZrt24N/Y/ULgMEbmL5Kwlkx4ErVxY5HM57s3si" +
                "Q54pUtwVOLszO/PmzWjvTke70QhH97hxzsfd1xN5/TUderaX7Y" +
                "mUWW2im4V91MN0tmMkYlpOi+bMyMs8mCczRQzPyiN7xr4ijonV" +
                "5TMZY8NvX2hfEClzf0c0XatEH/Uwne0YiZiSmaM5s7JjD+bJTB" +
                "HDs/LInrGviGNidflMxhjwd9o7IvvzcGfHdNtVG/qzTT6sGY5l" +
                "CBEwOzIwBPYO/UIs5GbSc4tVhDy4Az4ndsnwY+NO2UhjhpHdzm" +
                "6LlFltopuFfdTDdLZjJGJaTovmzMjLPJgnM0UMz8oje8a+Io6J" +
                "1eUzGWPA38/2Rco82NlX3Szsox6msx0jEXOYmaI584gdeTBPZo" +
                "oYnpVH9ox9RRwTq8tnMsaA38k6ImUe7HRUNwv7qIfpbMdIxBxm" +
                "pmjOPGJHHsyTmSKGZ+WRPWNfEcfE6vKZjDFWu73u7wfb632b2E" +
                "2a7e573sW9EDHE1xyMEWfBOBLlI9hi64Mvq7BD7xjuJMNz7Otl" +
                "r/zV+5UHn+7+XRY1YnUX8xnV72n5oPyl3p6W9xahp+XPM+LfX6" +
                "Setr+r7mn13vx6OhmHaZm2r7evi5RZbaJ7i/rEdYy0eESwnBxh" +
                "eeIezDNkyruhJ7MZVwHyYG/O6LtkefIb+bcnciNv2d/8+VVZ9T" +
                "6b5PeSX6nc+SGwbE5/LvMf3Xc9L8b4b1Xt9D6viLgWWJqzX0/F" +
                "q9Pfo/K9/4H1z3D+c2R5nX8V9TwayEiPijcD+T7K5VZl5r/luT" +
                "/Ung3nwfdMxb8R/+cR27uJ6zw++bwt/jv197veSKPmkXo6cwdP" +
                "uZ9OiHBG99P862nvp5URn+R+mr6Xqmc0fxPJs65Usq/JcEasqp" +
                "3qCP0ZxxjZIVtfVZiLecX5GJdYp3xGnytd+1XXfvmkvmu/98WS" +
                "PAkepWfUKp/TZjdc1Xbv7i4CVnPu32iVj9Nzu/a7wf3Ug7qHfX" +
                "fdu7TMdej7/iKMrXfhKo303F+0d9PWH+FqGcfZsU/ntP5zOu+e" +
                "No/q6WnzaPqeVnHgnva+X5aersZ7VPODSJ51pZJ9TYYzYlXtVE" +
                "fozzjGyA7Z+qrCXMwrzse4xDrlM/pc6Zwu//009TTdT1NP6+pp" +
                "+XL6c9oqWoVImdUmulnYRz1MZztGxjAxmlGRF3rwmtHjnrbr2T" +
                "Bbv7aYWF0+U6sIu9k/p77P9f0bX3hO6xzl72P2n8527fe2F/Ha" +
                "j/hu1tfTeTyjFu/dNPV0Ps+oVufsn1FVHNy1fzNd+6t7Tlfhb/" +
                "5wpP/bk85peo9K7/vpnKb76WreT7sPz/s5bR22DkXKrDbRzcI+" +
                "6mE62zEyhonRnBl5mQfzZKaI4Vl5ZM/YV8Qxsbp8JmNs+Nlatt" +
                "bXdNa12VWqLVzjR1eIZZhqQ4Qwhjl46Zl6LMxjyFhlvArMY/i+" +
                "NvRmLoB/Mbs40Iazrs2uUm3hmj8qDcswGd8jETvyQOmZIgbvIz" +
                "JWGa8C8xg+e3MvmAvg72a7ImUe7Oyqbhb2UQ/T2Y6RiDnMTNGc" +
                "ecSOPJgnM0UMz8oje8a+Io6J1eUzGWOsttEoLqO9+Mb+X1/vp/" +
                "P5XC425p3xzpP019BM4yNY9bUY");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value17 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value17[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value18 = null;

    protected static void value18Init()
    {
        try
        {
            final int rows = 57;
            final int cols = 85;
            final int compressedBytes = 1478;
            final int uncompressedBytes = 19381;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtWz2PG1UUdYO2o0D8gpUWaVOABAhpEYV31yOULsAGhPkWH3" +
                "9ilZXiXRMsykjpU0KaiJJfQIHkIgVIfIUuUhqk0ERpwL5zfc65" +
                "781k1zvZGPLG8nvv3nfuuedez4zHG+j1Rq+OXuv1RudGu736uH" +
                "w0eql3gmP0YuPO64nnld7Sx+h8sJ9/CL5/4gwvJ56dpZSWnpae" +
                "/gd6mmF+tleOf4/JV91xXT4q/TzNMf5s/Ob4C7LfGr/nPR1/kD" +
                "mH72U4LtbzJ+T7FNf++MLC+/bo70Ylw3r+vBGxp/bhj7L7Lq3f" +
                "n2f/K8S/Mx8/yjC/0XjNHvPaH384/rj12v+6nGtdH+jpzs3lGJ" +
                "aN+x/fm6+VHpzyrCzPUmfwLHV4vpxp3Rw7T6Wr3C6sHK6No20n" +
                "8rdxNyFnq/Yq4l57liYViM7zbF/fvm6jze4zO3oco2veZ2/kZx" +
                "+wnJlVMSLPk+ZtYkY1qeJYkapXtOaIXUKek91Przz95NxPr1w4" +
                "m9/7T1JPv7zX4e/9G+XbpfPn029KDzrv6belB90c29+lq9wurB" +
                "yujaNtJ/K3cTchZ6v2KuJee5YmFYhu4im/o5q+o46my31HTW6W" +
                "v0udxVH+Jl16uop/l3p0PU3vp10eRz89ZP/XU95Pf+numX/yfT" +
                "nTOn+quv34GVahwi6rKL9NT3cM9gZ7NtrsPrPhUYwjYKufI5kT" +
                "ORGtmVkXEKpTlTJHVBWZo+JYkcbk6oqZBntpN3PH4TPlXCvPUq" +
                "v3LFV+m+afpeae8t+gHbOnkx8ebU/Ltd/5k9rV7as22uw+s6PH" +
                "Mbpmm73gByJl48ysKnICDSxwvKsZEa36mipS9YrWjLFLyFPup0" +
                "3X/vi37q79wbl0VY5V/o4avPD4z9MmDd18Rw02Bht5n/kxwgeP" +
                "27rPjDl+9zuHsqaRrEHxOaSic8xsY25SmsY0++qaNgebNto839" +
                "l0Gx7FOAK2+jmSOevMEq2ZF+oEoTpVKXNEVZE5Ko4VaUyurpgJ" +
                "isFfXaou2Tib7TDLbOy6j/Hqs7da4EGGlIGzswIwKDrFpVysDW" +
                "PUlquIdWgHYk7uEvEfVAc2zuZ65wA2dt3HePXZWy3wIEPKwNlZ" +
                "ARgUneJSLtaGMWrLVcQ6tAMxJ3eJ+PerfRtnc72zDxu77mO8+u" +
                "ytFniQIWXg7KwADIpOcSkXa8MYteUqYh3agZiTuwT+/v3+fRtt" +
                "nu2Y5Wv2OEbXvM9ej1TGyMaZ/YiIPE+at4kZ1aSKY0WqXtGaI3" +
                "bJowdrgzUbbZ7fadfchkcxjoCtfo5kzvpOLtGaeXG3F4TqVKXM" +
                "EVVF5qg4VqQxubpiJigGf3+9v27jbK67vQ4bo/vStb59BBc43c" +
                "cMYJIzdZ0RqlOVRi7OEzVF1bmKWE1Eq3rVAv5qWA1trIaLu8gQ" +
                "Nnbdx3j12Vst8CBDysDZWQEYFJ3iUi7WhjFqy1XEOrQDMSd3Cf" +
                "x1fzdmL1j2lk9gw1DYdYTNPOoqzdDPPDurz7HI088+j4PV9XGE" +
                "7sAHHSk390Fr5Si8Wip6bvaCZW9FGAq7jrCZR12lGeJe6nMs8u" +
                "RimNX1cYTuwAcdKTf3QWvlKLxy6qsH1QMb7eU+rHnXLPfkZsQA" +
                "CU7PGSOxB1WMiDpTXFMVqQrVlqvI64jI2AvtEvj7W/0tG2dz3f" +
                "Ut2Nh1H+PVZ2+1wIMMKQNnZwVgUHSKS7lYG8aoLVcR69AOxJzc" +
                "JfBn/9108X+mTH4uf7c7+bF7a/eWjTa7z2x4FOMI2OrnyBwnRy" +
                "sr62KErpU9j8RuVKNq4xoxubpiJijmajN9njZ+AtNjfU7TJT/f" +
                "acfny3R1zt3J74vVH+VK7qintxerP0s3lrg+7uzesdFm95kNj2" +
                "IcAVv9HJnj5GjNzLqAUJ2qlDmiqsgcFceKNCZXV8wExeCv7lZ3" +
                "bbTX/ClL1rxrlntyM2KABGf9FJdEYs8RnE9HxkZlaRWpCtWWq8" +
                "jriMjYC+0S+Hv/AE2oVNM=");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value18 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value18[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value19 = null;

    protected static void value19Init()
    {
        try
        {
            final int rows = 57;
            final int cols = 85;
            final int compressedBytes = 1728;
            final int uncompressedBytes = 19381;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtWk1vW0UU9Qp1UUVZIFQkiug2El1AhCpEJGo/FyEVgoBfwj" +
                "+oiYxws4u6QPyBbrvogkpZVPwAsocVUjcRrNgWRPOur8/HzItJ" +
                "7EYB3rOYmXvnnHPPndp+ry7NZrM5GDSbzXweDDLKdY6MSQRizT" +
                "OTNQcD1q9VHgwUkwqMVqes4a5c2R17R8qp9eWV4Ji7jWvyOa0/" +
                "HZzjmnwxnz8ZXPA12R1c+uvbZ4P+WvM1vdmfwdrfp8f9Gaz9TH" +
                "/rz2CV6/bu7d0YY85cxJ5JjK455iz0gSjVuDK7ck2ggQWOd7Ui" +
                "2OqvqyN1r2it6KeEOpP3Jx+8GLcmo3R6b2/ybtefwTcblfvuO5" +
                "135I+KzHsr3N8/tnjJ9/7kwzNX2JZeP3uRGZ7L6ZnOtKrwHz3T" +
                "6R/nO9PmWnMtxpgzFzEyikkEYs0zkzVRE2ytzL6AUJ/qlDXclS" +
                "u7Y+9IObW+vBIcc7eVZ6kv+/vMKtf4/vh+jDFnLmLPJEbXHHMW" +
                "+kCUalyZXbkm0MACx7taEWz119WRule0VvRTWrD3x/sxxtzu7G" +
                "fsmcTommPOJhMKyDGD8XWE+gQWON7VimCrv66O1L2itaKfUrKb" +
                "nWYnxpjbb4WdjJFRTCIQa56ZrDn/1hG2Vl58MwlCfapT1nBXru" +
                "yOvSPl1PrySnBM+s+b5zHG3O48zxgZxSQCseaZyZrzysLWygt3" +
                "glCf6pQ13JUru2PvSDm1vrwSHJP+g+ZBjDG3Ow8yRkYxiUCseW" +
                "ay5ryysLXywp0g1Kc6ZQ135cru2DtSTq0vrwTHpP+oeRRjzO3O" +
                "o4yRUUwiEGuemaw5ryxsrbxwJwj1qU5Zw125sjv2jpRT68srwT" +
                "F3W3nyfbV/HlrpWepofBTjyYxcxhgzV671vxxZXyuoApTcFxDq" +
                "U526FtdxT+661pG6V7T7UHTuDreH2zHGfLITUa45kxhdc8zZZE" +
                "IBOWYwvo5Qn8ACx7taEWz119WRule0VvRTSnZz2BzGGHP7rXCY" +
                "MTKKSQRizTOTNeffOsLWyotvJkGoT3XKGu7Kld2xd6ScWl9eCY" +
                "5J/0nzJMaY250nGSOjmEQg1jwzWXNeWdhaeeFOEOpTnbKGu3Jl" +
                "d+wdKafWl1eCY+62vGZXX+K/x32/Pq2v/7qsd6n+t76u3/r2fu" +
                "p/P708Z1pe9/b6Z8yVn1F/iFHnXOWoWIzlzFplrTLn1eO1zDG7" +
                "Y7felcal41oeip5RdvK9Vv/Z7/rst5lzfvZHWzHqnKu9705G5H" +
                "wsZ9Yqa5U5rx6vZY5HW7PXSrfswT15x6lDJ/jUPakP7zv5Xqt/" +
                "n76Me9Twxxh15kxigMVYzsr0WpyrMfK1zHHdLSt6hbKuMtxv6c" +
                "P7Tn5Xt/TM/3p/3173M//sav/ZX/kedSNGnXP14h51IzHAYixn" +
                "1iprlTmvHq9ljkc3Zm+UbtmDe/KOU6fzHlX48L6T77Uu+h41e/" +
                "P/cI8abcSoc65mb52MyPlYzqxV1hpt1D2geryWOR5t7L1SumUP" +
                "7sk7Tp3O92nhw/tOfle3F/N303X+LnVZr+Hx8DjGmDMXMTKKSQ" +
                "RizTOTNVETbK3MvoBQn+qUNdyVK7tj70g5tb680vC4PM3+N5SX" +
                "ed3+s1zVdhHVcKdpnLbj+qdpdyFPVqd34XunV+lyAXZdZ3w4Po" +
                "wx5sxFjIxiEoFY88xkTdQEWyuzLyDUpzplDXflyu7YO1JOrS+v" +
                "ND4sT7P/7K//mv5ytufT6c/936OWXbObZzvT2dv9mZ7L+yn/b8" +
                "/01/6zvexqjpqjGGPOXMTIKCYRiDXPTNZETbC1MvsCQn2qU9Zw" +
                "V67sjr0j5dT68krNUXma/+bfpKfPLutnvz/Ti/g+nf7efyeu/f" +
                "mqP9OVrvHD8cMYY85cxJ5JjK455iz0gSjVuDK7ck2ggQWOd7Ui" +
                "2OqvqyN1r2it6KeU7OHd4d0YY25/XbmbsWcSo2uOOZtMKCDHDM" +
                "bXEeoTWOB4VyuCrf66OlL3itaKfkrJbg6agxhjbp8IDjJGRjGJ" +
                "QKx5ZrLm/IlD2Fp58VQiCPWpTlnDXbmyO/aOlFPryyvBMfTHj8" +
                "ePY4y5fQc/ztgzidE1x5xNJhSQYwbj6wj1CSxwvKsVwVZ/XR2p" +
                "e0VrRT+lZA9vDW/FGHP7Dr6VsWcSo2uOOZtMKCDHDMbXEeoTWO" +
                "B4VyuCrf66OlL3itaKfkrJbq40V2KMuX0HX8kYGcUkArHmmcma" +
                "80+IsLXy4lMkCPWpTlnDXbmyO/aOlFPryyvBMXd757o/C9y5fp" +
                "KLPEbkNM/7vFePuIZq1F2oTrCcoZmutfT+lSNquv/kco9t/De1" +
                "CGgl");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value19 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value19[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value20 = null;

    protected static void value20Init()
    {
        try
        {
            final int rows = 57;
            final int cols = 85;
            final int compressedBytes = 1349;
            final int uncompressedBytes = 19381;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtWcFuHEUQnR8gnEGRIlCkAJEiCCgQlEMys4uJAIESsIglFH" +
                "GKBJwACaRIgRmGnTBw8BkEEReOOeUPuHDzmQ+AH+AcifXWll+9" +
                "6h42uzvetdnuyN1d1a9evar02LN2lpWPZ26UdfnkeD412T81mZ" +
                "8ef53JsnbrAPPC/twOp9aF8dcreta+RmzDLDLK6+W75XuT3ftZ" +
                "dJQfl5+Q/dn464vyy/Kx8oRDPlGeNNZpsz+bzRjt61Pk1fKt8u" +
                "1s7lF+RNan5ef7a/VqdWk8P1cV6Gn1YqeGN0Nfdb4LXW0Fngv5" +
                "yWzBUV119rkZ+MtdJ10aqpcCT76Q0rl6GmWYq6dZtv6edkZQT9" +
                "t3VtPT9vrm9DTd0/57Ovpn0Z5Gvtu8Ee4WZTia43D15Vv5lsyy" +
                "qk9s71EM761tveAHImSzma0qzwk0sMDZU86IaNbXVRGrZzRn9F" +
                "3S6Pq7+tv6h/orW1H99bL/U8POp67+Zni5r/tQj2ac35lf3/yo" +
                "SNzF4UWZZVWf2N6jmLhtIxFvGZCTI5AnjmCdoVI+DZGsZlYFVg" +
                "ejOaPvkkbn2/m2zLJObvC22t6jGN5b23o1Egzw2QiLjyNYJ7DA" +
                "2VPOiGjW11URq2c0Z/Rd0ujB3mBPZln3T8TSvc4WowjY7LeRll" +
                "MyczRn1m4xgnWyUsvhVXlmr9hXxDGxunwmKLbVHt93qdFf/4/3" +
                "07u/bs776d17i/W0ebk5U11qTu/3tJkyNs/Od0//gz3oYHM262" +
                "00z8w4P78E9/PTtadO5E246+19uDkKXP3XtSn3tP1gPfc09XQ1" +
                "z/7o3uE9F+2Hq/tUP/pljb9R+ENmXnWnM2Mxh6vl6jrpjtB/sx" +
                "RbdVatryrMxbrieqAl1imf0eeKvFF8n6WxxCgeFA9kllV9YsPD" +
                "GEXAZr+NjHHaaGa1uiyC98weR+LUq2G1fo+YWF0+ExTbasNR1u" +
                "mu9X53f5//ZH7UcnkfJceiOpb++XQtvyazrOoT23sUw3trWy/4" +
                "gQjZbGarynMCDSxw9pQzIpr1dVXE6hnNGX2XkGfVf4s+Pp/3V/" +
                "U7lObE5vR00b/xpd+hHMbnqPS36L6f/fx2fltmWdUntvcohvfW" +
                "tl7wAxGy2cxWlecEGljg7ClnRDTr66qI1TOaM/ouaXSxW+zKLO" +
                "vkHWRXbXgYowjY7LeRMU4bzawH70EOwXtmjyNx6tWwWr9HTKwu" +
                "nwmKbbXp2V/3z/1N6mm9l3raZ0/rvxe/p8X94r7MsqpPbHgYow" +
                "jY7LeRMU4bzZmtLiBYJyu1HF6VZ/aKfUUcE6vLZ4JiW226p+n7" +
                "aepp2NPizvp72qUh3dOje0/T76T7H+mepmc/Pfvp2U/3NN3T4z" +
                "NST5cbgxuDGzLLqj6x4WGMImCz30ZaTuRENGe2uoBgnazUcnhV" +
                "ntkr9hVxTKwunwmKDf/OYEdmWScnO2rDwxhFwGa/jbSc08wUzZ" +
                "kP1BGCdbJSy+FVeWav2FfEMbG6fCYoBn/xsHgYfHKb+MSPGT72" +
                "+3Nl1DnkV7+iuxE+l6BZiUdajV6vjfFrl44wptunHOnn/ire+d" +
                "ufU0/7fpcq/pz/pM/4vsajZDsMRfmV/IrMsqpPbO9RDO+tbb3g" +
                "ByJks5mtKs8JNLDA2VPOiGjW11URq2c0Z/RdQp70ftr7Pb2V35" +
                "JZVvWJ7T2K4b21rRf8QIRsNrNV5TmBBhY4e8oZEc36uipi9Yzm" +
                "jL5LyJPuad+j/S30NT8tw9j8uOk9Hd4c3pRZVvWJ7T2K4b21rR" +
                "f8QIRsNrNV5TmBBhY4e8oZEc36uipi9YzmjL5LB3n+BdQK2kc=");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value20 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value20[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value21 = null;

    protected static void value21Init()
    {
        try
        {
            final int rows = 57;
            final int cols = 85;
            final int compressedBytes = 1792;
            final int uncompressedBytes = 19381;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtWj1vHFUUjUKQLJoU/ADkComCAhAFokgmO8GkQYI2ZcoUVv" +
                "rIytoCZCuSS4sUcYuQQJb4BanokPwHkJAoXQANkhvYvXvnfLw3" +
                "43i9iYIzE/m9d88999xzX7zJZp0rV8pn9/sr47OCZ/ol3emP3e" +
                "mncyh8taj54aV7/+LVv99vnlyo+rvX/fuzvdfeizX2xCJ2JDl6" +
                "5phR6INRqnFnduWaYIMLHme1I6rVX99E6l7Z2tFvqas+bA9jjX" +
                "2eOczYkeTomWNGsxIKwLiC+XWG+gQXPM5qR1Srv76J1L2ytaPf" +
                "EvpMP5l++t/63vRWOn20M/3wXH+efdCb+axAPr7An5ufa/z1H2" +
                "fwb5y7w0cF0izx2r/eXo819sQiBqKcZCBWnCtZEz1RrZ3ZFxjq" +
                "U52yhrtyZXfsE2lNbS7vBMekf9KexBr7PHOSMRDl6Fm/cl10OW" +
                "HNxFiBO2sdGOoTXNfQvM6kSP9E6l7Z6l69QP/2+u31WGOfZSLK" +
                "c67MSQZixbmSNaOzVmvndKcM9alOWcNdubI79om0pjaXd4Jj6L" +
                "dH7VGssc9v+yhjR5KjZ44ZzUooAOMK5tcZ6hNc8DirHVGt/vom" +
                "UvfK1o5+S1ndPGwexhr7LBNRnhlJjp45ZjQroQCMK5hfZ6hPcM" +
                "HjrHZEtfrrm0jdK1s7+i1ldXu1vRpr7PPbvpoxEOUkA7HiXMma" +
                "i99NqdbO3XegMNSnOmUNd+XK7tgn0praXN4Jjnna/+97qen7L/" +
                "a91M6vS76XutZeizX2xCIGopxkIFacK1kTPVGtndkXGOpTnbKG" +
                "u3Jld+wTaU1tLu8Ex9CfrE/W/Z4DCxwrMCAZa54Va/qJp4aqlp" +
                "XsQfk1prJryhxj73Na1vRji5kOJgexxj7PHGQMRDnJQKw4V7Lm" +
                "orNUa+fOnTDUpzplDXflyu7YJ9Ka2lzeCY6h32w2m7HGPv/baz" +
                "NjR5KjZ44ZzUooAOMK5tcZ6hNc8DirHVGt/vomUvfK1o5+S+hT" +
                "+ZN6r/N8tNzfJsvWXY6nud/cjzX2xCJ2JDl65phR6INRqnFndu" +
                "WaYIMLHme1I6rVX99E6l7Z2tFvqavearZijX2e2crYkeTomfOM" +
                "ZqUquhp37u7KGHWdsm+fMqYpHftE6l7Z2sNvqat+0DyINfZ55k" +
                "HGjiRHzxwzmpVQAMYVzK8z1Ce44HFWO6Ja/fVNpO6VrR39ltCn" +
                "8/fW850QKab5Gn/o7Px+ba1mZsR1xbLyLE6/C1SfpdO8UZ5qWU" +
                "Q13pDGUMb1h7T7mLPT8BSeG+7S5wLVdZ3dZ+O/TVf9OT/d/rXy" +
                "VMsiqvGGNIYyrj+k3cecnYan8Nxwlz4XqK7r3Hx682mssScWsS" +
                "PJ0TPnGXV9xsDlzuyKGXWdsm+fMqYpHftE6l7Z2sNvCX3KZ/ud" +
                "8f+QrPr59u3xDsY7ffWfRzsvTnv65PLf3621W2uxxp5YxECUkw" +
                "zEinMla6InqrUz+wJDfapT1nBXruyOfSKtqc3lneAY+s1pcxpr" +
                "7PN3BqcZA1FOMhArzpWsuXjnIdXauXu/Igz1qU5Zw125sjv2ib" +
                "SmNpd3gmOe9uW+9l+Hp11r12KNPbGIgSgnGYgV50rWRE9Ua2f2" +
                "BYb6VKes4a5c2R37RFpTm8s7tWvlbY4/N139v02bu83dWGNPLG" +
                "JHkqNnjhmFPhilGndmV64JNrjgcVY7olr99U2k7pWtHf2Wsnqy" +
                "PdmONfb5T6q2MwainGQgVpwrWXPxkzCp1s7dT8uEoT7VKWu4K1" +
                "d2xz6R1tTm8k5wzNOOr/1Vv/Yne5O9WGNPLGIgykkGYsW5kjXR" +
                "E9XamX2BoT7VKWu4K1d2xz6R1tTm8k5wTPr7k/1YY59n9jMGop" +
                "xkIFacK1lz0VmqtXPnThjqU52yhrtyZXfsE2lNbS7vBMfQb7fa" +
                "rVjb7idnEUWMbGLMVyy+NIIOOpQK3J0dQEHZJa/UYm9Y3VttIv" +
                "ahN+A9+ZagX/2s783upwC/je/gl31uvDv7hSi+lBEsZJMRO696" +
                "Kjt4rsSSiz61GlZNf1yhGWDwUWrzPeisXIVf/RPZ5yq/nD9zft" +
                "bF+j5Pj2V9vIiHXvu/j6/hV//z0/FOx2eZZ3JncidPikWMFZji" +
                "ZR4s1i/7RgUz6pXsQfk1prJryu6c/QzfVf/tyc1tTDYW8UaXmW" +
                "MRYwWmeJkHi/XNzUZ2YUa9kj0ov8ZUdk3ZnbOfwTvdqE0xFOPZ" +
                "PRlfv6t+dv+69BP++Vysv1f4Xurn8ftq5b+L/4x3cLFn/Px01Z" +
                "+fjne6+jttH7ePY409sYgdSY6eOWYU+mCUatyZXbkm2OCCx1nt" +
                "iGr11zeRule2dvRb6qqP2+NYZ/sic4wYa2LlWb9yhRY0E2MFKM" +
                "mtHjNDfapT1+I+7sld1yZS98p2H8ruqv8FDcgX3w==");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value21 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value21[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value22 = null;

    protected static void value22Init()
    {
        try
        {
            final int rows = 57;
            final int cols = 85;
            final int compressedBytes = 1375;
            final int uncompressedBytes = 19381;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtWsuKXFUUbTLogYgSEMSB9CCCUaGRVhQkSFO3KuLMYX4gv+" +
                "BUqmhTtgX9Aw7EQUb9L/EDHIbgyHxAIGC6du1aj3NuKt1V6ee5" +
                "Rc65e52111775FbdW5VsbZXH+GCrHRs+Zu+0PVjvmHw3ufdq/H" +
                "zS4TqdfNXH/u2visJer/YPBfLNGk5/tHh3BX//1BW+ll7/fIUM" +
                "2p5exT2tKlzTPT34+6x72u5RF/152q7TNzv2/41RZ0aSAy7Gct" +
                "ZMr1ViXj1eqxzX3bIH9+Qda2d1T7rqfWe+1zrv63T24U24Trvb" +
                "MerMSHLAxVjOmum1Ssyrx2uV47pb9uCevGPtrO5JV73vzC+77X" +
                "Zi1DnPZh+djMB8LGfWKnzulJhXj9dr93Tu6GC7dMse3JN3nDr9" +
                "VdyH9535fd22+/4mj+79GHVmJDngYixnzfRaJebV47XKcd0te3" +
                "BP3rF2Vvekq9535nut092jZh9flmepwdOz36MGT9/kHnX4/fl8" +
                "j5rttOfTy/Y9avLH9f8sHb0cvYwx5sQidiQ59Zgzkc8KqKkZqF" +
                "NnqM/Sqa6WTHWzqgP2oWyt6LuEOuV7//d3r8Z300fP3u57f45s" +
                "6PO07em6e1oej/5rz5drfZ4+Hj2OMebEInYkOXrOMaPQB6NU48" +
                "rsyjXBBhc8XtWKyFZ/fR2pe2VrRd+lZfb2aDvGmOcr2xkDUU4y" +
                "ECvOmay5qCzZWnm5W8JQn+qUNdyVK7tj70hzan15JTiGfveie1" +
                "F8f5hjgWMEprivp2KOpX7iye5neK1gqxNnskf3yzk+9/koc/qx" +
                "0Bg+GT6JMeaTlYjyPEfmJAOx4pzJmlFZs7VyulOG+lSnrOGuXN" +
                "kde0eaU+vLK8Exdzv/NXXv5IUo/sivsXvBwmoymKkamo8K+5Xn" +
                "BMWSizr71WcLqKY/ztAVYPBRavM+aK+chVd/R+13qQ1/W2z/Hn" +
                "UOz6eTWbvSNn1clff+r+MV679cjff+9L2b896f/nTz/s/EZb1O" +
                "p99OP53cm35ycp1OF39L089Od496jXpxVU6/2KD3uyvW99bQ/n" +
                "Ixn2EnhneGd+pY4BiBAclY11mxpp94aqhqmckelF9jKrumzDHm" +
                "PqdlTj/Wr9H9U55dxePi3Lfn0/P4TbrtadvTy7an3bSbxhhzYh" +
                "EDUU4yECvOmTVNzlZV9sUMPVf1OhOr7kbd+jlyan15JTgm/ePu" +
                "OMaY5yvHGQNRTjIQK86ZNU3O1spLd8JQn+qUNdyVK7tj70hzan" +
                "15JTjmbs/7vd+NL/693+fh7f2G0n7ra89S7b5//ff0/q37t2KM" +
                "ObGIgSgnGYgV50zWRE1ka2X2BYb6VKes4a5c2R17R5pT68srwT" +
                "F3267Tdo9q96ibeJ0OHwwfxBhzYhEDUU4yECvOmayJmsjWyuwL" +
                "DPWpTlnDXbmyO/aONKfWl1eCY9IfD8cxxjxfGWcMRDnJQKw4Z7" +
                "LmorJka+WlO2GoT3XKGu7Kld2xd6Q5tb680lC+PSxWD4eHMcY8" +
                "XznMGIhykoFYcc5kzUVlydbKS3fCUJ/qlDXclSu7Y+9Ic2p9eS" +
                "U4Jv2j4VGMMc9XjjIGopxkIFacM1lzUVmytfLSnTDUpzplDXfl" +
                "yu7YO9KcWl9eCY6hP3o4ehhjzCcrEeU5I8nRc44ZzUwoAOMM5t" +
                "cZ6hNc8HhVKyJb/fV1pO6VrRV9l1CnPGY/t6ehdY7R89HzGGNO" +
                "LGIgytFz/ZMj6yOjVOPKmgeG+gTXNXRde1KkvyN1r2x1r16428" +
                "pT2gftWlvnGOwOdmOMObGIHUmOnnPMKPTBKNW4MrtyTbDBBY9X" +
                "tSKy1V9fR+pe2VrRdwl12nW68eN/iXUw3Q==");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value22 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value22[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value23 = null;

    protected static void value23Init()
    {
        try
        {
            final int rows = 57;
            final int cols = 85;
            final int compressedBytes = 1331;
            final int uncompressedBytes = 19381;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtWz1vG0cQvSYpUwiuDBdxpCpFCjtIYaSIdCcGgpHYSH5Cau" +
                "svOKIpKbAaQ1CRIICRzm4M/QBXaQLLhpFfkr9gxOJwOO+93RN1" +
                "FEV9cJfA7M3bNzNvVsc7HiVVVf9e/9uq6n/Zr6vR2Br071YdRv" +
                "9O68r3CfJNNfXob4j/1QT+d50rfI3e4N+PyFo1k7E1qMo40+h2" +
                "nu5+tjjn6e7Dac/Tbnu6vbs4e7q9M589XaTr6RCZYk9Xn68+N2" +
                "uzY+Yr4hw+xnVENT9iwcXKqAoZ+Txp3bbM0U2qWDti9czmGrpL" +
                "Uaecp7M+T3srvRWzNjtmfiDMcUb4jGMk5oyaEc2VUVcwWCcrxR" +
                "yqSjOrYu2IY3J9aaVQHPmbR80jszYfr5jnx26R44zwGcdIzGmV" +
                "OZoruzpmsE5WijlUlWZWxdoRx+T60kqhGPJvNptmbR6ubLofCH" +
                "OcET7jGIk5R5UpmiuP1RGDdbJSzKGqNLMq1o44JteXVgrFkb+3" +
                "3Fs2a/PwDF52PxDmOCN8xjESc47eIRTNlcfvImKwTlaKOVSVZl" +
                "bF2hHH5PrSSqEYu03Hk0/8aO9ZeSbqPuoP9Yc8ZnjYwBjXdc/o" +
                "Ns3vuLPbGVrL2KxEmahR9WKMzm060ph2rD1HfdSa9ehUP6ejKX" +
                "++Heqepsa0Os7l3H3bfaU762x1T1NjWh3nMcr3UmVPL9to7jf3" +
                "/Ygx88MGxni6HizMn9a1CGTkI1ED83NMZucyq3LUc/Jete8e7d" +
                "xGM3qKbsZP04aZHzYwxtP1Bp7LI7+o2fAqyMhHogbm55jMzmVW" +
                "5ajnxD3dyHXR7vd/guMfp/qG4+fR/MPcv1V/cPmvB3u/l2ti2d" +
                "MrsKd/lj2Y+Z7+VfZgUT+fbm9NWH98UcrK76IhYka/iy7n6Vyu" +
                "py/L9e9Mz6YHzYFZmx0zPxDmOCN8xjESc0bNiObKqCsYrJOVYg" +
                "5VpZlVsXbEMbm+tFIoxm7Ldyhzee+/Knswz/t+NkP5m4lrc9+/" +
                "zKO+aZZnP3r6y7ENTG06Y660VoppdXtNUlzfHHyaqkUNqkk79j" +
                "xwVv6tmliH9u3xWqu892f/3i97eh7X0/oLszwj4pzghk1njtRa" +
                "KabV7TVJcV4talBN2jF3ltfEq9q3x6fd1ktmefajj9fTJecEN2" +
                "w6Y65E51KKaXV7nbinQ0XD66moRQ2qSTv2PK3X00SH9u3xbd2W" +
                "+/5M7/ufm+UZEecEN2w6c6TWSjGtbq9JivNqUYNq0o65s7wmXt" +
                "W+PT7ttr5hlmc/GvxxbANTm86YK9F5I8W0ur1O3NOhor3XqVrU" +
                "oJq0Y8/T+t5PdGjfHq+1ut33f3uzOPf98j9nl3vs/Ff24Cxj/c" +
                "X6C7M2O2a+Is7hY/QRjfzBSLNhZVSlOYMd3ODhKleMaNbX1hGr" +
                "ZzZX1F3y6Ga/2Tdr8/GKeX7sFjnOCJ9xjMScVpmjubLvFjNYJy" +
                "vFHKpKM6ti7Yhjcn1ppVCM3ZZn0/K8fzW/P+3duh73il9vX1Tl" +
                "bufp3rtynk4azfvmvVmbHTM/EOY4I3zGMRJzRs2I5sqoKxisk5" +
                "ViDlWlmVWxdsQxub60UijGbstn/vMYa/+Y5dmP3DI3bDpjrraV" +
                "9gh/TVKM6lCtdpXWYl15PaElt1NakfH6oD4wa7Nj5gfCHGeEzz" +
                "hG5nJiNGcdf1shDD7m7HlmrKoaVqvHEZPrSyvV9DcSo9XD+tCs" +
                "zcOVQ/cDYY4zwmccI3M5MZorj9URg3WyUsyhqjSzKtaOOCbXl1" +
                "YKxdht+XxaPvNfgT39H5UWWhY=");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value23 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value23[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value24 = null;

    protected static void value24Init()
    {
        try
        {
            final int rows = 57;
            final int cols = 85;
            final int compressedBytes = 1356;
            final int uncompressedBytes = 19381;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtW0GLHEUUblzwHNiLspfAGtQsSFgXBQki0z0bPAl6zCUg5H" +
                "9kaOx1dv+AehAhFxH8D3qTHPwRASEHb4JeJO7sm+d73/eqeqen" +
                "O2HJVjW8qnr1ve99r6ZmeqY3Od453qmq453jdV9VOtOxWo9RhM" +
                "3R7yM9Z1V5/lTmqkKMMng0KvUcrIqZWTFXhDGpujiTKfbVLj5a" +
                "3D23txe1rjxqF+9XA9riMLtyL3g+qLZui09p/t4l+E8GZzgKnt" +
                "lWSsueZva0/b3s6ZR72v4x5pzG9qitShvVyjmd+vO0butWrPTq" +
                "k7l5EKMIm6PfR6Y4fTSyel0egWNkTyNtldWgWh5bTKouzlS3cT" +
                "fLOS33/XLfv573/eaoOUr7xG/WfOiP654xxa9+iehDcC5BMx6R" +
                "XiPr9THc53TEmLxPOco5nf7zNPFd4NfhK8NR4/JukmNbHWNbc7" +
                "O5mfaJ36z5zKNzXPeMKX71KweyxkivAfEpJKJTzH5ufU5pjMn7" +
                "hOP0r4jrvhvzKnXfXvdfUfP78/tipVefzNmjGBz7ufcavyEim8" +
                "/sVTGnoQ1rOL+KGS0a9eUqQvWIxoy8S5Zn/Vn8uY2XP+vo9O8B" +
                "n+9frKN/eum/rj+7Yuf0wfyBWOnVJ3P2KAbHfu69xm+IyOYze1" +
                "XMaWjDGs6vYkaLRn25ilA9ojEj75Ll6Tung05LOaf6Sj+cPxQr" +
                "vfpkzh7F4NjPvdf4DRHZfGavijkNbVjD+VXMaNGoL1cRqkc0Zu" +
                "RdsjyxLX8sT0BHndMb8xtipVefzM2DGEXYHP0+0nNaTovGzF6X" +
                "IVAnKvUcrIqZWTFXhDGpujiTKfbVxnb6bzlrU7fyt5Op2uy1OE" +
                "qt2iyF6+PoW2H+Pu4ccjXqr4LX+rPkVFj0ZjyrdrJbTlh575c9" +
                "ffVbeSbtIiZ5Jt0sm6VY6dUnc/MgRhE2R7+P9JyW06Ixs9dlCN" +
                "SJSj0Hq2JmVswVYUyqLs5kih3/+RX2+cLXrMdqzVfRKq43FNmk" +
                "X8v/s/QhqqChAXUpZJPwVyFHE/qcjirLEX3CUT+vnzNOfOI3az" +
                "7087oyqo386ld0HsG5BI1KGOk1sl4fw31OR4zJ+/Ic9ZMs65NN" +
                "Pk82Q43Lu0mObXW8iHb2RrlzT76nb5Y9KN+lrtZ3qVRb/lNO2t" +
                "Ttqz/LHpT3/ot572//7/rOv3MciMVeR8svV9Z8bGPvuWKu6OPs" +
                "cl2muD5oX49qvQbWxBUrj9vTX1gT6uC6NT5WW++Jxd57FGNYs7" +
                "HHSNqNvejj7HL17ukeq9aR18CauGKsLK0JV7lujY/V1vtisdfR" +
                "+TndV4xhzcbecwWd+9HH2eXq3dMLRRfnlNR6DayJK1ae7DkNOr" +
                "hujY/V1rtisfcexRjWbOwxknZjt04+8fbZ5erd011WrSOvgTVx" +
                "xVhZWhOuct0aH6utb4nFXkftNytrPrax91xB563o4+xy9e7pha" +
                "KzO1Gt18CauGLlyZ7ToIPr1njOVe77U9/3Z89mz8RKrz6Zmwcx" +
                "irA5+n2k57ScFo2ZvS5DoE5U6jlYFTOzYq4IY1J1cSZT7Ksddk" +
                "7PDq/KOZ093f6czp5uck6//rj838jyO+pVfoZS9vRlPJcqz1DG" +
                "tfnj+WOx0qtP5uxRDI793HuN3xCRzWf2qpjT0IY1nF/FjBaN+n" +
                "IVoXpEY0beJcsz7L1/8sP1ee+ffL/9e3/2m1jsdaQWsWZj77ly" +
                "K/kIvS5T7NV5tVxVzIW60npMS2qnOCPnKveoqe9R3Yfd24u73V" +
                "urPe3WjN27w/a0hz3sYHcw3b2ge+eS9cMR3HfW/VY7Uc7p5N+l" +
                "/gPEb6Zg");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value24 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value24[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value25 = null;

    protected static void value25Init()
    {
        try
        {
            final int rows = 57;
            final int cols = 85;
            final int compressedBytes = 1534;
            final int uncompressedBytes = 19381;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtWb1unFUQXTfkJdIkokKISIAoEAX48wZRIUGbJi7gMYhXdr" +
                "QycRFbQjxBGhJeBUXyC9DQIT9AmrA7O3t+vrlrYq/yA/ezfO+d" +
                "M2fOnLmxs2t7Mpl9PvtiMpl9MBsmq+fgcPbJ5BWe2cfNzNcj5L" +
                "PJlZ/ZNxZ/dAn/y1fu8OkI2b2S036n/U77nfY77XdKz+Ef/U63" +
                "eaeHf13963R4ODyMNfbEIgainGQgVpwrK02uVlX2xQw9q3rNRN" +
                "bdqFs/o6aayzvBMU87fg4OJ/25xjM8Hh7HGntiEQNRTjIQK86V" +
                "lSZXqyr7YoaeVb1mIutu1K2fUVPN5Z3gmKft/5/21/3+uv+/fN" +
                "0/Go5ijT2xiIEoJxmIFefKSpOrVZV9MUPPql4zkXU36tbPqKnm" +
                "8k5wTPovhheje15igWMFprjnUzHXsX7iyW4zvFew1Ykz2aP75R" +
                "rfWz7GNW0sNPZu7912XmCBYwUGJGPNs2Kln3hqqOq4kj0ov2Iq" +
                "u1LmGHvL6bimja1mmu/NY419mZlnDEQ5yUCsOFey5qqzVGvntT" +
                "thqE91yhruypXdsU+kNdVc3gmOoT+9P70fa+yLTER5ZiQ5euaY" +
                "0ayEAjCuYH7NUJ/ggsdZ7Yhq9deaSN0rWzv6LWX17p3dO7HGvs" +
                "hElGdGkqNnjhnNSigA4wrm1wz1CS54nNWOqFZ/rYnUvbK1o98S" +
                "+vT3Utt/f7r+N39vfKqyiCreJo1NGdffpN1iLk6bp/Dc5i4tF6" +
                "i+TGd3Z3yqsogq3iaNTRnX36TdYi5Om6fw3OYuLReornWGG8ON" +
                "WGNPLGIgykkGYsW5kjXRE9XamX2BoT7VKWu4K1d2xz6R1lRzeS" +
                "c45mn77/pex9PvdPvPV3++eYW3YcKrTrG3s7dTY4FjBaa451Mx" +
                "172dVt9ktxneK9jqxJns0f1yje8tH+OaNtbW+PnX/r267aff6f" +
                "afRz/0O9j6nf7Y72Dbz8O/+x1c55k+nz6PdbEDyxhrYuOzfubK" +
                "+tpBFaDkvsBQn+rUtbiPe3LX1UTqXtnuQ9nr7Pn0PNbFvsqcI8" +
                "aa2Pisn7lCC5qJsQKUxN05M9SnOnUt7uOe3HU1kbpXtvtQNqr7" +
                "7/pav+tbIv1v0Vu90+v8/nT4MFbdGUkOuFjHu1Z6rzHm3ePjMs" +
                "e1W/bgnnxinaz2pFmfO+u9V/867d/778Kd/vM1+36sujOSHHCx" +
                "jnet9F5jzLvHx2WOa7fswT35xDpZ7UmzPnfWe6/+dbrt16jpk+" +
                "mTWGNPLGJHkqNnjhmFPhhjNe7MrlwTbHDB46x2RLX6a02k7pWt" +
                "Hf2W0OfuTb/nuzcXWOBYgSnOec7VEfdQjdqF6kSVVyiC84NbLe" +
                "0xu9L9N497bE/z3/i5+8Gtt8fLu/L3qKODS/I/vSln/TVq2++l" +
                "hvkwjzX2xCIGopxkIFacKytNrlZV9sUMPat6zUTW3ahbP6Omms" +
                "s7DfPiNs+Gs1hjX2bOMgainGQgVpwrK02uVtW1O2PoWdVrJrLu" +
                "Rt36GTXVXN4Jjkn/ZDiJNfZl5iRjIMpJBmLFubLS5GpVXbszhp" +
                "5VvWYi627UrZ9RU83lneCY9J8Nz2KNfZl5ljEQ5SQDseJcWWly" +
                "tXZeuxOG+lSnrOGuXNkd+0RaU83lneCY9E+H01hjX2ZOMwainG" +
                "QgVpwrK02uVtW1O2PoWdVrJrLuRt36GTXVXN4Jjkn/eDiONfZl" +
                "5jhjIMpJBmLFubLS5GpVXbszhp5VvWYi627UrZ9RU83lneCY9J" +
                "8OT2ONfZl5mjEQ5SQDseJcWWlytXZeuxOG+lSnrOGuXNkd+0Ra" +
                "U83lneCYp129D/sO5+Pf8/Tol1d4b/f9qvq31/4O+9u37O+m96" +
                "b3Yo09sYgdSY6eOWYU+mCM1bgzu3JNsMEFj7PaEdXqrzWRule2" +
                "dvRbWlfvT/djjX2Z2c/YkeTomWNGsxIKwLiC+TVDfYILHme1I6" +
                "rVX2sida9s7ei3tK6+mF7EGvsyc5ExEOXoWT9zXXW5YM3EWIE7" +
                "ax0Y6hNc19C8zqRIeyJ1r2x1r1542v7z/tb/bvoS+YThCQ==");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value25 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value25[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value26 = null;

    protected static void value26Init()
    {
        try
        {
            final int rows = 56;
            final int cols = 85;
            final int compressedBytes = 1609;
            final int uncompressedBytes = 19041;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtWUtuXFUQ9YgdxEsIEgMGgBggBoibJ8QikkmWkAWgtMIgSS" +
                "seJfFvAMqAqb2EDBHDyL8pK/ASSL/qeufzylhJWsLIt62uW59T" +
                "p05du9u/ra35Y/nbVn9s+LH8vd/Bph+Pn/w/dP76+Jr6L/+Nru" +
                "Hd8C7s6kQuY9jMzX19pmV+naAMYHJdQKhOVepcPMc1uepqI1Wv" +
                "aNehaO+W1/4f/bV6W1/7N/nR7oTVM71nD1cWObfzk7nms+Y5nx" +
                "4f1ylud558NlfLGlyTb5w8yC/euibV4Xtnv89afLf4/r39YtHw" +
                "dbr4+kM+J4uvrqz8NMt8+/Gf+8XPFn95Df6HD57wzSzz40cp7X" +
                "e64Tsd3gxvwsaZuYg9kxj1OeYs+IGYs/FkVuWcQAMLHFd1IrpV" +
                "31UbqXpF60S/pey+9+jeo7BxrioRpZ+WMYlArHnuZM6YrN06OW" +
                "9LEapTlTKHq3JmV+wbaU+1l0+CYt62v/b7++nNv9P2sr0MG2fm" +
                "IkZGMYlArHnurDi5W1lZFyPUV/YaiaqrUbXuo6fayydBMfE/bU" +
                "/DxjlWnmaMjGISgVjz3FlxcreyTuoMob6y10hUXY2qdR891V4+" +
                "CYqJf7/th41zrOxnjIxiEoFY89xZcXK3sk7qDKG+stdIVF2Nqn" +
                "UfPdVePgmKif+4HYeNc6wcZ4yMYhKBWPPcWXFyt06e1AlCdapS" +
                "5nBVzuyKfSPtqfbySVDM2/bvURv/HvW6vQ4bZ+YiRkYxiUCsee" +
                "6sOLlbWVkXI9RX9hqJqqtRte6jp9rLJ0Ex8R+1o7BxjpWjjJFR" +
                "TCIQa547K07u1smTOkGoTlXKHK7KmV2xb6Q91V4+CYp52/7a3/" +
                "jv+/eH+2HjzFzEnkmM+hxzFvxAzNl4MqtyTqCBBY6rOhHdqu+q" +
                "jVS9onWi39LUfTlcho1zrFxmjIxi1Ndn2vWUS+bMHDPwZO0DQn" +
                "UC6xxa1500c/VGql7Rql618LbF/07+7H+n7/87ufmP5V/9Dj7t" +
                "0b/v97/19Tu9nXf6/qf/u2H1TO/Zw5VFzu38ZK75rHnOp8fHdY" +
                "rb3fF/0aaWNbgm3zh56AbfuibV4Xtn/3zbth1WT84kBljY+amd" +
                "dhvb85xPj49/vdNtV50ea3BNvrFuVmvSqu+d/ZZ/1V6FjTNzES" +
                "OjmEQg1jx3VpzcrayTekOor+w1ElVXo2rdR0+1l0+CYt62v5/2" +
                "71H9Tvud9jvdyN/6HgwPwsaZuYg9kxj1OeYs+IGYs/FkVuWcQA" +
                "MLHFd1IrpV31UbqXpF60S/Jcwpft//u//G/qmP9nlYPTmTGGBh" +
                "56d2+qx5zqfHx3WKa7WswTX5xrpZrUmrvnf2+6z+ftq/R/U7vY" +
                "132l60F2HjzFzEyCgmEYg1z50VJ3crK+tihPrKXiNRdTWq1n30" +
                "VHv5JCjmbfvX6ca/Tp+352HjzFzEyCgmEYg1z50VJ3crK+tihP" +
                "rKXiNRdTWq1n30VHv5JCgG/3A2nIVdneufXM8Qw2Zu7uszLbjA" +
                "mTlmAJP81H/GCNWpSp2L57gmV11tpOoV7ToUPVVPhpOwq3NdOU" +
                "EMm7m5r8+04AJn5pgBTKLuhBGqU5U6F89xTa662kjVK9p1KDqr" +
                "7aAdhI1z/Ao+yBgZxSQCsea5s+LkbmWdXkWGUF/ZaySqrkbVuo" +
                "+eai+fBMXEv9N2wsY5VnYyRkYxiUCsee6sOLlbWSd1hlBf2Wsk" +
                "qq5G1bqPnmovnwTFxL/bdsPGOVZ2M0ZGMYlArHnurDi5W1kndY" +
                "ZQX9lrJKquRtW6j55qL58ExcS/bMuwcY6VZcbIKCYRiDXPnRUn" +
                "dyvrpM4Q6it7jUTV1aha99FT7eWToBj8w8VwEXZ1rt9pLxDDZm" +
                "7u6zMtuMCZOWYAk7zbXzBCdapS5+I5rslVVxupekW7DkVP1dPh" +
                "NOzqXFdOEcNmbu7rMy24wJk5ZgCTqDtlhOpUpc7Fc1yTq642Uv" +
                "WKdh2Knqrnw3nY1bmunCOGzdzc12dacIEzc8wAJlF3zgjVqUqd" +
                "i+e4JlddbaTqFe06FJ3VdtgOw8Y5viscZoyMYhKBWPPcWXFyt7" +
                "JO70yGUF/ZaySqrkbVuo+eai+fBMXEv9f2wsY5VvYyRkYxiUCs" +
                "ee6sOLlbWSd1hlBf2Wskqq5G1bqPnmovnwTFtO0/LNZHdw==");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value26 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value26[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int lookupValue(int row, int col)
    {
        if (row <= 56)
            return value[row][col];
        else if (row >= 57 && row <= 113)
            return value1[row-57][col];
        else if (row >= 114 && row <= 170)
            return value2[row-114][col];
        else if (row >= 171 && row <= 227)
            return value3[row-171][col];
        else if (row >= 228 && row <= 284)
            return value4[row-228][col];
        else if (row >= 285 && row <= 341)
            return value5[row-285][col];
        else if (row >= 342 && row <= 398)
            return value6[row-342][col];
        else if (row >= 399 && row <= 455)
            return value7[row-399][col];
        else if (row >= 456 && row <= 512)
            return value8[row-456][col];
        else if (row >= 513 && row <= 569)
            return value9[row-513][col];
        else if (row >= 570 && row <= 626)
            return value10[row-570][col];
        else if (row >= 627 && row <= 683)
            return value11[row-627][col];
        else if (row >= 684 && row <= 740)
            return value12[row-684][col];
        else if (row >= 741 && row <= 797)
            return value13[row-741][col];
        else if (row >= 798 && row <= 854)
            return value14[row-798][col];
        else if (row >= 855 && row <= 911)
            return value15[row-855][col];
        else if (row >= 912 && row <= 968)
            return value16[row-912][col];
        else if (row >= 969 && row <= 1025)
            return value17[row-969][col];
        else if (row >= 1026 && row <= 1082)
            return value18[row-1026][col];
        else if (row >= 1083 && row <= 1139)
            return value19[row-1083][col];
        else if (row >= 1140 && row <= 1196)
            return value20[row-1140][col];
        else if (row >= 1197 && row <= 1253)
            return value21[row-1197][col];
        else if (row >= 1254 && row <= 1310)
            return value22[row-1254][col];
        else if (row >= 1311 && row <= 1367)
            return value23[row-1311][col];
        else if (row >= 1368 && row <= 1424)
            return value24[row-1368][col];
        else if (row >= 1425 && row <= 1481)
            return value25[row-1425][col];
        else if (row >= 1482)
            return value26[row-1482][col];
        else
            throw new IllegalArgumentException("Unexpected location requested in value26 lookup");
    }

    static
    {
        sigmapInit();
        sigmap1Init();
        sigmap2Init();
        sigmap3Init();
        valueInit();
        value1Init();
        value2Init();
        value3Init();
        value4Init();
        value5Init();
        value6Init();
        value7Init();
        value8Init();
        value9Init();
        value10Init();
        value11Init();
        value12Init();
        value13Init();
        value14Init();
        value15Init();
        value16Init();
        value17Init();
        value18Init();
        value19Init();
        value20Init();
        value21Init();
        value22Init();
        value23Init();
        value24Init();
        value25Init();
        value26Init();
    }
    }

    /**
     * The GOTO table.
     * <p>
     * The GOTO table maps a state and a nonterminal to a new state.
     * It is used when the parser reduces.  Suppose, for example, the parser
     * is reducing by the production <code>A ::= B C D</code>.  Then it
     * will pop three symbols from the <code>stateStack</code> and three symbols
     * from the <code>valueStack</code>.  It will look at the value now on top
     * of the state stack (call it <i>n</i>), and look up the entry for
     * <i>n</i> and <code>A</code> in the GOTO table to determine what state
     * it should transition to.
     */
    protected static final class GoToTable
    {
        /**
         * Returns the state the parser should transition to if the given
         * state is on top of the <code>stateStack</code> after popping
         * symbols corresponding to the right-hand side of the given production.
         *
         * @return the state to transition to (0 <= result < Parser.NUM_STATES)
         */
        protected static int getGoTo(int state, Nonterminal nonterminal)
        {
            assert 0 <= state && state < Parser.NUM_STATES;
            assert nonterminal != null;

            return get(state, nonterminal.getIndex());
        }

        protected static final int[] rowmap = { 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0, 5, 0, 0, 6, 0, 0, 0, 7, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 9, 0, 0, 0, 0, 10, 0, 0, 0, 0, 0, 11, 12, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 13, 14, 0, 15, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 17, 0, 18, 0, 0, 19, 0, 0, 20, 0, 0, 0, 21, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 22, 0, 23, 0, 24, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 25, 0, 0, 2, 26, 0, 0, 0, 3, 27, 0, 0, 4, 0, 0, 28, 0, 0, 0, 29, 0, 30, 0, 31, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 32, 0, 0, 5, 33, 34, 0, 0, 35, 6, 0, 36, 0, 0, 7, 37, 0, 0, 0, 0, 0, 0, 0, 38, 4, 0, 39, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 5, 0, 0, 40, 0, 0, 0, 0, 0, 41, 42, 8, 0, 43, 9, 0, 0, 0, 44, 45, 0, 46, 0, 0, 47, 0, 10, 0, 48, 0, 11, 49, 12, 0, 50, 0, 0, 0, 51, 52, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 53, 10, 0, 0, 0, 0, 0, 0, 0, 54, 1, 12, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 13, 0, 0, 0, 0, 1, 0, 0, 13, 0, 0, 0, 0, 0, 0, 0, 0, 2, 1, 0, 0, 0, 2, 0, 14, 15, 0, 0, 0, 55, 0, 2, 0, 0, 16, 17, 0, 3, 0, 3, 3, 0, 0, 1, 0, 18, 14, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 19, 0, 0, 0, 56, 0, 0, 0, 20, 57, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 58, 2, 0, 0, 0, 0, 0, 3, 0, 0, 15, 0, 0, 0, 59, 60, 0, 0, 0, 61, 21, 0, 0, 0, 0, 4, 0, 5, 0, 0, 0, 0, 0, 6, 62, 0, 63, 22, 0, 0, 0, 0, 7, 0, 0, 0, 8, 0, 0, 0, 0, 64, 0, 23, 0, 9, 0, 0, 10, 1, 0, 0, 0, 65, 0, 0, 0, 0, 0, 1, 0, 0, 0, 11, 0, 2, 0, 0, 0, 0, 0, 0, 12, 0, 13, 0, 0, 0, 66, 16, 0, 67, 0, 0, 0, 0, 68, 0, 0, 69, 70, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 14, 0, 0, 71, 17, 0, 0, 18, 0, 0, 72, 19, 0, 0, 0, 0, 0, 24, 25, 26, 1, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 27, 0, 0, 28, 1, 0, 0, 0, 0, 3, 4, 0, 0, 0, 29, 30, 0, 0, 0, 0, 0, 31, 0, 0, 0, 0, 0, 32, 2, 0, 0, 0, 0, 0, 0, 0, 0, 33, 0, 0, 0, 34, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 35, 36, 0, 0, 37, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 38, 0, 0, 0, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15, 0, 39, 16, 40, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 41, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 42, 1, 0, 0, 0, 1, 6, 0, 0, 5, 43, 7, 0, 0, 0, 44, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 73, 45, 0, 46, 0, 47, 48, 49, 50, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 51, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 4, 0, 52, 0, 1, 55, 0, 0, 0, 8, 56, 0, 57, 0, 58, 0, 0, 0, 6, 7, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 59, 60, 4, 0, 0, 0, 1, 0, 0, 3, 0, 8, 61, 62, 0, 0, 0, 0, 9, 1, 0, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 63, 0, 0, 0, 0, 74, 0, 0, 0, 64, 0, 65, 0, 0, 0, 0, 0, 0, 0, 0, 0, 66, 67, 17, 18, 0, 19, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 75, 0, 0, 0, 0, 20, 0, 0, 68, 0, 21, 0, 0, 0, 0, 0, 0, 22, 0, 0, 0, 0, 0, 23, 24, 0, 0, 0, 0, 0, 0, 69, 25, 26, 0, 0, 0, 70, 71, 0, 0, 0, 4, 0, 72, 0, 5, 0, 0, 76, 73, 1, 0, 0, 0, 27, 74, 0, 0, 0, 28, 0, 0, 0, 0, 29, 0, 1, 0, 77, 0, 0, 0, 0, 0, 0, 78, 0, 0, 6, 0, 10, 0, 0, 0, 0, 0, 0, 0, 21, 30, 0, 0, 0, 0, 0, 31, 0, 0, 0, 0, 1, 0, 0, 0, 11, 0, 75, 76, 12, 0, 79, 77, 0, 0, 0, 0, 1, 0, 0, 0, 2, 0, 3, 0, 0, 78, 0, 13, 79, 80, 81, 82, 0, 83, 80, 84, 1, 85, 0, 81, 86, 87, 88, 82, 14, 2, 15, 0, 0, 0, 89, 90, 0, 0, 0, 0, 91, 0, 92, 0, 93, 94, 0, 95, 96, 9, 0, 0, 2, 0, 97, 0, 0, 98, 1, 0, 99, 3, 0, 0, 0, 0, 0, 100, 0, 0, 0, 0, 0, 0, 101, 0, 102, 0, 0, 0, 0, 0, 0, 2, 0, 103, 104, 0, 3, 4, 0, 0, 0, 105, 1, 106, 0, 0, 0, 107, 108, 5, 0, 0, 0, 0, 0, 0, 0, 10, 0, 4, 109, 5, 1, 0, 0, 0, 0, 1, 110, 111, 0, 0, 4, 112, 113, 0, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 2, 0, 0, 114, 0, 0, 0, 0, 1, 2, 0, 2, 0, 3, 0, 0, 0, 0, 0, 23, 0, 0, 6, 0, 16, 0, 115, 17, 1, 2, 0, 0, 1, 0, 0, 0, 3, 0, 4, 0, 0, 0, 0, 18, 0, 0, 19, 0, 0, 0, 0, 0, 116, 7, 0, 117, 118, 0, 11, 0, 0, 0, 12, 119, 0, 0, 0, 20, 0, 2, 0, 0, 7, 0, 0, 0, 4, 0, 0, 120, 121, 0, 122, 123, 0, 8, 5, 0, 0, 0, 124, 125, 0, 5, 0, 0, 0, 0, 0, 126, 0, 0, 0, 127, 128, 129, 0, 8, 0, 130, 0, 13, 9, 0, 0, 2, 0, 131, 0, 3, 2, 132, 0, 14, 0, 133, 0, 0, 0, 15, 10, 0, 0, 0, 0, 83, 0, 1, 0, 0, 2, 0, 21, 0, 0, 0, 22, 0, 134, 135, 0, 136, 137, 138, 0, 139, 0, 0, 0, 140, 0, 0, 0, 0, 1, 23, 24, 25, 26, 27, 28, 29, 141, 30, 84, 31, 32, 33, 34, 35, 36, 37, 38, 39, 0, 40, 0, 41, 42, 43, 0, 44, 45, 142, 46, 47, 48, 49, 143, 50, 51, 52, 53, 54, 55, 0, 5, 58, 1, 0, 2, 0, 6, 0, 0, 0, 0, 0, 0, 144, 145, 146, 0, 147, 0, 59, 4, 86, 0, 148, 7, 0, 0, 149, 150, 0, 0, 11, 60, 151, 152, 153, 154, 155, 87, 156, 0, 157, 158, 159, 160, 161, 162, 163, 61, 164, 0, 165, 166, 167, 168, 0, 0, 7, 0, 0, 0, 0, 62, 0, 0, 0, 0, 169, 0, 170, 0, 0, 0, 0, 1, 0, 2, 171, 172, 0, 0, 173, 0, 174, 12, 0, 0, 0, 175, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 12, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9, 16, 0, 0, 17, 0, 18, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 176, 177, 2, 0, 1, 0, 1, 0, 3, 0, 0, 0, 0, 88, 0, 0, 0, 0, 0, 89, 0, 13, 0, 0, 0, 178, 2, 0, 3, 0, 0, 0, 14, 0, 179, 0, 0, 0, 0, 0, 0, 32, 0, 0, 0, 0, 0, 0, 0, 0, 24, 0, 0, 180, 0, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 33, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 181, 0, 182, 19, 0, 0, 0, 0, 4, 0, 5, 6, 0, 0, 1, 0, 7, 0, 0, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 13, 14, 0, 0, 0, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 183, 0, 184, 185, 186, 0, 2, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 34, 0, 0, 187, 0, 188, 189, 0, 20, 0, 21, 0, 6, 0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 190, 0, 0, 0, 0, 0, 0, 17, 9, 10, 0, 11, 0, 12, 0, 0, 0, 0, 0, 13, 0, 14, 0, 0, 0, 0, 0, 191, 0, 0, 192, 0, 0, 0, 193, 22, 0, 0, 0, 0, 23, 194, 24, 18, 0, 0, 0, 0, 0, 0, 195, 0, 0, 1, 0, 0, 19, 196, 0, 3, 0, 7, 15, 0, 1, 0, 0, 0, 1, 0, 197, 25, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 63, 0, 0, 198, 0, 199, 0, 200, 20, 0, 0, 201, 0, 202, 0, 0, 21, 0, 0, 0, 90, 0, 26, 0, 203, 0, 0, 0, 0, 0, 204, 0, 22, 0, 0, 0, 16, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 205, 0, 0, 0, 0, 0, 0, 0, 0, 0, 91, 0, 0, 0, 0, 0, 23, 0, 0, 0, 0, 0, 0, 0, 5, 0, 6, 0, 7, 2, 0, 0, 0, 0, 0, 1, 206, 207, 2, 3, 0, 0, 0, 0, 0, 0, 208, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 209, 0, 0, 0, 210, 64, 0, 211, 0, 3, 0, 0, 0, 65, 92, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 24, 0, 0, 0, 0, 0, 25, 0, 0, 0, 27, 212, 0, 213, 26, 28, 0, 214, 215, 0, 27, 216, 0, 217, 218, 219, 0, 220, 29, 221, 28, 222, 223, 224, 29, 225, 0, 226, 227, 6, 228, 229, 30, 0, 230, 231, 0, 0, 0, 0, 0, 66, 0, 2, 232, 0, 0, 0, 233, 0, 234, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 30, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 17, 235, 31, 0, 0, 0, 0, 18, 19, 20, 21, 22, 0, 23, 236, 0, 24, 25, 31, 26, 27, 0, 28, 0, 29, 30, 31, 32, 33, 0, 67, 68, 0, 0, 0, 237, 4, 0, 0, 0, 0, 0, 0, 32, 0, 0, 0, 238, 239, 1, 0, 7, 0, 18, 0, 0, 0, 0, 0, 0, 0, 0, 240, 69, 0, 0, 241, 0, 0, 242, 243, 0, 0, 0, 8, 0, 33, 34, 0, 0, 3, 0, 0, 244, 0, 245, 0, 93, 246, 0, 247, 0, 0, 35, 0, 0, 0, 248, 0, 249, 36, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 37, 0, 0, 0, 0, 0, 0, 0, 0, 0, 35, 0, 0, 0, 32, 33, 34, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 35, 0, 0, 0, 4, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 20, 0, 250, 0, 251, 0, 1, 38, 0, 0, 0, 0, 0, 0, 0, 21, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 39, 0, 0, 0, 0, 7, 0, 0, 0, 0, 40, 0, 0, 0, 0, 36, 0, 0, 0, 0, 0, 0, 0, 0, 252, 37, 253, 254, 38, 255, 0, 256, 39, 257, 0, 41, 0, 258, 0, 40, 259, 41, 0, 0, 0, 0, 0, 260, 0, 261, 42, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 262, 263, 0, 0, 264, 0, 8, 0, 43, 0, 0, 265, 266, 0, 0, 0, 0, 0, 0, 0, 0, 2, 3, 0, 23, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 267, 268, 269, 270, 271, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 272, 0, 0, 0, 0, 273, 44, 11, 0, 0, 12, 0, 13, 5, 0, 0, 0, 42, 0, 0, 0, 0, 0, 0, 70, 0, 0, 274, 0, 0, 275, 0, 2, 0, 276, 14, 3, 0, 0, 0, 277, 0, 0, 0, 0, 43, 45, 0, 0, 278, 279, 280, 0, 46, 281, 0, 282, 47, 48, 0, 0, 8, 283, 0, 2, 284, 285, 0, 0, 0, 8, 49, 286, 0, 287, 50, 288, 0, 0, 51, 0, 3, 289, 290, 0, 291, 0, 0, 0, 0, 0, 0, 0, 52, 0, 292, 293, 0, 0, 53, 0, 0, 294, 0, 0, 0, 0, 295, 1, 0, 0, 0, 5, 2, 0, 0, 296, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 44, 297, 45, 0, 0, 0, 0, 0, 71, 0, 0, 54, 0, 0, 0, 0, 0, 0, 0, 0, 298, 0, 0, 0, 0, 0, 0, 0, 10, 0, 0, 1, 0, 0, 2, 0, 299, 46, 0, 0, 0, 300, 0, 0, 0, 0, 0, 0, 0, 301, 0, 0, 0, 0, 0, 55, 0, 0, 56, 0, 302, 0, 0, 0, 0, 0, 0, 57, 0, 0, 0, 0, 0, 36, 0, 0, 0, 37, 5, 303, 6, 304, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 24, 0, 0, 0, 0, 0, 4, 0, 0, 0, 2, 0, 305, 306, 3, 0, 0, 0, 0, 0, 0, 0, 0, 25, 0, 0, 0, 0, 0, 0, 0, 307, 0, 308, 0, 309, 0, 0, 310, 0, 0, 0, 311, 0, 0, 58, 312, 0, 0, 0, 0, 0, 313, 0, 0, 7, 314, 0, 0, 0, 315, 316, 0, 47, 317, 0, 0, 0, 59, 94, 0, 0, 0, 318, 319, 60, 0, 61, 0, 2, 26, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0, 95, 0, 0, 0, 3, 48, 62, 0, 0, 0, 63, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 320, 0, 49, 321, 50, 72, 0, 51, 0, 322, 0, 0, 0, 323, 324, 0, 15, 0, 0, 0, 0, 0, 325, 326, 64, 0, 0, 327, 65, 66, 0, 52, 0, 328, 67, 329, 0, 68, 53, 330, 331, 69, 70, 0, 54, 0, 332, 333, 0, 71, 55, 334, 0, 56, 0, 0, 0, 72, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9, 0, 10, 335, 0, 9, 336, 0, 0, 337, 338, 339, 73, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 57, 0, 0, 58, 59, 340, 74, 0, 0, 0, 0, 75, 0, 0, 0, 38, 0, 0, 0, 0, 0, 341, 60, 342, 61, 0, 0, 6, 0, 1, 0, 0, 0, 0, 0, 27, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 343, 344, 0, 345, 0, 0, 28, 0, 0, 0, 346, 0, 0, 0, 0, 0, 347, 0, 62, 348, 63, 0, 64, 349, 350, 0, 0, 65, 351, 0, 66, 0, 0, 76, 0, 0, 352, 353, 0, 0, 77, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 354, 355, 96, 0, 356, 0, 0, 0, 357, 0, 0, 0, 78, 0, 0, 4, 5, 0, 0, 6, 0, 0, 0, 0, 0, 67, 0, 79, 0, 358, 0, 80, 68, 359, 0, 360, 361, 362, 81, 82, 0, 363, 83, 69, 364, 0, 365, 366, 367, 84, 0, 0, 0, 0, 368, 0, 0, 0, 0, 3, 0, 7, 0, 0, 34, 8, 0, 1, 0, 0, 0, 0, 0, 0, 70, 369, 0, 71, 0, 0, 0, 85, 0, 0, 3, 0, 0, 0, 370, 0, 371, 86, 372, 0, 0, 0, 0, 0, 72, 73, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 373, 1, 0, 4, 0, 5, 0, 0, 6, 0, 0, 0, 0, 0, 87, 74, 75, 374, 76, 0, 88, 89, 77, 0, 78, 375, 0, 376, 377, 0, 0, 378, 379, 0, 0, 0, 7, 0, 97, 90, 0, 0, 380, 0, 381, 0, 0, 7, 0, 16, 0, 0, 382, 383, 384, 0, 91, 385, 386, 387, 388, 92, 93, 0, 0, 0, 389, 0, 0, 390, 391, 392, 94, 95, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 79, 0, 80, 393, 0, 0, 0, 0, 0, 0, 394, 0, 395, 0, 0, 96, 0, 2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 97, 0, 0, 6, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 396, 397, 0, 0, 398, 399, 0, 400, 0, 0, 0, 0, 98, 99, 0, 0, 0, 98, 99, 0, 100, 401, 0, 0, 0, 101, 102, 402, 0, 103, 104, 0, 0, 0, 0, 81, 0, 0, 105, 0, 0, 0, 0, 82, 0, 0, 9, 0, 0, 0, 0, 0, 0, 0, 403, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 404, 0, 0, 0, 0, 0, 0, 0, 405, 106, 0, 83, 107, 108, 0, 84, 406, 407, 0, 0, 0, 408, 0, 8, 0, 409, 0, 109, 0, 0, 85, 0, 410, 0, 0, 86, 0, 411, 0, 0, 0, 0, 0, 0, 0, 0, 87, 0, 0, 0, 0, 0, 0, 7, 0, 0, 412, 0, 0, 0, 413, 0, 88, 414, 0, 415, 0, 89, 0, 110, 111, 112, 0, 113, 0, 416, 0, 114, 417, 418, 0, 115, 419, 0, 0, 0, 90, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 116, 117, 118, 0, 420, 0, 421, 0, 0, 119, 422, 0, 120, 121, 423, 0, 122, 0, 37, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 123, 124, 0, 125, 0, 0, 126, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    protected static final int[] columnmap = { 0, 1, 2, 0, 0, 3, 0, 4, 5, 2, 6, 3, 1, 1, 7, 3, 8, 9, 10, 1, 7, 0, 6, 0, 2, 11, 12, 1, 7, 0, 13, 0, 0, 13, 14, 15, 4, 14, 0, 16, 2, 1, 3, 3, 7, 0, 17, 18, 19, 20, 6, 13, 21, 14, 14, 14, 22, 14, 3, 19, 19, 6, 21, 4, 23, 24, 25, 26, 1, 8, 27, 28, 2, 29, 30, 1, 31, 32, 0, 1, 24, 33, 5, 1, 34, 0, 23, 35, 1, 36, 1, 1, 2, 1, 0, 5, 37, 38, 13, 2, 39, 40, 4, 1, 41, 1, 2, 9, 42, 43, 3, 44, 45, 7, 46, 47, 2, 48, 1, 49, 0, 2, 1, 50, 51, 4, 3, 52, 5, 53, 54, 55, 56, 14, 1, 3, 0, 57, 58, 8, 0, 5, 3, 59, 1, 60, 61, 21, 4, 62, 63, 64, 65, 1, 24, 10, 66, 67, 68, 69, 19, 70, 20, 71, 5, 72, 6, 73, 6, 74, 75, 76, 0, 0, 3, 27, 77, 8, 78, 79, 80, 16, 6, 81, 82, 27, 83, 84, 0, 85, 86, 3, 6, 9, 2, 87, 1, 88, 0, 89, 2, 90, 1, 91, 1, 92, 93, 94, 95, 18, 96, 97, 98, 99, 3, 100, 101, 3, 11, 102, 12, 1, 103, 104, 105, 106, 23, 3, 3, 107, 108, 109, 0, 110, 111, 4, 112, 0, 113, 20, 14, 29, 3, 29, 114, 115, 3, 5, 116, 3, 1, 4, 117, 21, 11, 118, 119, 1, 120, 9, 121, 122, 123, 124, 125, 126, 127, 3, 31, 24, 128, 7, 1, 1, 129, 130, 2, 31, 1, 4, 5, 131, 10, 2, 12, 132, 32, 133, 134, 135, 15, 136, 1, 24, 3, 137, 13, 1, 138, 5, 17, 5, 4, 139, 6, 15, 4, 140, 141, 142, 23, 31, 14, 2, 20, 143, 1, 4, 144, 145, 33, 146, 7, 147, 148, 5, 149, 150, 151, 152, 153, 154, 35, 37, 155, 156, 7, 8, 157, 38, 33, 10, 158, 159, 9, 160, 6, 5, 161, 162, 163, 164, 165, 7, 166, 3, 167, 168, 169, 41, 21, 170, 171, 172, 33, 173, 2, 6, 16, 174, 175, 3, 42, 176, 177, 0, 178, 179, 180, 45, 35, 46, 181, 182, 2, 183, 58, 6, 8, 184, 185, 31, 47, 186, 187, 188, 189, 190, 191, 49, 0, 192, 193, 7, 6, 0, 21, 194, 195, 196, 35, 197, 198, 9, 1, 199, 200, 201, 20, 2, 5, 19, 202, 26, 18, 203, 2, 20, 25, 20, 3, 3, 1, 40, 204, 10, 205, 206, 2, 7, 10, 207, 208, 209, 8, 11, 210, 211, 48, 37, 212, 213, 2, 0, 214, 215, 216, 42, 16, 4, 13, 8, 1, 0, 217, 32, 9, 218, 219, 6, 220, 221, 54, 222, 38, 223, 224, 225, 1, 226, 227, 228, 5, 41, 59, 3, 11, 229, 11, 10, 230, 231, 13, 232, 233, 33, 234, 60, 235, 236, 237, 1, 12, 238, 239, 240, 241, 242, 3 };

    public static int get(int row, int col)
    {
        if (isErrorEntry(row, col))
            return -1;
        else if (columnmap[col] % 2 == 0)
            return lookupValue(rowmap[row], columnmap[col]/2) >>> 16;
        else
            return lookupValue(rowmap[row], columnmap[col]/2) & 0xFFFF;
    }

    protected static boolean isErrorEntry(int row, int col)
    {
        final int INT_BITS = 32;
        int sigmapRow = row;

        int sigmapCol = col / INT_BITS;
        int bitNumberFromLeft = col % INT_BITS;
        int sigmapMask = 0x1 << (INT_BITS - bitNumberFromLeft - 1);

        return (lookupSigmap(sigmapRow, sigmapCol) & sigmapMask) == 0;
    }

    protected static int[][] sigmap = null;

    protected static void sigmapInit()
    {
        try
        {
            final int rows = 609;
            final int cols = 16;
            final int compressedBytes = 1625;
            final int uncompressedBytes = 38977;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtXM1u3DYQHjLcDZsaDePYhdMT4+aQoxv0AcYBXOylgI/ubQ" +
                "2k9z4CW6CFgT5Bb84b9BHsWx4jufUxKml3tdKK+qckSprvkl15" +
                "FYrkN9/MkEPdms/rF0+u4V8t8ehPeHWDT7nS9+9OxQ+wWMFXz9" +
                "/rK9Bn6mZ9fvXXhzdf8B2uOL6F7+HvXx6//WcNBA+AmSvCZC6L" +
                "xOfwr8x6I4EQQEIRO3g+8XrGa6OBBUxWgX7JBZwpWIKC+5cqJD" +
                "zCEwH6Z5AruF6fwxKFhnNYaVwF/3x99givSL9GjtvIfwFchP5r" +
                "EfqvZei/jl9s/dc3EPgvkCqYfxb4r4+h/4KN/7p5PCH/hfiwVo" +
                "E56+Bj4CAYYmDcwRcmLgBWIJ+FNi4CkwLNzBv9O6rQ5AOb0wbh" +
                "xKAPPdh+uEuJU9AZiVsd20BVcpwjw07/TkL9O4317zjo7NtA/5" +
                "gI+S+ljvTvYaN/sJqO/iE8fD7jGPN3hXAMWsBC/Bby9+jZZo5D" +
                "/i52/GUhf6W59IO/s9fvD3n6LcL8I9LvO7t+U/5R6v+ep/3fFx" +
                "q/zvTnUgAe6o/M1R/I6o9KOykReycVejOzi8uj6zpqXJF+1XDl" +
                "UtEoEQgEAmFK8V/J+n2QFgbx3yJv/Z5BTpKM6U98t4R8uYk/1t" +
                "A+/vjV/Lcuj/9lbvx/+0c6/r2ucz+t/xA6BCu3Dofxu4qMUiZX" +
                "vDAybrP9Yg6tnHuw/pXKX5+O1X5xk6+BFDuJNNnZFd3O/zCI91" +
                "9ep/Zfjrf7L7v1x2j/5aft/gv4t/9ikmbLdte4BPuy8faqJo0j" +
                "dMnJqvzLyogo1h8Dzvf/0/Imq6lb6/Zz9n+PE/u/V7T/S+jRf+" +
                "zBe40/0tZoYEz1Pzb/Wz5+Olp/H/n+tfu0g1X9ZeQ/4v1zmRx1" +
                "Pof9cw/it4PxxZqjO5n8Yaj6hVT8cGrLX87j/CVuH923b+m/zP" +
                "Qfva3fGNj/EcaKcP9c8Qb1O+Bt/c5+/aBC/pZTv/Gdff3rx+z6" +
                "d0H9h4jrH+/Guf6NxfKS+EXj+NEhfy8t/JVT5+/hNev+Ubp+Ba" +
                "h+ZZLgBfVLGavdUEdhmYH3iRHVX2miW/1USPb6PKKIZoQq+cNY" +
                "FlzyJhfTn+L6CXRXPzF33JbUj5Sf/5FtJGXw+LOs/qXi+afBnt" +
                "8f5NZPWMiQqJ8QlhHcjds1Kxst7PH5jfX5fYJubH+jjcLK6l96" +
                "Or/Y2P4RPln2X3h6/wV2+y/3+/0XKN1/qZ1/to8dpYBlLrsmWb" +
                "9Taf8MzHb+Pmbmr2T/fYz1P1kdJ8wpTXTgFNiksj7V1H4e4jul" +
                "ju1JpuyKb74Ztr8YCIpKtCw2fzhSwajKk0RLRlaeTOFb0Fqfhd" +
                "wNm0SHv3auv6jinur0Ko4E5nT8R73+4ASyyhoHLVc0NejeQgmr" +
                "MizHm7TLgvHWpYRtL3XGyf/uVVgrfHt+zY39WTFrOiye9TIiD9" +
                "NH1pzhJseOzfhSEt/9KXrGmhqeuJGa8Xozh2WBJuuAL/Uck+qa" +
                "b2b8hoR1vX6l1piHqttWDIydWGprScxmm8bV/ZNaBWky/yjyH7" +
                "Ao/uqfa3zg+2sarovmCpYaNBAKhowPnA5WFJjS+NPy4KL7xxqP" +
                "/o87ky7AZdLUZb7K22IxntFyn1MX0xWx0C6VrMdZJPSV8EtPeE" +
                "fwFbxzDnqSTtR4/0Df8QfBv/xlGEwrf8mcH65QXYz0/vSy87/7" +
                "+qVlfv2SmYG1kP7QjHo3/9XOv+acn+36/CvaJNr6i6R+zLT+nu" +
                "yX+j98AJVzfjljtbpqhDWtdQA+U/5P1m7q1O8v/a3fnxvm6vn5" +
                "zNolUP44XlUqj47QaXtNIAoisF7P744U1ccP/B6/hue/h+Q/oV" +
                "wftPOgycx4RL3zX/bz55Bz/lxXOX9e8/253sL0d36+zfzVOX8+" +
                "q/kbiyAcnt9d78/vXkDF87umHYfS9XuYZHmmfk9UadPQzA4SRZ" +
                "L/JRBa6h8c6J88qF/G/uzIVP+dycuNuN1RC+2wfYLruB/9zhtc" +
                "89xeEK8O+54KxsP4x+37Tzoqi2STJnZy/HnZ+CujCsbfraiP2S" +
                "F5Mv6F+u/PK5mkCx4TWul3YgRt/ItemhHoL0A/9t+vBKT7b/Nf" +
                "kWtJ9R867r9PlCbzIlQOlUy5I2IOidkqfuzn/Xt95Jmmou2afT" +
                "8H8umTSkr/B4IDugE=");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            sigmap = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                sigmap[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] sigmap1 = null;

    protected static void sigmap1Init()
    {
        try
        {
            final int rows = 609;
            final int cols = 16;
            final int compressedBytes = 1160;
            final int uncompressedBytes = 38977;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtXUtu2zAQHQqMwbZZEEEXyY7NSYgCBbIpkCP4KDxKe5TerP" +
                "7ItihREv8faR6CBIgTczSceW+GlGiARShA5MCMn/nlSwe7f9v4" +
                "xa9DYOAgsqJL8aZSSwGu5z89/yRa8gekTYc8j0AgBoRw5gRFHt" +
                "QggPABF9HrC8/8xELs+4ApFDszF6I20PEviCYhxj9BZNRveZ0B" +
                "a83lpg5gB06tvW6L3QF1FmktCEyy+cTYyhxOcvq25N41ie34b4" +
                "UAA/Wv27b+MWMTwFsMBI5ihEAg6mAgucqdshcsVcpek/59te7/" +
                "fsYtgNwgYvWvnYK/ZkP4rbpSBmsVG41PsH/eXOOAQCRs+RB1MI" +
                "YyK/hw/YbceyWOM4iIDoIuCKtjU7xlhhynA/4hkHj/N1UJNc+f" +
                "MGs/hmcF81/D/t9Ef5nt9e9gH0stiMXNLZKG/T/2XwjE7iHc1Q" +
                "t7oBpVwkEWV/SfW+j/eP+QkHEZqI3XbXsmmGqxkaPesZaefUyD" +
                "EiubqP4Sb6X/2v6SxlKOsDrtZ7b2IxCZ1xB2zzLKS1QQ1q282G" +
                "g03urfL+XWvzB+EeXiny32f9Iy/lW+dG1aQ1HDEbWJvOwjk2kx" +
                "eup7mdS6Hr5cISBazP/Gnv9D/sT4R+y3fkJ48HfE59cwfszw6X" +
                "/uvX3I8weto4/U0vtPqlS0VnL93vihxGXDlwsm2RO8cjicrunP" +
                "y4V5JBAK4hcwBp/HdzhIKuAdPkB+nH58e/0Hb8cRf4l49SeecW" +
                "Ab0XvmH9SvDftPgLaCLlKMQRdY+XN1m1gmuWZEHN+tz45s8Crj" +
                "1w9My2vibwQGL+wpoNynG/XX0ZGRMwr97wOCDNDrxC0eSToD3O" +
                "/flMHnrxa//tXqRHhN4D7On07NxWJXtY6YDaV845OC4/vyl5rL" +
                "3s7sUyqiDu/JX5H2j0P5J8L/K9eaR0DyCQjk30L8T2Ndv3yML8" +
                "bj51DQTP4Lrz9Kx0/k+kfm469tF+Dc7AQ7/suoP1vwf1H/hc5/" +
                "pgJiqp8n57yAw/mDSqsfWj9/0Nr+0HUXkr6EKbb+MfY/GP1Pe/" +
                "+Dsf4sBuf48bM/2fy72s+Vh/34tGVy/dLyB4bz97gfHPT4Azy/" +
                "tK35S8h/C/ybZXxEEQVPoh8++m3JX1CKv0L9V7Z+y1v/QKT6x6" +
                "Vmrj1+Cq0fZOnfuscNJHTYOXfSYBdZ6W4RjYF3ljRRn/qF9//F" +
                "+h9MnbhkqXzfS4sfaR8/rKL6mY5kQURx6VyY6uuX4/zrpv57mc" +
                "u/5/T+Uy3FtQS8A7Mh4dQThNnOcCIwGwmrsV7G8/9icUfKAq5s" +
                "AXjI13PzwuMj7OO9lakouX+i0lo03391fTL9ftTP5Fb/kfN64r" +
                "l+5jNFbGL7Q59NrfrZ1r0/eFvn56/j57/HnOBGdqRV4/Y3AuFw" +
                "/zDRCzklTILCl8qL60CXY9uOxdcpin9+LLc1c8vLAIHXPxu/0h" +
                "S/AhuRPl0TPQVFt+mtPUVGM+iuG3CT/knc+6enywt00j8tPL9D" +
                "c9oftv8WrX/0Fd5w/WcuurA1IYw8/wJKn3/aGlf+B7NasHQ=");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            sigmap1 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                sigmap1[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] sigmap2 = null;

    protected static void sigmap2Init()
    {
        try
        {
            final int rows = 609;
            final int cols = 16;
            final int compressedBytes = 939;
            final int uncompressedBytes = 38977;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtXFlu2zAQHRJywRT5IIz+9I/ISdT89w4+Qo/A3qRHreVVUr" +
                "SZ+4jvIYgV2PSQs76hGBHtApYqhbz8kBWXlwsMCX277F6b6xvv" +
                "mgSpHz1NWUWtj/7MWL5wlx8Xonj9J3CTMvVX7Jo5279Np6wq/H" +
                "9D/REDEf31395oul/n9feQcP28Y1EQ4IJf/WKt0mqUr81U/im0" +
                "XzJJNRFhk69fLPBfs7X+veWrf17z72ndvCRVe44PVlj0JP8wl7" +
                "+7SR4ubzSd/sVd/6Krx3ZH9dem0nYPzb6Lpw3/WfUIEoHxJY2f" +
                "+wI9UYDUBEfwlr/axxn4H8bH8/80ubJY+bXr3318mP2fuuOvHX" +
                "yBHtafZvBxNS0U+Y9z/Dajli1BIyUyjQ2Rx0SxmgEKaNb07CYF" +
                "ze/tBRxPLuO94F8/CuN/bTjFbOufYvufcLG/JD7nGlzmaW6LPI" +
                "754/cF/mgH/PGTh3ZE9Pjx3L+ofrxv/ogj3zv+4usv7vmjNPYP" +
                "UD895589/tn6X9H5o8haA6x14Bs8Q0DX3vwvgXwJy7hAZ9beun" +
                "z5vPC7/z/PX+7jfz/5y2C87sbr3KZa4V869vmTJOc/d5xnc9sP" +
                "AABgRxBiLeOWBTVJYjUYbUXYvp+Q/9SlpQ1+ysZtecwfYQ9w7F" +
                "9W+78U/WNbZAzp7dknTA7LUx+K1T8D9NzboCLUq/96979ey/+v" +
                "35WzQY2TRT6HjpyJ/wAM0gUABILZWf1D/QAA1A8ACFh/ZPz649" +
                "IHGcR9NrTM5bMgThb2B38EgNKhBvVVIFr5otmi/wZ6AgAgRP9Z" +
                "xPMbrWt+i3f+RX4df12/TPr/J8at4Q4mO6d8ACgtXZY3oXH+Oq" +
                "W8fxXg/lmI5ye7I/f54xLPP7e8g7Sl3cAi5SbPn9f8pRbyl3nk" +
                "LwUNMoDIFn+547d2+bXXF9i/DkzruX0+P+lJpC+sWxW2y28XXE" +
                "VOtwGN2b39lvHK+nH+Im7fX7b9cEePX/9BC/0HPfoP8KTM/cPI" +
                "fnLNfjqd/eQODJ+YP9Z6Yz37Ngb4DwAA0ZKJLZQRP/iD3vb8/S" +
                "H/+4ypsmz8N/j6BQeXtRtrl72by0QOmf2g1v63dSNLDHsRS0Cp" +
                "+xfHufz9nmqHQNQU6RtWq+G3c0o0UAOQnp3M1S+4IxAVxt5Lgi" +
                "I6kNL07Xz973hLiGffNIfu8s9Jnit8Y+jjXmve1F/6eYIGwV+A" +
                "1GjSf7lIJp885eO/Ixk1K+P+Ubr1j9P8KdD5J992KWq79R8GzK" +
                "UI");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            sigmap2 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                sigmap2[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] sigmap3 = null;

    protected static void sigmap3Init()
    {
        try
        {
            final int rows = 609;
            final int cols = 16;
            final int compressedBytes = 896;
            final int uncompressedBytes = 38977;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtXVuO3CAQBORIKJoPtNoDcBSU/9whRyG5SW6aeJ72DDsGA+" +
                "4GqrTS7MhmwE0/qowfQgA5UOc/4eX54wwrpLn+O39Olw0nI6TQ" +
                "n8Lfm3otHAwIsIBcfTMYfxo88t8j/32Pzn8/VuZTYnsSrQxMN/" +
                "n4y2JCOooLd/26YeU02+UVBRhgBFMzWgAe5GRQmrBfP6lw/f1T" +
                "Qj9F6TeXqd9kPftR69dQe03Mn1LsT63fa9g/hb8u20ui8xfKi9" +
                "9v6qH936cPqC2v45ndVvws7beow+bR9kLKP5eNCsZ/ZP/6pf9C" +
                "/kt2/EzOn/mS8VtIP6qm87dLPxai+ftImT9PUb/Am5/F/gGMeo" +
                "oSaaOf/8/g7yz4EwAAAABw4qUAAHQrZnDoAAAABWBX52QsDIJE" +
                "Dftfz4tV1Ck2cSzA8fpx9PaH2N+t9lsuD5jrMsI9rvX6t6fe/S" +
                "c3rQ3hP8gfrc//lDqC+KvYc9szR8n1O9tefAKDyxIlTND/7fn7" +
                "7P/fzhum2f/lzf/lnHL8++u/bu1/PuJn1d7M7Q0DI+tgMBoiIa" +
                "EhgoG8+lXj/jHPOyP6r2lREyU2a5DKi7/hH8q4frkd8MrfNfhD" +
                "axXBENq6bPzZ2h2zvn7QbRviy/Hbw8YvKecP+iPT/3Xb9z+NPn" +
                "8AAP8H2pHLOO5U7kfSHgAoAf9dYGpHUlm4LgAkQdOx9dz1x3r6" +
                "X73q/0v/Krx+yTh5QX+NpbZ95J6ey5BRtAHkr1qxhOiC/xED53" +
                "+ADSbyQkbMw3dCGWyyJfyv5PMLb+11+ed3qW370VC+g65fqDZ/" +
                "jOuHybS/YfT8qePnz6H+EOfvLdvbsv2D/wEjx1+B+u2q1e+ahY" +
                "X8+smJu9pmnL9y5u+U+fzpE56/CTxRxtzotXRDj492082UGXgt" +
                "0C1/i1Ctu+tf8P5dFdle3vlTUv9J9w9H9Y/438UB8Ya/XvAcP+" +
                "o1fj7Af3fkT/0m/9i7ftTpsfeU+G1oF+M2a4Wbd/lFX7h2j18U" +
                "Gf9B/UtWbP9oRYHz532yAT4PAPDd1Y9I/t26HM59/s+7/G2T69" +
                "94cuDJ/lbg/UOVzFxKe3WSvzp6/2fO/LX+/tZD/Le19XPHzP4V" +
                "j6HU+1t3jLBj8VRYMyH/dKsf2ObPsfKHFwBAEH+F+Xef+k+yH5" +
                "0flOM0ln9XlIzXIqjLjI037Stdn8jCflj/HBz/AComy40=");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            sigmap3 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                sigmap3[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] sigmap4 = null;

    protected static void sigmap4Init()
    {
        try
        {
            final int rows = 609;
            final int cols = 16;
            final int compressedBytes = 797;
            final int uncompressedBytes = 38977;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtXUtu7CAQBItIKMqCRQ7AUVBOkqNw9KfJ5DmTyIPBBvrjqk" +
                "V+DgKaprq6jT3GAMAulsKF27Vsf/4lvtrw/ePtu7tfeAvGGv9u" +
                "8to0e/PxvMdU1z8wGHnzr/bhK39YLCNQz1+mmr8SDAhGlAXHcl" +
                "SeePy5sKzPtEdcKeKEfsKeABA/AehfmvHDf+H/wMxwgflXoY1a" +
                "juivyLp/AKBJjxEtuCD+Zol5hOGJMtz5lHhY/7q7/m1qb/+3t2" +
                "t7hASgu9qv3asZxlICspU8xJ9xrR94kv5Nv/5HjL/L+QH9/isn" +
                "WFqsP+OChE77o34xI35stvf8/GfZX/9c2TZfL1mx5/egVmMV9k" +
                "/Y8/+3cv5c1X7o+lH7z7xOEiNrXRbJoALEVUNdaF165MSuUX/w" +
                "5m+p+gMAAEB47B1Uv5icJo2pP3TK/49FJE9vv07IRfWiGIsJm/" +
                "4Xv36/+d/L1wVXvn9NZr8ctyRSSAXNfh9ruv3LJ3j2/Mbc569k" +
                "muqnLe2l6+cp/H92/hT2S3P4n4n9j+iXeSUJ28PMY+yP/UOrnz" +
                "rZH/7P+/mbaz+3gPobAAAAwBOBQhY99Hi4f7bn79qkFW/9WrT/" +
                "ufrfjn4NKvI3AFCavwsZP23+DP5A/ox9zBmj9dfCvH/mcB1lr/" +
                "xivGZUvT/CwU6A4vjT4N+dzt+QjV+02sxK/Q+YKeg13L/Vp1/T" +
                "nPqB2vdnJlb+c+28W8L5xXDq/o3u+IWUs5pyIsxwKf1T035piJ" +
                "/heQzbCgKJbP5O1fuzkX+o4P9A3F76/AGi/EPs/hWRPyo+/z5C" +
                "v7TkP4/t7aj5879lluG/8F/kz6fXPxp8fs9Ya//AdeWmwlL87i" +
                "gyNIr/tox7nMuSNnjd/lXbFfM3M+aPrXDl/OGI/ohz9xjqJ4xB" +
                "/fmt1Miq+298fzv0F2/hBv4FAJb5ayf+pPpYF+rcLIr2hWRw/1" +
                "+p/p76/F+X58dT1/HT2t8e3Ix89M/89Z8bv7JUasD5WcXxL3VS" +
                "FhLfv+kL/hdX//NM+IMJ5p+fkf7+3ynvz5XOvwPrb7h/Lpu/e9" +
                "HI4Zb/ACdwu9c=");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            sigmap4 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                sigmap4[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] sigmap5 = null;

    protected static void sigmap5Init()
    {
        try
        {
            final int rows = 280;
            final int cols = 16;
            final int compressedBytes = 360;
            final int uncompressedBytes = 17921;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtmkFuwyAQRQeLVLTqAlXddIdyEqsnyRF6BHqTHrW261RxlK" +
                "ZOmsIA7ymSkSJixgzM/zgieemmj0QzXSaCGD83x6v9+uLRixH3" +
                "LPG7a3TSn+z/sLr/66K/ueL+qzB/fEhGAADaoT8uE7nL1Pr6dU" +
                "n9ARXExu//v5iaBoMYg5bXzzlC85IlkK1Fkfv8ocb4b35+cuH4" +
                "0d9IpmQVL+4zzYlsxHm5G9ofT3NBGCIPm7H5tuuGDLVBtvukv3" +
                "fv8rI7/j3/swf+3SXnm/+lf7EsmVTrJ/f+y/6hWIwu0gT/w/wr" +
                "ntszW5HFYzblv9DvAElBvzP/Neu3Luv4c8S/Uv+Vfv6nUb/ki9" +
                "83Hn9F/jGRf6t2/fL8oGQS5S/ckoD+R78l9S8a37+reH6a9k9T" +
                "eH/F+rt0/9GE/y79/TX6D4D6h/5Slz+t//+J89cxcDdHbw9rZ9" +
                "ef8LWHQ/ECQP0sfP0zfoBr+QQDSk2V");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            sigmap5 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                sigmap5[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int lookupSigmap(int row, int col)
    {
        if (row <= 608)
            return sigmap[row][col];
        else if (row >= 609 && row <= 1217)
            return sigmap1[row-609][col];
        else if (row >= 1218 && row <= 1826)
            return sigmap2[row-1218][col];
        else if (row >= 1827 && row <= 2435)
            return sigmap3[row-1827][col];
        else if (row >= 2436 && row <= 3044)
            return sigmap4[row-2436][col];
        else if (row >= 3045)
            return sigmap5[row-3045][col];
        else
            throw new IllegalArgumentException("Unexpected location requested in sigmap5 lookup");
    }

    protected static int[][] value = null;

    protected static void valueInit()
    {
        try
        {
            final int rows = 40;
            final int cols = 122;
            final int compressedBytes = 4402;
            final int uncompressedBytes = 19521;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrVnAmcFMW9x/9V3VM93TM+JCsQRcUnkQiCy5HNriIsyBJQjC" +
                "GJmIgaFzS6MeIB0XihQbm8UDEgIiAoYNAVEAIe8cArQYkHamKO" +
                "94kH0ZcDzUMiUVTm/evqru7pnu0ZZiHpz2enu6tquqvr2//f/1" +
                "/HrDUKCFjWV8GDHPs2bYUvwH5QAwfCQVBvfQsOg8OhL/TL7gf9" +
                "7SPICBhEJ8MQGGr3pvXefXAcvZDeCL+F0ZkGGENXsn/AmXAOtF" +
                "gj4Xy4kL7Dhttr7bx7HVwGl8NP4Bq2FSg40IHV0uX2h7QTfBH2" +
                "h0OsAfQJeyZ0J7XZudDTXkHq7f3hSKsTfAW+6nb2dtIaugYGwj" +
                "H0aTgWvgbHwwl2Z3ofnGw10Ovp7+F0+J79dbIZxpFm7wC7jt5j" +
                "dbEHwESrM/wYrrDWwdXkYlJDz7A+hn2gE32TjYHO0AUOgK70Ju" +
                "hmrYVD4UukL/SC3tCHrrD60672NTDA7gBHwdF0NDTSDWwxNMEI" +
                "GEkvpxO9T+BE+AbtTMfaU+AkGOtOhjOsnjDeqoOz4fvWengDzo" +
                "UfwkXwI/gjXEwnwVUwxVkINmSAWQyy4NLT6WraEfKwr9WU7QQd" +
                "4WB6Uu526xDoQV6BL9Mt9ujscjgCaqEOGuyp9GSykZ5i1dBZMJ" +
                "icCMNgeAZgFNsGX4dvwrfgO/BdOMVZBKfCadAMZ5EW+AGcBxPg" +
                "ArYEJsElcKl1PFwJk9mHhUJmaWYJzMgekbnUnuMBbaVT2AS41u" +
                "qKOZfRW9l5BbHR2ZIzvZp9xs8zg2h97lNMv7BQIC8VCu7SQsE6" +
                "MPu3Qmhzt7s9kfMN+pxtVenv0YczE9j5Oj3zkNMZP5eJMlfh0Y" +
                "hCIQuiZJf8gXjl14JrwtRCgXPmx851zmJ15W5Yj2bMqcNvjrMH" +
                "4HfwipwzpiNnzIFCzGat5Z+kb5Bi/1R8dhBPjXWxprHlqg0uLy" +
                "RsMD6zQVxtPXnRTGf/5J/evepeWXWd1Trf2yTOr83dYT0sU5xN" +
                "7DduRuc7HWmLNZttxjJzsJYn6vRsT/MuyHmaPCJ+DdkS1bI/hu" +
                "l4hpzpJrgerlM1+T1y3khfwKOu4SfxRP3JCLZLXXuSqvOFcp87" +
                "nHP2Boo2OjTbIMr82vvMmxB67q36iL6Lf538mt7EOZNacdxE6j" +
                "lnq1MhcfNmSM7BRjarK13gExScRd7FolaHJHMOtuzhbi7MGe15" +
                "lar95+wjdTSp6Dp14tvTw6k0VM46Mco520lyzp9lvYHv9Fl4vC" +
                "VEsIY+JMoR+lKJ1ljjt8L1Uc5ozzOxxBC8xlEW6jbcwHU7s4K2" +
                "soIFqNs3wq141wLMRd3+KfTLM7iFc6ZXi/d3qHqPjxP13AWj8X" +
                "iWjfVH3b4Nry50G69f8CaKcqjbokbHAFXP+hnnzHUba9A7N8Ce" +
                "KTlDT7cbqXcuwqd+kuu2qPOPxDWOwT/UbXGd26KcfWusUzWbKD" +
                "lnZ/icv43n6t2BLqINLMkZvoR/N3PdxvP+bt7pIjmjbiNnm7A1" +
                "XLd92xW6jelT4CYYG+Zsrcf82Vy3DVufkhNvB+r2dwPOkJecoa" +
                "Pg/H3rDeiBqV/2v8Xbguv2TnV9GwZjyjA/X+g27oVu417qtsHZ" +
                "1+05MFmk1HD/bB8r/HMX0z+789hb2j/bQ9mWzHzyrLjHkDBne5" +
                "jgPCZzQ+5I7p9FjuCcPyj3subs++fbAdvQGYl3fltyVtfqnlnA" +
                "OaPW3JIZAUfiseKs8n3O6J8d7p9VOvpnsZ8ndGqw5ox/d8AV3g" +
                "64WilYI/fPIk/458CeBedekrP/vgjO7hZotG22NspZ2Nr+3D8b" +
                "9RP+GffKP+MRvl/cP+OnLTifyf2z5gz7im+hf3b/jPUo4iz9c/" +
                "Z0VZ8myRmG4+cozVn6Z1Feceb+Gc98/4y6PV9x7oqc7/Ra7UWw" +
                "SHO27+Kc2dOC8wJA72UvcJ5nz5DX0ZYeAFQB9jgeYX3ZL/yajU" +
                "H1xvcAzmGP6rR8t9wr7Cn7TpEvOIujDvZC51TOOeTjukvdRvur" +
                "Z09wzone8ARvhnNylLP0z+xJ8UwbVB7qNufM/TN7BPZRqdzSF4" +
                "ZUH+OwiC50UGUb7Qa2JqEeJ4XOkLNx5nMWZ7Z4p84JOGPavqrk" +
                "wd6DnHPR1XmksSt7hqrPYumfNWdV5jsR79UScPbL3OUfLYa7AW" +
                "MN+BnWoRX3WAZE7OBO8yOq6dI/xz1v5lzlbf4r/9/hnPx5uVfj" +
                "/bOD/t6dYURsYHLOjCiU3Lhuw7JSuh1wNnVbpS5Vtmwl30Fyzm" +
                "H0Y8+LL5EtlLtZE6L+WcU2c7Fl6mNrMTXbrHU7zR0C3Tba4B7V" +
                "4t0z54HltXLddnqhPd8HD6Bu3w+t3D97lvTPWH4FrCQjnCNzO2" +
                "AVDHUOw37Vaq7bTh/tn91l+QbUbXwKp7fS7Y35RtM/c91Wd/2X" +
                "U8vON3Vb+2fO2RapXLedumLdxpY5IM4/c3s2dTvgzO050xjnn8" +
                "UZ71fdHKfb4gjfijjddqnpn412LfLPas+si6P+mes2vrVoz06P" +
                "eP+cHR/4Z96vivPPhm63+HbUT+o25jwoddvpjrq9PrcTHs20Cs" +
                "7cPz8s+s8/92pRt59A/4zqDP1hLXIeI/zzY5Kz+H6fzP1St91f" +
                "5C/gus05+xa9zq+Z0m1YA8JWkHOibjsXad126oracR2ckDu62D" +
                "8n2TPvP0t7Dun2Q5GrJup25vgETRkT6DaMT6XbVyTrttMjTrft" +
                "qX59mlRqrG7DI9qetW47/fwyj6v9Bh6HSXuGp/Dcj8NErorDcp" +
                "dm1kvdzs/RcZi0Z1FqtPTPIt424jD/XioOwyMxTqK0ekY4Dgvs" +
                "Gfcl47AcvqFFcdi4UCtNjONsxmHwpF82MQ7jnNnCeHvm/jkah4" +
                "l9chx2leQsShlxGOec7RIXhxma3y99HAZPA7c3bc/PSHumtZwz" +
                "e1HE25vYkIAzbcpdpjm7061adjQ9gQ6lX+Oc6UBaT3vCcbQ3bc" +
                "g8AqPpYBhDG+kozpkeRY8X42FH0D60LxsUcKYjNWfajw6KcmbD" +
                "JGdxpjjT4XGcxXmbnPl4mIzDYjl3Tcn5+WpwhmeR87XxnIXNbK" +
                "wK5+16PMzgPN/UbWcgPAe/dHrBrwLdVvYsdJs1Q38dh2ndVvo1" +
                "lp2p422xD+m20Q7CnqVus3FctyP5hm5Lzsm6Hcc5NiKugm5zzm" +
                "3F2yl1e27A2dRt8Zyxum1EsOMr1u2NOg6D5+EVZzBshpe978Fr" +
                "8CL8Gl6P3pVhjJPN03N9P+9zxryzo6XjOKu7ipG+OM4YKS1Q+1" +
                "uMK9Wlj2dJcxxnkVNjxtsx3+wbH2+Luj5fqNJmLUrOi3KGFyLt" +
                "P57HYW22gD/u6Z0GagQNXvU5o27Tu7lue82mbkM9Xax1m9sz6v" +
                "YSGEQXct3mnOkdqNsz6CJ2Cer2fNTt6XSBnMfQ8baKL+dF/bPm" +
                "LOcxSum25gx/0rrN5zGqpdvQDf8OjddtPo/B7dlpaVu3TXtO0G" +
                "0+j7E4WbfD8baexwhzTtZtPY9BZ2rdRs5at6/Uuk1fg7/Au/R1" +
                "+LPTK8YCt2h7RmK/8S6I2rPTh80ow57fTrbnbEvMe163Oxak7T" +
                "ncf46xtH2S7dm5pOy7vhk6e8+/y2Nl2PNbxfbcxl2bTXvGftX/" +
                "qvR3THuGv8r+M/zNjLezEwN7zmKfkP4uN1yOe0rOsv/MrovG22" +
                "F7Lo63TXsO/HN2UrI9VxKHBeNhch4jYs9/TxeHOZOrFW/bZ6a1" +
                "5+I4rLQ9R+Ltrdw/+/b8vhGHqXgbOX9gxNsfZd/UnJH5W/QDui" +
                "M/hG7zdfvvqNvvO314v4puTcOZfsjHSXT/uYjzlnblHNXtfziZ" +
                "PRdvC87nVM6Zzz+3zdlaGsRhPuf/k5yVZnYtVgL6ceQcOePnNv" +
                "8cLQI5i/rSrWkUjX5YMoow4u1Kt4yhbrDN1O1MY1Ftbmpr3LO6" +
                "cVjmrcq/S2dVPO6p2ty9Fz6G7ZYHO+BfmLoTPod/wifwacR/iP" +
                "kdK7/7TwsfJbbDst27cvvE29mDq8b5nd3gHFpn0Ha8bbTBZ/4R" +
                "988FrtvizNdtq4dzoNZtOjuwZz0eJnVbjoeFdTtpPMzUbZFe4X" +
                "hYVXR7V0r/3FIt3c68m6zbeFRat+eUNy8ZjJMQkLptjyU2Yc5A" +
                "YhGHZNz3DRs+NvSuZNGeBxfacWPDqn3FtPH27uo2IanseVvlT+" +
                "Kkahv71Ji6qZkj9g3iguUM5vbsfk4852b6AtS4gbWr+SpzXjK8" +
                "zkCNb8+S9kxy8fasjmiIQ2i+yt0VxGF6virOnlMRnhg3Lxmerw" +
                "rZc+J8VYk4rMz5qswOI9WYr4q3Zz5fZdjzS8nrSQJ7Njn79pxX" +
                "8fatEU+wkb5Aroypf/+k+edqbeniMJharj1H55/Ls2enJcV9xq" +
                "ey55274Z9fKnn/aSXiMLVmjVxFfpL0/eybwbinNwjLPhurKWrt" +
                "gmOlVuhxsXfb8u+o23ZD1eKw3fFpqd4kOjPGelR8QmutjWBZz+" +
                "k4TLRPDdyIOU3eB+QaqdusmaD+yPkqrtvmfJVzt5ivmqXmq24z" +
                "56usX5q6zeerJGc+XxXVbXOcROt23HxVubodcK5Et0vEYWXqNi" +
                "OV67YeJymt26Y9+/3nOUH/mah1vTk9cnBt4J+1PXPdtmJjEuee" +
                "st/OWHt2d6UiOHXP2nP1dJux9rJnmFYiT+m29TKo94zcqPtV+M" +
                "n7Va/mesJh5Aa+DpCPbwf9KnOcBFtiKR8nMeclY+8n+lXiqEMc" +
                "Zz4eZm3yz0quA4zrVyVxNvtVKrVoHWCpeclk3S53HSDLJs9Lxl" +
                "y9jHlJ41t+v8pPuUvRvdXaSmbrdYAiR60DtHZkJwb2nFsgOVvb" +
                "w+NhyHlZueNhCZwnleV7l1Vqz3odYDp7phuq1m88NF05671K/X" +
                "NsdHub2t9O5pE51qcWRoNBIJVbEdVt8cz4BlifR9gtcpb7xwva" +
                "X7f3dByWRrdTPvVh7R2HxXKea7QHAvbHw8R6T3IHX+8Z9J/RP6" +
                "+AlVQq5iref5ZxGO4bnHvN/jOs1v1n203qP8fHYSI/VRzmjtpz" +
                "cZg7smpxWM/2jsNC9Yis96S1ZL7JGd+AO6FG5DSZnDF9QUK8vS" +
                "IUb682423bSx9vl8H5tD3I+ZSqcR69NziThZozt2e7E+dsd7T3" +
                "g/vIXZwztIY5k0W6X0UWRzjfb3ImS0Kca9qD839ov+rkvWvPfB" +
                "2gLVoAOR/A421yr1zvGV4HSJb76z0fk5zx25wzX9Gv1nvyeJtz" +
                "Flf2Ofvx9ki9DlByDsdhoTMVbwec/Zx1VYm3U68DTPbP5a4DzB" +
                "7bXvG2XgcYG2+r9duklc9XkVXF81XizF9PIlodOcv1JNqe7b7I" +
                "+QG5noQ2yvkq054j81Uj9XqSYnuWnNOt6+X+eXfnq8j96ear3D" +
                "9Ua74qO7ry+ap060kkZ/JAaL5qpR4n4ZylfyarTc7s6TBn9kzA" +
                "mT0uOec+Qc4rw5zZo/ZXAs7sqci6odvb5ix/R1eC82kxnMXvoP" +
                "Tv6Io589/RFf9eMgXn/6ka51l7hnN4XjJYp2/E4KFfBtKmon6V" +
                "isPwyNTtVVy38U/oNu5N3Q7pLtdt0z+n6HEPj+2BjUrppa+otD" +
                "cS9KvckdXqV7lv7JV+1YN6fJv7Z5GyTut2vH/WnIv88+oS/nnE" +
                "v4t/xqOK/HOa8bB0/tn7017xz2qdvo1MCJaDzeRhPHqU8N/MiX" +
                "X6XLdNe0bdXoLlxV2Y8u757XSR82B4nCT4/bM4eypU+03J4yTG" +
                "d56o+L1Xul1sz+yRvT1O4r1dxghPzDr9MseIIuv0yZPkMXjFRu" +
                "sgj1P+HzJiFULp9uEhTT1ItcSaMu6einN7jIdVqttex2rVJnfz" +
                "3uBMfJvhv7vh9gwv4+dr8CJ5TtqzfXaYc/x6T+S8Nv349n8e5+" +
                "rZc1k1r7o9w/8DdYSYTQ==");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value1 = null;

    protected static void value1Init()
    {
        try
        {
            final int rows = 40;
            final int cols = 122;
            final int compressedBytes = 2206;
            final int uncompressedBytes = 19521;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtWwmM1UQY/mc67WP3LS6IghJAEJBDYBcjiIosuyyXsIIGBI" +
                "wi7HIIGjkUbxSIGGNEI5JIFIiAoBFBSUSDByCnohAQQUAQFrnk" +
                "UohuOITn3+m0r+1ru+/a7nsLTeaeTtv53vf/M//8LxQKhciOkB" +
                "pvDzlc8IcaK0PUmI1y6hH4PBT1BaV8tKEhXy54Pt47WbbxdY/E" +
                "/NR9ltLhuN58v7WsFEOncu4YYisfEekBLaU5QEDCciYEEetf4W" +
                "q4BmpBPahPC7G2KTSHXGir4Ux703zaDTpDPr2T3kZbQE/ainYI" +
                "LIO+tBP0p3m0F5TASHo7vRseh7H0ZtqaPcOf9Sw8B5NgCu0BFN" +
                "6BbGUobUvvwvrr4HrjvRrzuIUotcFwK7SnXY32jhgKoBvGvTMb" +
                "YXy/qH8IBvN0puUrx+s4w0tGXXW4lqe1oQ7UNfVtgqEltILWVp" +
                "zhDgx5mTdgXAjdjd73QB+R6wcPmEYZAcN5OhoehXHwJOYmYHgR" +
                "JmPMMCgYqkGG6J0FNXhaExqImpswNDNGy4F20MGOM3QBnA/oha" +
                "EI7oX7YAAMhEG8/4MwBIZhOgoew3gMhifgKcDZh5MwkX/RdPP8" +
                "kF2mb33b9BzEmZ6iZRif1uvocQwn8Rf/BaYnovmV0jNitAT5nD" +
                "nMPz5nFod8vthbkXVK3G9BdhqzgXym8wSf94T5zNusfJ6LYbbK" +
                "Z/4+86AnlucEvkQ+vwv9MT9L5TO/D/lsoDtT5zPWUwzZOs7l89" +
                "mEmYnPvOwTn1mHNOfz3xqfUW6fJfvZIijj6JcS1MdwDi5gS6GV" +
                "z7rc5jkutzFV5fZXmOJbqHKbt6Hc5qkqtz+28LkHjv2vymdNbk" +
                "chAbqmn36uiCsBPgv9zDaQY+QoOUgOkyPkT9fnaOuw7x3XYV/H" +
                "/NaJyu0VPsrtb9Ic50PGOuwsOa5sgjL2E9tMTsJFckLlM9sSNZ" +
                "+/9eDzpnTns/JD1eAzz58q9zmGfnbgs8EuttUfPvuJc7BhmuP8" +
                "l3lfRcq0dRjOTez7qpUe+6qf7fsqbR0Wz76Krq6MdRhdld7rMA" +
                "JiX7UdCNvFfmE72QFykZxTcWZ7VZzZDrYbmpILkMtKhX7mNhPo" +
                "LOZim+DzKl7bX7zhSBduIc6MWxBUnNnvEe2NLaU2Hix1xNmNz2" +
                "acRVobv/68rWdLZz5DXrCj6+j9LCWBsygZOPMSE7UGzpivIdIG" +
                "LqO3c+KzjrMoDYi4y8DZ4PN/Op/ZIaJQQoEEbLrRop9JNVf9vN" +
                "pDPx+062dNbqePfg52ikI2khSW2/pMykBkoYMo0+Q2xshnua4q" +
                "tymFXKudROezYSdZo9pJouGzZicJ75/Tgc/SVC8+y/VSnc9U0l" +
                "IZUQvbPanCe9fiLZy5qJ9naPpZw1lWtXa+hjPqZxXntaifT8Ab" +
                "vHcJDdjtJHKBqp81e5iqn8PrMLN+FjUtTHkXOwlyrE+5TB7vxG" +
                "dNP2Nax1Kr6uc3nfUzPqvIVT9PM+tno326qp9N5ckiVSy9skRa" +
                "06ix6WcMDvrZaOf6GVObfjY9QdfPWcJO0pA2ojdS/quijWkzz/" +
                "V2c4tMrS/08zq/98/RXrTJFTuJMRclPOYzT4fREbQ4ZpzXV2Wc" +
                "g4OqBs62mdnvzmdNbpv6on6WcWUV2BDtOcYVPlceznKRuSSZdJ" +
                "Smn42WnLB+tq7DpDbeOMsFxh1n/J2dpPC5JBVwltomPBc5Up5p" +
                "vM7O+ypR47iv4mWXfZXcx2lfhTlf9lVJwTkl7GFSQcJ8HihHyA" +
                "TyAm+x7JDgFrfzZ56P6fw5oT3xyz7vn1+N4jkVfkYd2Oj5/Kke" +
                "ba8InCeRmdJgaUg5+oG3y1Mc32FHqq7DkoLza+mtn6WHDbk9FO" +
                "bBQnkqfCRmZy6GD2M4f97vIbdfjtYeZrWTlC+3YUG8OMMHl9M6" +
                "DOYLPi+RF4Mkf6qfY+AvoFizk4h+hp0E+34m6vJF2pPPxGnoi3" +
                "lhJyFz7XYS7RxD5JJiJ4ni65JmJ1FxTms7yWzNTiIvVc+rpDGR" +
                "51Vyt8A5/byKc0ust3W/Id0eRi9xe1h/DedIvyHNHqbZPTU/QO" +
                "NdKtVvSJJMOLueV3nYw3w8r5JY3OdVSwTO6+S1OM44b35U+473" +
                "dbSIsC6hFL2qjt8QK4xbP4/V9bO8lSwPhTLqSxNtutE0NpsV57" +
                "5qC977Xlg/w498tNmJ7auCy5x7Z9SrgHVYUaX8Qm3ra/Y+dIpx" +
                "hM1i7yS8QORt8trwOYbow/VzRqOwfhZ91zvpZ0xN+ll6vXz97C" +
                "S3Y9HPweX+6efUOMcIdIlXP0vTdH9PeY80X97twKVCqz3Mjc8Z" +
                "xR58/q0y7SRVR27Hbw/T/YYwd4wcxfgwOSIttMnBGTabyj5HLC" +
                "75/dXBXcnAWV+HpaLcjnhXFvedC3T9rOMsLfLis7t+lkd78Pmf" +
                "1OVzdDinCJ+TgDOclT5RZM1PX1rs7KfvhXPG0lj99K/Ibf/W22" +
                "a/XmmJU4/MiH9HOvv1mnbz1f356izw0e6ZEnI7EPfeVdfPmr+n" +
                "chXbqeC3SSvs/p44W7msVPR19PdU5XZs/p54byX7e0orrXLb3T" +
                "/Mnc9++nvqcjt+f0/1f3TGaGvMfoDBPNUPUMXZuKe7GWfbE6LA" +
                "WeSyHdsrDGcywRlnS88Ux1nnc/x+gDRHcXlSMs6flfpW/czWVv" +
                "19FVuTSjZBtk6X2/JGlNtNVD99lMGavWovj1Fuiz4WuW2MsC3m" +
                "Zxr7ski5XRFXUvwMikJV4pJPoTyeDrhztp9LRswaP8dQDKtLcv" +
                "0MnM8lPfpfVueSCeB70vTljnZPZbjV7hn2J7GeV6l2z7D/tpfd" +
                "U/ffTtTumdXqcrN7YilBu6dmD1NGq3YSij2px4m2s1+v/1dWzu" +
                "W2r0rAwmKSe0p7ZYJ0HuvKk7so0aWLtro5pvys1PrGqmMnif8K" +
                "/y/WYukYleT19tN2e1gy1ttZg6+sw2L+zf8Pe2AFAQ==");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value1 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value1[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value2 = null;

    protected static void value2Init()
    {
        try
        {
            final int rows = 40;
            final int cols = 122;
            final int compressedBytes = 955;
            final int uncompressedBytes = 19521;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtm01o1EAUx4dJdqbrrhQPgh68qIhiS1sUdRWtlVqLn8Wb4k" +
                "UpIggiCFKtRfxoTypKFUutgqIoimJBUQ+erCcrVgQtggdBquIH" +
                "qNhCQeJkNk0z2aSbZNNtJn0PkkwmH7szv/zfy5skmqYbadLnuJ" +
                "Ecwbs0V8PzhbVZWsQNzw16pFo6UqJ7tBgZOWRwPi4D5/SZ4nFO" +
                "bdJiaKRtzH67yvY4bKu7Yil3FYlzO3AOyLc5q2fa6qZnpcJNz7" +
                "Qtzn47GpyVqpA4txicb0vht7tBz74JH+U9sjPLGe+WgvMT4ByQ" +
                "9glDz/1ScH4KnAszOmBhf9LWaz/xIJv/Mte/semHUf7uqd9/h8" +
                "S5Bzj7t5JKxrQ1q+fEC8ifY8t5kdkvjDM5JYHffg2cA8bn08ow" +
                "u3vXSt7ky5+VfxHInz8C5yCGFHJWGUYpxvmvF86oRuSMGuyc0T" +
                "60X/gFY3QFYaF2Bppp+ycLLOXFaInF6/xhNWt8eqpB29mnO7ae" +
                "Xw2oDJU7cebbatE6s7wZbTHL2x3Pt1dYO2YsiVCbNpbTzJp5vo" +
                "ixqw9t5aVtfL6DTY3CHgfQQdTE+mAIteRwZHuSc+Ppt5Ozi3r9" +
                "NheuZ9ktOcex9nk+PY/p/duj1cYw/HZcDFeQi+Q8b1svuZAye0" +
                "btw7U5HDtwDa7jR63ASzHzsbgML+Prq9hUjTfwcgav58uFuFx9" +
                "KfxWvVmqwis9/bu1wHk8LFVZkJ47Qc/R1bMLs65cPeONfvVMLo" +
                "l6VnuKqeeJic/qsyjxVR0jcTpj6WNHzkbJxpnXVZPrds6248Fv" +
                "T6iekcKv/pQm5FXOnMkNPa8SOaMGu571vMrKOZtX4fqRvCrLOX" +
                "9eNcoZ8qrC8iqr3yY3Baph+O1bop5H8qo4+20Z8qo8vW76bVbm" +
                "nNnS4rfN6+WOs9+2nKkonAuIajG/3w6DM7kblfgMnAvlTO65c4" +
                "7OfRhwtsTk+6597Pt+Ozc+A2c59Uy6vcRnYx3iswTjJP70TB6A" +
                "nmOl54eg50nAOd/49qO4tDTmz6seW1oag+dVwDl4fPY5HvYKOE" +
                "uYg72dLC2Npd9+T/q96Zl88K3nd6DnCDLX3w/7NMZ2yd7fBs6C" +
                "ngfIV/JlnOLzZ9BzRGLukNuWRJ2tz/l3NwmTvtfvbhLmU+Owvr" +
                "sBzkH0TEsppUlaoml0KiU0Faae6RTQc/hG0z61bOqLenqvWh8n" +
                "wZdtdXm/x8AdEJ/lMpqBvEr+cRK1ly63tLQvDL8N42ERo33Nv9" +
                "+mq0f9Nu4Evx1tQ/8BK5CuCA==");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value2 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value2[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value3 = null;

    protected static void value3Init()
    {
        try
        {
            final int rows = 40;
            final int cols = 122;
            final int compressedBytes = 528;
            final int uncompressedBytes = 19521;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtmj1Lw0AYx+VI8vhW/ATdFZXiZEW0CCrSVkVdRBy0iLODi6" +
                "sfQevb6OaHcBHESaEqXUQHtSLYWrCDa7yGGpvW2BQam8v9H7jc" +
                "c8klw/PL/7kXjoVolqYpTjGaoQBFqUMvGRvXK4zF2RibNLxhNs" +
                "h6eN3HwkZ7lJcIixn+EIsadS/rp3bL+1OmN8BGdAfGJvSmmNKl" +
                "+8xoznlfdVuXxPzHmYVsnzRCz/PQs1c40wIRtVEr13aANORt73" +
                "Omzvr6awXX9bwIzp7R85J7nLUgOAs4Z1vGPAzzsF/z9oqYepbb" +
                "2HENva8iRh4enxPu6VnJQM9S5O11cJaC8wY4S8F5E5wF/Cc4Z9" +
                "M3OPM6bN6JmPOxrW/Otl8C5/9eE++o5077qheIl8Dz7V338raa" +
                "Qt72iJ6T0LMkir41eO856JlGtARW9L7jPwKcRR6fD3BuCGZR/h" +
                "FiIDjBE0M9a7wk/lBXt6UVRNxEytu2TxqRt0+Rtz2v8bOKmOfZ" +
                "J79+mO0sL+8lP+eIWgFRFYB7qsi5jFq2zM8hPr7hfA3Okozltp" +
                "zpBtERVr9pjM9SqjlfrWe6Q972Iela5wDvESPvrp+1Ay1Z9JRL" +
                "evi5r6Sq18/aYd3nAK+wfvbUWPxoRBX7YX7n/ATOUnB+BmcpOG" +
                "fAWQrOL+AsBedXcJaC8xs4S8E5C87+NpfPGVj2Q7FP0jxr+QIA" +
                "St2l");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value3 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value3[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value4 = null;

    protected static void value4Init()
    {
        try
        {
            final int rows = 40;
            final int cols = 122;
            final int compressedBytes = 527;
            final int uncompressedBytes = 19521;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtmksvA1EUx+V02nM9uhBrO/EIIlaIqEgQqbJlSxofw7NfQF" +
                "giyoI0dhIJa49ILHwPWhRdEBnTqTR9oFOZxr29/5PcO+f23k6a" +
                "/2/+p3emNc10cDzdU9hq8+aPQe15o2YToVhwApy1Zf9QQDlBKa" +
                "t/yo7vrBb/yu+dnJGSUPWfmT7Cz1pwToKzFpyfwVkLzi/grAXn" +
                "V3DWgnMKnBHZq+ENGihO8N3JKk8DlFI1qEcQT3GIJ3ma/Rzk+u" +
                "zMaNHaEI3QuJ0NUh91WMcu6rfHAasN06SdD1DQPnZSN9flvX8i" +
                "m/XSkKNPNwZCrnH2MHMtC8vVfvaBswLVt8y66kumOWdyYRRp7A" +
                "JnbwScZfHzjzMucBZecJaLs2D4GX7+s59bwVkLzm3grAXnPnDW" +
                "gnMAnLXgHARnmTiLmcpwNmLgrOA1Ef19XsxCI3n9LBbxfLv6Qy" +
                "w5X+tdhV7Yh327D1uGn6Wp22uWq22nsl+soG4rUIMj5a3P/F4l" +
                "1ivnZ88lOGtRtzfAWZIasOk9d7wPu4ReCn8/74odse/Ez2KvbD" +
                "9vw8+S+PnQ1nPO7sO0UE3/66UWJYkcVPTsMdP0lNItaq35KHht" +
                "Jyffgm+qosaXeu55BI204HwMjTS9rzrBPkwmzuI03RuNbnM2ms" +
                "C5Cuv2GTSS+P75KpMZNyLnOYhx60rdvoCfFbyzu4YGqkbNJ8yo" +
                "Olo=");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value4 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value4[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value5 = null;

    protected static void value5Init()
    {
        try
        {
            final int rows = 40;
            final int cols = 122;
            final int compressedBytes = 599;
            final int uncompressedBytes = 19521;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtms9LG0EUx+UxmdmJ2nMR9NRDRaWIbbWIhmjUYqJFxB+nlp" +
                "b+oPRf8SII8VAKqTf/DvFQogiiaEE9KKhUK3rwug7rsiYhSTcl" +
                "CzO73wfZfTs7m8D7zPe9l0nome0ay1u/bM/YFqXsEqMMJWnU8f" +
                "qpl9rVuZP6nOtB9UpQ2vFf0bhz7qAutlH0/GvP66YB24fRiA2r" +
                "i1l5a8PvXMop/umSsR8F/nfEMxRrYhMxiATnHcTAVCNVn619fl" +
                "bmTh3qs7WH+qyJRq9ia37nxtYRL3P1LDk/D0rP/BR61sNk3Inn" +
                "e+f4kT7ThypRf1p01ar9Gn5iJBEZVH2ucKcOepaPoGcDV9pjxM" +
                "DkfvveWF62PIxjPyzEnNvAOcT99nOVkXv81Gf5ombOq+CsDefe" +
                "APuwl+CsW94OhPMAOOvGWSYKx1Gfoeca9JwE50hwHgJnDb9XDS" +
                "Nvh7jfTgWnZ3YCznoY21EdmIomK/ilmB04x1322+3Qxtzx46In" +
                "t2v+rCPPO0TkNdZ+RmY839GzOvd5I4mS2UrPFd8JetaaMyX9cJ" +
                "aT4ByxfvsN6rOReXsKeRt6LqPnaejZPJMziEEo9klmsU+CvP1f" +
                "eXsOnHXjLOeL9jSg5+jW57eIgbl65lm+5Nbnd9X1zJehZ9Tnsv" +
                "X5EzjrxFl+DYYz/gcYET1/A2cD10TuH33aAmKkcRe9WH489qWE" +
                "8l+6Vcdr7/qPel26/oWvdXKDaIc0b2eRt3XhLH+KCZERaTEpms" +
                "W4aKwnZxEHZ204rwghpLBsW3Hm4Ky/iaba5nNVLxvuALBqUT4=");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value5 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value5[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value6 = null;

    protected static void value6Init()
    {
        try
        {
            final int rows = 40;
            final int cols = 122;
            final int compressedBytes = 591;
            final int uncompressedBytes = 19521;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtmtsrBFEcxzlm58wcNgp5kBee3JLck1shdl0iJf+CB2+88E" +
                "L+CA9s4c0DSuFVRGldSu4hecCTeNcxO7a1bpnRDGd2vr+anTNn" +
                "z7S7v89+f5fmkCIeNimoLvCISfukkX8w0kYaSLM+qiYVJFc7F5" +
                "BK/bpWO+qIXx9XEZ9+zieF0s67+1sio2JSww0YaeIwS8wzry4Z" +
                "XrsIf7nB1GX4wKkmHWrHqboinairkbkL/fVIOgtfX4fPN+/uPD" +
                "D9WVeR0SU8/+cqXTMctzfhLacaKfr2HQvqMHUddZgonNUtGzlv" +
                "grMgUXvbs4G47Qo979qnZ8859OyK/LwHzsLo+YG20zbqpx3US3" +
                "000UrOlIGzMJwfOVfGQmPqVUbBWXxTxs2tl5+i7p3jPOEnr09r" +
                "a54/zAWixpP/+/uZF/+Bn/OzFGTJb/N4jhF7nFmK3mHVRc9/xV" +
                "meAGcnG0s1uC4NvkJf9ZWeWTr0LGB+zkB+hp5/w1lZ/kvOLBM8" +
                "Dek5C3qGvfbPLFvc/hlmUV2eAx+4gnMefODc/MzyjdZh8oXZ/C" +
                "wfIz+7on8uAGeROLNSmziXgLNQnMtt4lwGzg6sw1qd+K3jB0Hu" +
                "c35mfkv17IOeham3O6Mot1sctzvAWTw9s27WhX1DbojbrAd6js" +
                "G6ql6P273yvXxnT70t34KzMPm5Twkos0Y4KzOmn0tOgbMD9d8P" +
                "H6CvQl8leNweoJSqVAnt06cy6m3xjSaZWx/ap69xHvrGx1bUYV" +
                "ngLGbctrivGgZn0ThLQTbyNo/9YbFlcS9hujJT");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value6 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value6[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value7 = null;

    protected static void value7Init()
    {
        try
        {
            final int rows = 40;
            final int cols = 122;
            final int compressedBytes = 465;
            final int uncompressedBytes = 19521;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtmj9Lw1AUxeWSpsl9IujgIrqIiqgUwb9ILYKKNE5+H1EHQU" +
                "GdFJcqtOIncHRUnCp2cHEv/vkUsWSwtSY1hZfyXt+5UPJ6ky7n" +
                "l3Nyk4YyvOuHFq3/6WzTGm0GqxVapMnadpqWgu+rtU+OvGC9TP" +
                "lgO0UzVvXX77d+VrOU9WMUbfgoKUWZyD0SOPMeOBvBeR+cjeB8" +
                "CM5GcD4CZw3PiWLr/XwMjQz18wn8bATnU3DWr/gMGsDPoX4+h5" +
                "+7cA67gEZdkduX0AC5HZrbBeS2Kpz5OjnO9jA4K5LFpdRj3GNT" +
                "T9DLiHPiFhponNt39pf9mVBufyC3VZvDrDLf1/tWRcp7Bs/gbM" +
                "S8/QDOej4n4ZemXsO0TgVo1BVzWAUaILdDc/sVuW0E5zdw1vP6" +
                "3DLXq9DICM7v0EiX3GZPam7nkdvqceYd5yAtZHJOMzjrV8KFBj" +
                "r7WQjnJs687ZTa9bNzBT+rlNuiN5n7KnBW7/pslUVfvY//MXBf" +
                "FXn97odGWvh5AH42gvMgOKNi5PYQNNLhOYn8edvNws+qcXZzjX" +
                "05uS1GwFmNEqPt5bblNfXwPolGfua5ZHJbjHXSz2IcPP/hvJAM" +
                "Z57vKOcJ8Iyqnm+CvcJq");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value7 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value7[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value8 = null;

    protected static void value8Init()
    {
        try
        {
            final int rows = 40;
            final int cols = 122;
            final int compressedBytes = 418;
            final int uncompressedBytes = 19521;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtmrtKxEAUhmXIMmsGH0RUxMYLy3rBVZdNFK0UtPIxLEQXSz" +
                "t9AQsVxEIsFLbRRkHZBWsbHyUuEVzUiAETmMn5DoRMJgmE/8s/" +
                "5xySKEofphIRjoYa/fVM7cdMqObUYjyqqAk12N0Pq8n4eLq7za" +
                "ggHk+pRrwfUiOm+uX++udoTFVTPd0ChLIJM5v+2lITvdz1s5nX" +
                "yzrUgV7RA7qhTZZ+1j5+tsTPNfwsxM9L+eXn/nH8bA3nOpypt/" +
                "9Zb4dwFsF5Fc4OvhMnf9R0a2gk1M/r+FkE5w04i+C8CecC5uct" +
                "NBLq5238bBtnr212evPeSxacvQ6cC7hu76KRvX42e3yvkrNum/" +
                "088nPpAM4i6rAmnEVwPoRzAeuwIzQSwfkYjWwNc5r+Wv4bcrqv" +
                "OqOvEuDnc/xMfHsnLtGAviqxr7pi3S5gvX2NRkL9fIOfRXC+hX" +
                "MB1+0WGlncP999jLx2+bE3n81/BuUH/OxgX3WPBuTnxPz8hJ9t" +
                "4mye8+HsXcDZTj/7QZac/QacHczPHTQgPyfm51f8LILzG5ztiL" +
                "53n13dOQ==");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value8 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value8[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value9 = null;

    protected static void value9Init()
    {
        try
        {
            final int rows = 40;
            final int cols = 122;
            final int compressedBytes = 392;
            final int uncompressedBytes = 19521;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtmltKw0AUhvUwSGwGNyAuQGqRIt7QKq1a6wUvOxDdg6J7UV" +
                "C3U8ElqOiLlwdXEEMeWsQotXRgJuc7EDKZkJfz5Z//nxCZTn4p" +
                "Wf8xsyt1aWajJZmXyfQ8JQvZ9Up6rMpONl6U7exclkr88O35Vm" +
                "dUlVrSQ8lGQg2kpMPZtOPH7ry5HwRncwdnXzjHT+70bJ7hHF7F" +
                "L/Qg/HXbiT+/omcVnN/g7GEOeyeHFTiHfZDDVHD+dMd5ZALOAb" +
                "4T13/ft8P0qAhlS/RABWdLD9hX5fmzHcOfVXAeh7N/+2dbZv+s" +
                "gnMVzqzb/XAercE5wLw9Qw/Qc24Om0XPKjjPwdkXznY5uopue+" +
                "Ec3fyXc3QJZxV6Jof5o+e6w7zNuq1Dzw04q+C8BmcVnJtwVsF5" +
                "E86+cTZt2+rO8327cHl7y52e+Q8wxLJ79AB/zvXnffTsoT8f4M" +
                "8F9udD/FmZno/QM5XmsGN6oILzCT0gb+fm7VPWbQ/9+Qx/Rs99" +
                "6fkczio4X8DZjxr6Am/C+AA=");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value9 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value9[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value10 = null;

    protected static void value10Init()
    {
        try
        {
            final int rows = 24;
            final int cols = 122;
            final int compressedBytes = 251;
            final int uncompressedBytes = 11713;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrt1z0KwkAQBWAZYrcXEZVgpSIqgoqoN7ITC89gmcpaxcLCA4" +
                "g/6QQFL6AInmANiwQMEVKkSHzvQdgNyTb7ZWaJ2PoTa69G2o/l" +
                "SlsHIkNpSdfMalKWnDcWpGLuG97VlIGZV6VvxrwUrcPX+p4/K0" +
                "ldR4h0NBNL1CT6u9kx9yutEfvnkxjqWU1Zz0lz9vr2jH37b/u2" +
                "w77NBL6JOfeA53Po+bxg34ZwXtIZwnlFZwjnNZ0hnDd0hnDe0j" +
                "mF/1U77gHrObSej6xnCOcTnSGcXTpDOJ/pDOF8oTOE85XOEM43" +
                "OkM43+kM4fygM4Tzk84Qzi86JyOZN5bAjmQ=");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value10 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value10[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int lookupValue(int row, int col)
    {
        if (row <= 39)
            return value[row][col];
        else if (row >= 40 && row <= 79)
            return value1[row-40][col];
        else if (row >= 80 && row <= 119)
            return value2[row-80][col];
        else if (row >= 120 && row <= 159)
            return value3[row-120][col];
        else if (row >= 160 && row <= 199)
            return value4[row-160][col];
        else if (row >= 200 && row <= 239)
            return value5[row-200][col];
        else if (row >= 240 && row <= 279)
            return value6[row-240][col];
        else if (row >= 280 && row <= 319)
            return value7[row-280][col];
        else if (row >= 320 && row <= 359)
            return value8[row-320][col];
        else if (row >= 360 && row <= 399)
            return value9[row-360][col];
        else if (row >= 400)
            return value10[row-400][col];
        else
            throw new IllegalArgumentException("Unexpected location requested in value10 lookup");
    }

    static
    {
        sigmapInit();
        sigmap1Init();
        sigmap2Init();
        sigmap3Init();
        sigmap4Init();
        sigmap5Init();
        valueInit();
        value1Init();
        value2Init();
        value3Init();
        value4Init();
        value5Init();
        value6Init();
        value7Init();
        value8Init();
        value9Init();
        value10Init();
    }
    }

    /**
     * The error recovery table.
     * <p>
     * See {@link #attemptToRecoverFromSyntaxError()} for a description of the
     * error recovery algorithm.
     * <p>
     * This table takes the state on top of the stack and the current lookahead
     * symbol and returns what action should be taken.  The result value should
     * be interpreted as follows:
     * <ul>
     *   <li> If <code>result & ACTION_MASK == DISCARD_STATE_ACTION</code>,
     *        pop a symbol from the parser stacks; a &quot;known&quot; sequence
     *        of symbols has not been found.
     *   <li> If <code>result & ACTION_MASK == DISCARD_TERMINAL_ACTION</code>,
     *        a &quot;known&quot; sequence of symbols has been found, and we
     *        are looking for the error lookahead symbol.  Shift the terminal.
     *   <li> If <code>result & ACTION_MASK == RECOVER_ACTION</code>, we have
     *        matched the error recovery production
     *        <code>Production.values[result & VALUE_MASK]</code>, so reduce
     *        by that production (including the lookahead symbol), and then
     *        continue with normal parsing.
     * </ul>
     * If it is not possible to recover from a syntax error, either the state
     * stack will be emptied or the end of input will be reached before a
     * RECOVER_ACTION is found.
     *
     * @return a code for the action to take (see above)
     */
    protected static final class RecoveryTable
    {
        protected static int getRecoveryCode(int state, org.eclipse.photran.internal.core.lexer.Token lookahead)
        {
            assert 0 <= state && state < Parser.NUM_STATES;
            assert lookahead != null;

            Integer index = Parser.terminalIndices.get(lookahead.getTerminal());
            if (index == null)
                return 0;
            else
                return get(state, index);
        }

        protected static final int[] rowmap = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 0, 5, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10, 0, 0, 0, 11, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 12, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 13, 0, 14, 0, 0, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 17, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 18, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 19, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 20, 0, 0, 21, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 22, 0, 0, 23, 0, 0, 0, 0, 0, 24, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 25, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 26, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 27, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 28, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 29, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 30, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 32, 0, 0, 33, 34, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 35, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 36, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 37, 0, 0, 0, 38, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 39, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    protected static final int[] columnmap = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255, 256 };

    public static int get(int row, int col)
    {
        if (isErrorEntry(row, col))
            return 0;
        else if (columnmap[col] % 2 == 0)
            return lookupValue(rowmap[row], columnmap[col]/2) >>> 16;
        else
            return lookupValue(rowmap[row], columnmap[col]/2) & 0xFFFF;
    }

    protected static boolean isErrorEntry(int row, int col)
    {
        final int INT_BITS = 32;
        int sigmapRow = row;

        int sigmapCol = col / INT_BITS;
        int bitNumberFromLeft = col % INT_BITS;
        int sigmapMask = 0x1 << (INT_BITS - bitNumberFromLeft - 1);

        return (lookupSigmap(sigmapRow, sigmapCol) & sigmapMask) == 0;
    }

    protected static int[][] sigmap = null;

    protected static void sigmapInit()
    {
        try
        {
            final int rows = 1083;
            final int cols = 9;
            final int compressedBytes = 130;
            final int uncompressedBytes = 38989;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrt2osJwCAMBcCM7uZxAxER/ORugRbM57WYOdYCeEHqZSjTO/" +
                "odwE6ZeZZ9gSwBAMCveVWmBWDXTqn8PqhVALBz13auu0bccF7O" +
                "FP2OOgTMlrP3MwG9LGuBOaYHUT8AgEwCAIA8BgAA4PsLAHMeAA" +
                "AAAPyzAgAAAAAAACA6z1sMPQ==");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            sigmap = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                sigmap[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] sigmap1 = null;

    protected static void sigmap1Init()
    {
        try
        {
            final int rows = 1083;
            final int cols = 9;
            final int compressedBytes = 80;
            final int uncompressedBytes = 38989;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrt3LENADAIAzBO5/NyAgMqYrAfSLZsiQAAAAAAALjlNXI5a7" +
                "MPAAAAAAAAAAAAAAAAAAAAAAAAAAAATPjUBcDuAAAAAAAAAAB8" +
                "VBG4oeE=");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            sigmap1 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                sigmap1[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] sigmap2 = null;

    protected static void sigmap2Init()
    {
        try
        {
            final int rows = 1083;
            final int cols = 9;
            final int compressedBytes = 72;
            final int uncompressedBytes = 38989;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrt1DENAAAIwDCk4xwk8BESWgW7FgEAAAAAAAAAAAAAAAAAAA" +
                "BwVw3yeQ+ARwEAAAAAAAAAAAAAAAAAAAAAAAAALGlQx2Eh");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            sigmap2 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                sigmap2[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] sigmap3 = null;

    protected static void sigmap3Init()
    {
        try
        {
            final int rows = 76;
            final int cols = 9;
            final int compressedBytes = 26;
            final int uncompressedBytes = 2737;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtwTEBAAAAwqD1T20ND6AAAODYAAqxAAE=");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            sigmap3 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                sigmap3[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int lookupSigmap(int row, int col)
    {
        if (row <= 1082)
            return sigmap[row][col];
        else if (row >= 1083 && row <= 2165)
            return sigmap1[row-1083][col];
        else if (row >= 2166 && row <= 3248)
            return sigmap2[row-2166][col];
        else if (row >= 3249)
            return sigmap3[row-3249][col];
        else
            throw new IllegalArgumentException("Unexpected location requested in sigmap3 lookup");
    }

    protected static int[][] value = null;

    protected static void valueInit()
    {
        try
        {
            final int rows = 37;
            final int cols = 129;
            final int compressedBytes = 189;
            final int uncompressedBytes = 19093;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrt2skNgzAURVFXQ0ImyJxQgOty6ZSQsPU7+lvE4uroC9m00n" +
                "5OHVoxvU7566m6U4oBpRhQKt6A7wEG7AEGGGCAAQYYYIABBhhg" +
                "gAEGGGDgtvW99aJtZwY+SsUbGJWKN3BSKt7AUal4Aw+l4g3slY" +
                "o38FbKHlAq3sBVqXgDk1LuC5RyPqAUA0rFG/gqFW/goFS8gZdS" +
                "8QYWpeINzEqlG9g+/iXrbg+clYo3cFfKGZFS8QaeSvVsoKzJi5" +
                "GN");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value1 = null;

    protected static void value1Init()
    {
        try
        {
            final int rows = 3;
            final int cols = 129;
            final int compressedBytes = 38;
            final int uncompressedBytes = 1549;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNprYGggCN2MGhhG4XCFDESpctMeDanRNDAaUsM5DTAAANGigW" +
                "o=");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value1 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value1[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int lookupValue(int row, int col)
    {
        if (row <= 36)
            return value[row][col];
        else if (row >= 37)
            return value1[row-37][col];
        else
            throw new IllegalArgumentException("Unexpected location requested in value1 lookup");
    }

    static
    {
        sigmapInit();
        sigmap1Init();
        sigmap2Init();
        sigmap3Init();
        valueInit();
        value1Init();
    }
    }

}
