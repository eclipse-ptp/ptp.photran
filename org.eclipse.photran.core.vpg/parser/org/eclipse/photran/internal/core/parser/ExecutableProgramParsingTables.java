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

        protected static final int[] rowmap = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 1, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 2, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 15, 62, 63, 64, 65, 3, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 0, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 18, 126, 0, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 8, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 15, 157, 158, 159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 109, 189, 190, 0, 191, 192, 102, 36, 1, 29, 0, 103, 193, 194, 195, 196, 197, 198, 199, 200, 201, 140, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 212, 220, 221, 222, 223, 224, 225, 226, 58, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 1, 2, 58, 3, 1, 8, 123, 4, 124, 15, 127, 5, 218, 237, 125, 6, 7, 128, 126, 0, 173, 238, 209, 212, 8, 214, 239, 215, 88, 29, 9, 216, 217, 240, 219, 102, 29, 114, 10, 220, 11, 241, 221, 12, 222, 13, 0, 14, 227, 2, 129, 228, 150, 242, 230, 243, 15, 16, 231, 29, 244, 245, 246, 17, 247, 30, 248, 249, 18, 115, 250, 251, 19, 252, 20, 253, 254, 255, 256, 257, 258, 130, 134, 0, 21, 137, 259, 260, 261, 262, 263, 22, 23, 264, 265, 24, 266, 267, 25, 3, 268, 269, 270, 26, 27, 152, 154, 28, 244, 271, 272, 237, 240, 273, 274, 4, 275, 276, 39, 29, 39, 245, 277, 278, 279, 0, 88, 280, 39, 281, 282, 283, 284, 285, 286, 287, 288, 289, 290, 56, 291, 30, 292, 293, 156, 6, 294, 295, 296, 242, 297, 298, 299, 238, 300, 301, 103, 302, 7, 303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 29, 39, 313, 31, 314, 315, 32, 316, 5, 317, 318, 319, 33, 320, 0, 1, 2, 321, 322, 323, 29, 34, 324, 325, 58, 326, 239, 327, 144, 328, 8, 329, 246, 241, 247, 330, 243, 9, 173, 10, 331, 35, 332, 236, 8, 248, 249, 252, 253, 254, 333, 255, 256, 334, 88, 335, 257, 336, 337, 338, 258, 180, 250, 259, 339, 340, 341, 263, 265, 342, 343, 102, 344, 345, 346, 347, 348, 349, 11, 36, 37, 350, 12, 13, 14, 15, 0, 351, 352, 16, 17, 18, 19, 20, 353, 0, 354, 355, 21, 356, 22, 23, 24, 38, 357, 358, 359, 360, 361, 362, 363, 26, 364, 365, 366, 367, 368, 369, 370, 371, 372, 373, 374, 375, 376, 377, 378, 379, 380, 381, 382, 383, 384, 385, 386, 387, 388, 389, 390, 391, 39, 28, 40, 31, 267, 41, 42, 32, 392, 33, 34, 393, 394, 395, 396, 43, 44, 397, 45, 46, 35, 36, 398, 37, 40, 41, 42, 399, 400, 401, 47, 48, 49, 402, 50, 51, 52, 53, 54, 1, 403, 404, 405, 406, 407, 55, 56, 2, 57, 59, 60, 408, 61, 3, 62, 409, 63, 64, 65, 0, 410, 66, 411, 67, 68, 47, 4, 412, 69, 70, 413, 6, 71, 414, 3, 415, 4, 48, 72, 5, 416, 73, 417, 418, 6, 419, 74, 420, 421, 75, 76, 7, 422, 77, 78, 423, 49, 50, 424, 79, 8, 80, 81, 425, 82, 426, 427, 1, 428, 429, 430, 431, 432, 433, 123, 83, 84, 434, 85, 435, 86, 9, 87, 53, 88, 10, 89, 0, 90, 91, 92, 436, 11, 8, 12, 93, 437, 94, 95, 1, 96, 97, 98, 13, 99, 14, 0, 100, 438, 101, 104, 105, 106, 107, 439, 108, 109, 440, 110, 111, 112, 113, 441, 114, 442, 443, 444, 116, 15, 445, 446, 447, 448, 449, 450, 451, 117, 118, 452, 119, 453, 120, 18, 121, 181, 454, 455, 8, 456, 122, 123, 19, 124, 126, 457, 458, 459, 460, 127, 129, 130, 25, 131, 20, 132, 15, 133, 134, 461, 21, 462, 463, 464, 128, 465, 466, 54, 467, 468, 135, 136, 55, 0, 137, 138, 139, 140, 141, 469, 142, 22, 470, 471, 472, 473, 143, 56, 145, 117, 146, 147, 148, 474, 475, 476, 149, 150, 151, 152, 23, 8, 153, 477, 478, 479, 480, 481, 482, 154, 483, 102, 484, 485, 486, 155, 57, 487, 488, 156, 489, 490, 491, 492, 493, 157, 494, 495, 251, 496, 497, 173, 169, 158, 498, 499, 500, 501, 502, 159, 503, 504, 160, 505, 506, 507, 508, 161, 509, 2, 510, 511, 56, 162, 512, 513, 514, 515, 516, 517, 518, 163, 519, 520, 521, 164, 165, 522, 523, 524, 102, 166, 525, 526, 170, 527, 528, 167, 529, 530, 531, 268, 532, 270, 18, 168, 171, 172, 174, 24, 533, 175, 534, 15, 260, 26, 535, 176, 536, 261, 537, 262, 538, 539, 177, 540, 271, 541, 542, 543, 15, 276, 544, 7, 8, 58, 9, 10, 178, 545, 546, 11, 547, 548, 549, 16, 143, 550, 18, 277, 551, 59, 0, 3, 552, 553, 554, 555, 556, 557, 558, 559, 560, 561, 562, 563, 27, 564, 148, 565, 566, 567, 29, 168, 568, 569, 570, 174, 571, 31, 572, 32, 17, 573, 574, 575, 179, 180, 576, 577, 181, 182, 578, 183, 579, 580, 581, 582, 583, 584, 585, 586, 39, 587, 588, 589, 590, 591, 592, 593, 43, 44, 45, 46, 594, 595, 596, 597, 598, 47, 599, 600, 601, 602, 603, 604, 605, 606, 607, 608, 609, 610, 611, 612, 613, 614, 615, 616, 617, 618, 619, 620, 621, 622, 623, 624, 625, 626, 627, 628, 629, 1, 630, 280, 3, 631, 285, 48, 49, 63, 66, 60, 632, 50, 77, 633, 634, 4, 635, 184, 636, 637, 185, 638, 639, 640, 641, 5, 642, 6, 643, 12, 14, 644, 645, 646, 647, 27, 648, 649, 650, 186, 651, 652, 187, 188, 653, 78, 654, 655, 656, 657, 658, 659, 189, 190, 660, 191, 661, 182, 662, 192, 15, 663, 664, 665, 666, 667, 668, 80, 81, 669, 670, 671, 82, 672, 87, 88, 94, 95, 673, 193, 100, 674, 675, 2, 676, 101, 102, 103, 677, 678, 194, 679, 680, 112, 114, 115, 117, 118, 61, 681, 682, 683, 684, 28, 20, 685, 686, 687, 119, 7, 21, 22, 688, 689, 690, 691, 692, 693, 694, 695, 696, 697, 698, 699, 700, 701, 702, 125, 4, 703, 704, 705, 134, 135, 133, 706, 136, 62, 195, 143, 144, 145, 147, 707, 148, 153, 154, 708, 155, 156, 157, 709, 6, 158, 159, 160, 196, 197, 64, 198, 199, 710, 65, 184, 67, 68, 69, 70, 711, 712, 8, 9, 713, 714, 715, 716, 717, 718, 719, 720, 721, 722, 723, 724, 725, 29, 30, 32, 726, 727, 728, 729, 730, 731, 732, 733, 734, 735, 736, 737, 179, 738, 739, 740, 741, 742, 743, 744, 745, 746, 161, 747, 162, 748, 749, 750, 163, 751, 752, 753, 754, 755, 756, 757, 758, 759, 760, 761, 762, 763, 764, 765, 766, 767, 768, 769, 770, 771, 772, 24, 25, 26, 33, 773, 774, 775, 776, 777, 164, 778, 165, 779, 166, 205, 167, 780, 200, 781, 201, 782, 783, 169, 784, 34, 785, 786, 787, 788, 789, 790, 210, 791, 170, 792, 793, 794, 795, 796, 797, 798, 799, 800, 173, 801, 802, 803, 804, 175, 805, 806, 807, 808, 809, 10, 810, 811, 812, 813, 814, 815, 816, 817, 71, 7, 176, 177, 818, 819, 820, 821, 822, 823, 824, 825, 826, 827, 178, 36, 184, 185, 828, 186, 187, 202, 1, 188, 73, 189, 190, 191, 193, 195, 74, 196, 197, 198, 199, 203, 204, 205, 206, 829, 830, 207, 831, 832, 0, 833, 36, 33, 834, 835, 836, 208, 210, 211, 75, 212, 76, 290, 837, 43, 838, 213, 214, 215, 217, 218, 219, 220, 839, 221, 203, 204, 840, 205, 841, 842, 843, 844, 845, 35, 222, 77, 846, 847, 223, 224, 8, 848, 225, 849, 225, 226, 78, 850, 275, 851, 227, 228, 229, 230, 852, 853, 291, 854, 206, 855, 231, 232, 233, 856, 857, 208, 209, 858, 210, 859, 860, 861, 211, 862, 863, 864, 212, 865, 866, 45, 213, 215, 867, 868, 227, 217, 869, 870, 871, 872, 218, 873, 220, 874, 875, 876, 44, 221, 877, 222, 878, 879, 880, 881, 79, 234, 235, 882, 83, 36, 46, 84, 85, 47, 50, 86, 89, 51, 52, 883, 236, 237, 238, 884, 885, 223, 886, 239, 887, 224, 888, 225, 889, 890, 58, 226, 891, 80, 241, 243, 36, 37, 247, 250, 2, 251, 38, 254, 892, 244, 893, 894, 895, 1, 896, 294, 897, 245, 53, 898, 88, 39, 248, 249, 40, 301, 102, 229, 899, 41, 900, 230, 901, 902, 253, 258, 259, 232, 903, 904, 233, 905, 906, 234, 907, 908, 235, 909, 81, 256, 257, 264, 54, 265, 267, 0, 236, 268, 269, 270, 271, 272, 238, 910, 911, 912, 273, 274, 278, 276, 279, 280, 281, 282, 283, 284, 285, 286, 287, 1, 913, 288, 289, 290, 291, 292, 293, 914, 294, 915, 916, 295, 296, 917, 918, 297, 298, 919, 299, 300, 301, 920, 302, 303, 921, 922, 42, 55, 923, 924, 925, 926, 927, 928, 929, 930, 931, 932, 304, 305, 933, 934, 935, 936, 937, 938, 939, 940, 941, 942, 943, 944, 945, 946, 947, 306, 56, 57, 60, 61, 62, 64, 65, 67, 68, 69, 70, 71, 74, 75, 948, 240, 0, 949, 307, 308, 950, 951, 309, 952, 76, 953, 954, 955, 241, 246, 310, 311, 242, 312, 297, 313, 314, 956, 957, 315, 316, 317, 318, 247, 958, 319, 321, 322, 324, 326, 109, 327, 329, 43, 330, 959, 320, 323, 325, 333, 334, 960, 328, 961, 962, 963, 250, 336, 337, 338, 339, 964, 965, 966, 340, 967, 341, 44, 332, 79, 335, 342, 343, 344, 345, 90, 91, 347, 968, 346, 969, 251, 970, 348, 349, 350, 971, 353, 361, 362, 2, 972, 973, 365, 367, 368, 374, 82, 381, 974, 392, 383, 385, 387, 388, 393, 395, 90, 396, 397, 310, 398, 400, 313, 401, 975, 976, 402, 403, 977, 978, 404, 979, 980, 981, 982, 983, 405, 407, 11, 984, 985, 408, 410, 92, 93, 96, 412, 91, 986, 987, 988, 252, 92, 254, 989, 990, 991, 406, 992, 3, 993, 994, 995, 996, 997, 96, 998, 97, 999, 1000, 1001, 409, 1002, 4, 1003, 1004, 413, 1005, 1006, 97, 6, 1007, 1008, 1009, 98, 1010, 1011, 1012, 1013, 260, 1014, 98, 99, 1015, 261, 1016, 262, 1017, 414, 416, 419, 420, 421, 422, 423, 45, 0, 425, 1, 426, 2, 427, 428, 429, 430, 46, 431, 100, 2, 47, 432, 433, 434, 435, 436, 99, 437, 438, 439, 440, 441, 442, 444, 445, 446, 447, 448, 449, 452, 453, 454, 455, 456, 458, 459, 3, 263, 460, 461, 462, 463, 464, 465, 466, 467, 469, 470, 471, 472, 473, 474, 264, 475, 265, 476, 477, 479, 1018, 112, 483, 485, 486, 4, 267, 478, 480, 487, 481, 5, 489, 1019, 491, 482, 269, 270, 484, 488, 490, 492, 493, 494, 495, 496, 1020, 271, 497, 498, 499, 500, 501, 502, 503, 504, 505, 506, 507, 508, 509, 510, 1021, 1022, 511, 512, 1023, 1024, 1025, 272, 513, 514, 3, 114, 115, 515, 1026, 516, 1027, 1028, 1029, 1, 4, 517, 518, 116, 101, 519, 520, 1030, 521, 522, 114, 48, 1031, 1032, 523, 524, 525, 1033, 277, 1034, 1035, 279, 526, 1036, 281, 7, 1037, 1038, 282, 1039, 1040, 1041, 117, 527, 529, 528, 1042, 530, 533, 1043, 283, 1044, 532, 303, 1045, 534, 1046, 284, 285, 535, 537, 538, 1047, 1048, 1049, 1050, 539, 1051, 1052, 1053, 286, 1054, 1055, 118, 1056, 0, 1057, 1058, 1059, 287, 1060, 1061, 1062, 1063, 1064, 1065, 120, 102, 103, 104, 121, 123, 124, 1066, 127, 129, 130, 131, 1067, 1068, 105, 1069, 1070, 1071, 1072, 123, 49, 1073, 50, 5, 540, 541, 51, 132, 542, 543, 106, 545, 546, 124, 544, 52, 1074, 1075, 312, 1076, 549, 547, 548, 550, 551, 552, 553, 315, 1077, 137, 1078, 1079, 1080, 320, 1081, 288, 1082, 1083, 554, 1084, 555, 556, 1085, 557, 1086, 1087, 289, 107, 1088, 108, 558, 559, 560, 561, 562, 563, 566, 1089, 1090, 564, 565, 567, 1091, 568, 1092, 571, 1093, 1094, 569, 1095, 1096, 1097, 1098, 1099, 1100, 138, 1101, 1102, 570, 1103, 1104, 1105, 572, 573, 574, 575, 576, 1106, 1107, 1108, 577, 578, 6, 7, 579, 582, 583, 585, 1109, 292, 1110, 1111, 1112, 293, 588, 1113, 297, 1114, 300, 1115, 590, 580, 1116, 1117, 109, 587, 589, 591, 592, 593, 595, 2, 1118, 1119, 1120, 125, 53, 596, 54, 597, 1121, 302, 598, 1122, 1123, 1124, 1125, 304, 599, 1126, 1127, 1128, 1129, 1130, 1131, 1132, 1133, 605, 606, 1134, 1135, 614, 615, 1136, 617, 305, 1137, 1138, 618, 622, 1139, 628, 1140, 1141, 139, 1142, 1, 1143, 1144, 600, 601, 1145, 630, 624, 110, 9, 602, 631, 140, 306, 12, 1146, 603, 1147, 1148, 1149, 1150, 307, 1151, 308, 1152, 141, 142, 149, 632, 55, 1153, 1154, 1155, 1156, 1157, 635, 1158, 634, 1159, 636, 311, 638, 310, 639, 1160, 640, 111, 1161, 1162, 10, 641, 642, 644, 645, 646, 1163, 1164, 647, 1165, 648, 649, 316, 650, 112, 1166, 1167, 11, 1168, 651, 652, 314, 1169, 317, 1170, 653, 1171, 1172, 146, 1173, 150, 1174, 151, 1175, 318, 1176, 331, 342, 1177, 1178, 56, 604, 1179, 1180, 1181, 1182, 0, 1183, 1184, 1185, 1186, 1187, 654, 1188, 1189, 115, 343, 1190, 1191, 1192, 607, 608, 609, 57, 655, 1193, 656, 657, 1194, 658, 1195, 1196, 659, 1197, 1198, 1199, 1200, 152, 660, 661, 1201, 1202, 662, 663, 1203, 0, 1204, 1205, 1206, 8, 167, 168, 610, 611, 1207, 1208, 664, 171, 612, 613, 1209, 616, 1210, 172, 174, 1211, 344, 323, 1212, 665, 1213, 667, 1214, 666, 1215, 1216, 674, 669, 672, 1217, 12, 345, 1218, 620, 179, 1219, 675, 1220, 676, 348, 677, 349, 350, 1221, 351, 678, 1222, 1223, 325, 679, 681, 1224, 1, 1225, 1226, 352, 1227, 1228, 116, 1229, 117, 1230, 365, 1231, 367, 1232, 180, 621, 1233, 9, 1234, 625, 626, 1235, 683, 1236, 181, 328, 685, 637, 686, 687, 688, 689, 126, 58, 3, 4, 629, 680, 1237, 127, 59, 368, 1238, 374, 690, 1239, 381, 691, 118, 1240, 119, 1241, 1242, 1243, 182, 1244, 694, 13, 1245, 692, 693, 695, 1246, 696, 14, 697, 1247, 698, 1248, 15, 17, 18, 1249, 699, 1250, 1251, 1252, 1253, 186, 700, 1254, 1255, 701, 702, 1256, 703, 393, 704, 711, 331, 705, 707, 1257, 1258, 1259, 712, 709, 713, 714, 2, 128, 60, 120, 715, 716, 717, 1260, 1261, 718, 1262, 383, 1263, 332, 121, 123, 0, 124, 125, 719, 720, 187, 61, 62, 721, 722, 63, 723, 188, 64, 724, 1264, 385, 725, 726, 727, 728, 729, 730, 731, 732, 733, 734, 1265, 735, 736, 1266, 737, 1267, 1268, 738, 127, 1269, 189, 187, 1270, 1271, 1272, 392, 739, 387, 1273, 740, 741, 1274, 131, 1275, 1276, 742, 1277, 19, 394, 132, 1278, 1279, 743, 744, 745, 8, 1280, 1281, 1282, 20, 397, 135, 1283, 746, 747, 1284, 388, 190, 191, 192, 2, 395, 396, 1285, 748, 1286, 1287, 749, 750, 65, 751, 193, 752, 753, 136, 754, 755, 756, 1288, 757, 758, 759, 398, 1289, 1290, 137, 1291, 1292, 1293, 1294, 760, 761, 1295, 762, 399, 1296, 1297, 1298, 197, 763, 764, 765, 1299, 766, 195, 767, 1300, 1301, 768, 1302, 769, 1303, 400, 770, 771, 772, 335, 773, 9, 196, 774, 10, 11, 1304, 775, 776, 1305, 1306, 1307, 401, 1308, 402, 1309, 404, 1310, 1311, 413, 1312, 1313, 138, 1314, 141, 1315, 1316, 777, 778, 779, 780, 781, 782, 783, 1317, 1318, 784, 1319, 785, 1320, 129, 66, 786, 1321, 1322, 1323, 345, 198, 787, 1324, 346, 130, 420, 67, 350, 1325, 1326, 1327, 1328, 199, 202, 788, 789, 1329, 790, 791, 1330, 800, 792, 1331, 1332, 1333, 793, 1334, 1335, 1336, 1337, 1338, 421, 10, 795, 11, 12, 1339, 1340, 794, 796, 797, 21, 22, 203, 798, 1341, 204, 1342, 68, 799, 1343, 801, 1344, 1345, 1346, 802, 1347, 803, 1348, 805, 1349, 804, 1350, 806, 807, 808, 810, 422, 69, 809, 1351, 142, 1352, 811, 13, 1353, 23, 812, 143, 1354, 1355, 1356, 1357, 1358, 424, 813, 14, 1359, 144, 425, 1360, 1361, 1362, 1363, 1364, 427, 814, 1365, 434, 1366, 435, 436, 1367, 1368, 437, 1369, 1370, 1371, 1372, 6, 13, 1373, 1374, 1375, 1376, 205, 1377, 815, 816, 817, 818, 1378, 819, 820, 338, 12, 206, 207, 1379, 821, 822, 825, 13, 827, 828, 354, 1380, 438, 439, 15, 1381, 17, 1382, 208, 1383, 1384, 440, 1385, 1386, 1387, 145, 147, 355, 1388, 1389, 356, 829, 14, 830, 210, 831, 1390, 70, 7, 8, 832, 833, 834, 835, 441, 836, 351, 1391, 1392, 442, 211, 212, 443, 444, 837, 838, 839, 1393, 1394, 1395, 840, 841, 1396, 1397, 1398, 1399, 1400, 1401, 1402, 15, 844, 1403, 1404, 842, 843, 845, 1405, 1406, 341, 216, 222, 241, 1407, 1408, 1409, 188, 1410, 1411, 1412, 24, 449, 1413, 1414, 1415, 1416, 452, 453, 846, 454, 1417, 1418, 847, 1419, 1420, 1421, 1422, 455, 456, 848, 457, 1423, 1424, 243, 190, 1425, 1426, 71, 849, 850, 1427, 0, 244, 851, 852, 458, 245, 1428, 853, 854, 855, 1429, 856, 1430, 1431, 857, 858, 859, 860, 1432, 862, 861, 392, 1433, 1434, 863, 1435, 865, 1436, 460, 1437, 866, 864, 867, 868, 869, 870, 872, 1438, 1439, 1440, 1441, 352, 357, 358, 1442, 72, 459, 461, 359, 462, 18, 1443, 148, 150, 1444, 1445, 1446, 871, 1447, 1448, 1449, 1450, 1451, 873, 16, 874, 875, 876, 877, 1452, 878, 463, 1453, 1454, 879, 880, 881, 882, 464, 1455, 1456, 465, 466, 883, 467, 1457, 1458, 152, 1459, 884, 469, 885, 470, 1460, 1461, 153, 1462, 471, 1463, 1464, 1465, 151, 886, 1466, 472, 887, 1467, 888, 1468, 889, 890, 473, 891, 896, 892, 1469, 393, 893, 894, 895, 897, 474, 1470, 360, 363, 1471, 1472, 154, 155, 156, 1473, 1474, 898, 899, 900, 901, 902, 903, 1475, 1476, 1477, 1478, 1479, 904, 1480, 905, 1481, 1482, 475, 1483, 1484, 158, 1485, 1486, 25, 1487, 159, 1488, 1489, 26, 194, 906, 1490, 2, 1, 1491, 907, 908, 909, 910, 398, 916, 403, 364, 366, 369, 490, 493, 1492, 1493, 1494, 246, 247, 1495, 917, 918, 1496, 919, 1497, 921, 1498, 1499, 946, 948, 248, 950, 1500, 1501, 27, 494, 1502, 1503, 28, 495, 1504, 1505, 249, 161, 952, 954, 928, 933, 370, 1506, 371, 250, 251, 253, 496, 498, 254, 255, 258, 1507, 1508, 949, 1509, 951, 955, 502, 1510, 1511, 503, 504, 1512, 1513, 505, 956, 14, 957, 499, 506, 509, 515, 1514, 1515, 958, 960, 961, 259, 262, 1516, 516, 1517, 1518, 519, 1519, 263, 372, 1520, 1521, 1522, 962, 963, 1523, 1524, 964 };
    protected static final int[] columnmap = { 0, 1, 2, 3, 4, 5, 2, 6, 0, 7, 8, 9, 10, 11, 2, 12, 13, 14, 15, 16, 17, 18, 19, 20, 6, 1, 21, 2, 22, 8, 23, 24, 25, 2, 2, 7, 26, 0, 27, 28, 29, 30, 29, 31, 8, 32, 33, 0, 34, 35, 36, 37, 38, 39, 9, 2, 6, 9, 40, 14, 35, 41, 42, 31, 43, 38, 18, 44, 45, 18, 46, 47, 47, 20, 1, 48, 49, 17, 50, 33, 51, 52, 36, 48, 40, 53, 54, 55, 56, 57, 58, 59, 0, 60, 61, 62, 2, 63, 3, 64, 65, 66, 67, 68, 69, 70, 57, 71, 72, 73, 74, 75, 76, 77, 43, 78, 79, 54, 47, 80, 66, 81, 82, 0, 83, 67, 84, 49, 85, 86, 87, 88, 67, 3, 89, 0, 90, 91, 2, 92, 1, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 89, 68, 69, 104, 105, 106, 4, 71, 107, 108, 72, 109, 73, 4, 104, 5, 66, 110, 111, 24, 112, 113, 3, 114, 14, 3, 74, 115, 116, 117, 118, 119, 3, 120, 121, 122, 123, 124, 125, 126, 17, 127, 7, 88, 8, 128, 129, 94, 98, 130, 131, 132, 105, 133, 108, 1, 134, 135, 136, 137, 138, 139, 0, 140, 141, 142, 143, 144, 145, 146, 147, 110, 148, 2, 112, 70, 149, 150, 151, 152, 1, 153, 3, 154, 155, 0, 156, 157, 158, 159, 160, 6, 4, 161, 162, 0, 163 };

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
            final int rows = 1218;
            final int cols = 8;
            final int compressedBytes = 3326;
            final int uncompressedBytes = 38977;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrdXU2IHMcVruotKaVhjVvKyNLBkB55BcOiKD7okGMvDME2CR" +
                "mBD74YViCDLjnqPjImKNLBslc4l4CSHHzIIQfb7HkEiw8hhxhd" +
                "DevomGMOOae6e2anf6rqe1Wve1dJG0lGT/Xq9Xuv3m+93u9f+3" +
                "r38ftfqL8kyy8+2Pru5g9XjrefTC7dn+2P//3pbbX4/lcHu49n" +
                "3+w8SpaHBr5bwu/8+P7s7vg/NPjnu4+Pnt54r4BPvtt9efl4XO" +
                "I38NfnBPhrB4A+hN9PnxJSaCHEwvwSW+bX+fG2+T0TIhUP9oQY" +
                "/P2Y+AuyDanOB+Ffv3rtUc31PPqX259fS5ZP1UiINz7Ino9+kI" +
                "/Giyy5OCuoTs+ef1j/DHxm6C/0Txr8l4/VWv9O1s+e7oyK9XU4" +
                "WX9552v7ghj9ThmRJcs/CplmYvyhSicX74r98Z9fz438fv31Xx" +
                "8fHf70X5eeH95+68XPXl45vvrkn7+/f2TWP7s9/Pni8r/Un/zp" +
                "9nuF/ojno5fykQrTHz99b16Qhn87YpEsF2LL8O+ND7fTSVry79" +
                "M9iv4PzZ8e+GvX369o9mNg+ijna0j/wFxf6Y9q6s+duv4A+6tF" +
                "Ure5SfH/K4uciuH9T8PYZy1tyE9B/mz/xpXvGcc3ZwzvMDQPix" +
                "+Ghw/Mn8H9H4CD/Zv2IVnHZ2lD/z3xx9839vXQZv899vewH/82" +
                "LH9I+uOLz4aPzxNHfF2etLGBP1xDpPlPXTbwRZl/yJwff6zkv+" +
                "Py/8PnT0OfP1L8WOUfosw/WvHjwSo+Xh7OT+Ljg3L9M2r8MKvo" +
                "n1f06379y7Dxo8plYpWfSmeF/Njwrn7oun6g9Zb8tP187MtfER" +
                "zlpwh+VMGr/CQr85MNfM6Hc/NnLv6Q9fNqfRpCH8l+cPLrmn+b" +
                "V+tFPb+A+TPAj9ZHwEVj/4Hz4/V6t308nf1d6+vweQUXdbg1f6" +
                "/nX0B+Q68/E/hkUPy6UR9xxY93a/GFp/5Q+qf3D5v8vXOp6f88" +
                "cBNFJbB+aT//X1HsB8zvof0Jxq/r+FF9gAtvns8X1fn8U8Xfn8" +
                "wp9Qn/+lD8ooZfxL2f6PP9rElxrR5xVI8v2/6Tkn/rzBvcoPqM" +
                "lT7Lo0XkAxZy89ezhlv1oyGf3vcX8fQ58ztnfl+HW+0f0/6i+o" +
                "En/1WV/jjt8yHFPlvwNw4Fyn9Q/t3CL9tHEq2PgIuQ9StzJzca" +
                "I4emrwHn1hfq6236WYuPF5v4ufSPXxbxMcwvhTzvyy8QnFD/YO" +
                "Hfftf5fmX8j+sXvPpHHT6v4I34GtWHMTyzpOSh6z2P7Nnx5dT1" +
                "GUBSHUil/fp5soFWsr5/ssav12+o1qibzNSZiYCL41/9tcrF+e" +
                "LPUYk3EVV8kW4wSMC8QPma95NaF+8nq/f7RfF+ymz6dvV+AfyX" +
                "MfID/OmTPrt+AP08gSt7fQroF7JvGz3UHdtPp7+tH2qFV2L6Cv" +
                "2u8/d8m78u/avw4/fzGwBVZ2Hpncerw7Pxj72eb7Q+t8o34eNX" +
                "kfbXtX9f9g/lL0C+6Pz4KsA553xntPMJ+aubdqhjvzMo/8TF6U" +
                "UI/S6469RkTPnTUlZMP/P8CWhfZZVwtf2DpsG5/o97vjQ4v9B+" +
                "U/1TaPxE808E+w5VyMt/aF94/gvJ1x7feexPHnj+sP4g/ebFN6" +
                "PMIpxaiU671jv8Q2h8H6qfOdX+ROYfgfoXrP9t/Kg/CvSbcP/3" +
                "Y05/ldufRPK/fvU3Uo9E8pnekuKZQfrtLZ1rNU3Ez/WDdzNa/L" +
                "CJj2nyT+lmME4+m/PP7S+SbKYPBvi3WNGZRd8P7ef+L6hfbLvq" +
                "F6dFn6t/WfKXZNMd+knWb8f9PWb/smu/LjvyT0emAehH/UNEn7" +
                "O+rLr4G/VFIv6I/mP3/qRvPZTvxGu/cH0886d/SD6veH8wYr0I" +
                "W4/iF0LO6IND+etmeayVXwbTH2g/UH8T+W/S/SRO/5RbnyPHiR" +
                "KI0w7n9xcD5dfuL4L6BLc/vQDxFbR/buOkaOsz9/6Swr/Me04J" +
                "8XNofzCovxexf1h+BW1BTH0gpL5Eos/Z31zpYB1uq32u4dKy/k" +
                "zhQ/en6fazfn4SsnwY/c0vW/cfqXDR6v+i/qs3frWsD+ofw/lM" +
                "0N+1wEUdzu9f8/rDdf3Q1vxEgP4j8rt+/UL3WwqiygpocVI69a" +
                "/l+v6iXFxy5Q+8+IhanyP6H4sbykD8QV0fEq4G1Uf9/T9B9b8y" +
                "qr8kxD6PP9NV/XQxzpNc50Z+nyRKG8V9pziTWODt/lHewo/gJ/" +
                "Q76BtlNeNvS4GQfJn8Efea61f0J+36XCF8S33u+tWPKrhawX9b" +
                "wUUbLuxwpa8LAxcPiv1/ZDY08JV+6VV9La+7vY39odkniL/gT+" +
                "KrT/rpR/Qh/iD6kX1H9HXg34bJx8BL/n1WvKMPf+T7rfE/6OB/" +
                "m6g/E6986fzhvd8w8ifqn2f/Ln/G3fOVUM7XOUCfjDrfyL6Q+z" +
                "c6rr8Ycb9hO6S/w90f1y2Y8YH28wzf/wHx4UMANw7az59pWH+9" +
                "3R8M7U/mPccf8H4Pwk/dPw6O47/1K8jN/SshMur8Mj0+jIP3vn" +
                "8O5JuHxle8+Bz3V+B8Brg/cMb2B+Ll4vfDcf1tYPrh/bRseP5r" +
                "r/yB/Ub6M10VnKS05Re9608uiM8mblAlfeea50NmIs/69K+hD1" +
                "F/YH5r66GG5K9gPeT/ifyFXf6IPlL8lDj9H3g/eP8klD7hoi9y" +
                "fdT568/+ovkfrv+m33+Ji6/j6ldc+36K8UvXfwfej9P+OqZT/6" +
                "ak+i/uP8L6cl7FL7Jhn5N1XuBc/7D8V4vVmgLbO5v+R/rKxA/s" +
                "+CpQP7n5aah/jaS/v/iAKz+6XNb/OO3ZvxuW/2P8/Jbh/Y3lhV" +
                "x8NDXu6eIjbSKUZQ/57eaRg8SXQ8f/FPlYDlkqTym/3JeVwdlq" +
                "0a8D4TIOfpL/2ftneP9MNOxvF387vlK4/6MC+IP4e6+5/iS+pL" +
                "4/zH9BfwHEF9MikxDzdHGrWP3EOKGdzMTn6ci3f3B9AMyPDAkX" +
                "EeuVG06qr5xefEW7fzmcfYuff4nJP9OI9a3zHZzf91X/se9PqF" +
                "/768cPxV7qj391+ZHdMtQ0+UtaXqyYiX117TyN/tJ+Ne/vNfgH" +
                "4MKBPzsV/mtRvyOY1v955pZPQHzCrZ9wzwfff/ifkf2vF8T4HN" +
                "q3kV1+i57sa0B+LoepX9r0p8f6/cD5GSE/Zvk3fvwamB/ih9Cf" +
                "68//G/7tce5X9Nj/kEw4I//xmSZm/IT7R6B+FFzfCoOz4zvAb2" +
                "Z/bujziebr2Pzj1/dY8o2Lz+nrI+bTGioD15P5v76/3XajE6b8" +
                "Dm6C78PdZM23nbH84PwU9h/o+5WE7zMF8CfnnD/b+2l//EXpP9" +
                "Uzw3b9HRpIP/1w/kxbvv+zvn9CWI++f8adX0P0nbn97LV+8iLO" +
                "/4frT9qf/2G8v8T2ke8/of1FP1/Im//D+SJAP2E+6eqq/tF+2W" +
                "uVcmaynkc76hs++KD9BXS/jSA/Sv+IM7/mr+8wv99F+H4Na75N" +
                "wPk1pN/Dxqfc+TZu/IqmhxB99Plhz3xz4oZP0l9KAy/6K1L8we" +
                "yaZyLXxQUjrZt7x81Ps++HkOuD0lvfif5+GHd/ZLzI3weU1P7B" +
                "tr1/IGn055GK7ILD+laM/9s8+Oc/UvGruPor934Xkn8oPPJ8kL" +
                "9PGaq/zO+/Dn6+yOefyx//g/vvvdmftvy98z9s/WTf36Gev2H2" +
                "x/jvEc8Xsg/nrPXTzvxUdz6MVZ91zZ8lbvwN5YLzayD+8OtfIk" +
                "j3B3n1/9D8WwXZ97Z2LOzxmRdefxxw13r6fGGb/833w3DnfCEp" +
                "eZPx9ul/BK6Y8HP/h3D6+Ubzqx79DPXPre/PE+bXSe8X9/5o/r" +
                "YH/njh/fkfu3+bpLNV/ml4UeSfosg/y6qr+f0axB8wvxtVH6fj" +
                "j82PtSX/qtfHENzfH4y7/0ynv7x/NWLMD1vjg4D8K3R+uCv/Pe" +
                "U7n238tvz16Jsbfyvoe8vQd+X4zVZ+KTn5IX7/nJh/h9QXEP9s" +
                "3weLqw+8cj8/vZ0XDFy/6KF/xLu/1sv9CVb+0wN/3T8fh7v+zO" +
                "8nw59f5elP5j3NP3vsM/5+Mlu+e5z5NPv8ryrnfx0ZmM2+ueEn" +
                "+Fv1I7maL/bsT4LT9u/adyn6eb/e+ON6P1J/w4tfeOYTiPFP/P" +
                "wDO/5j1md1FaOm2eZ8lJ9NLecjU9GZz46uTznOH4wftbX/iO+3" +
                "9wXX1vgwIfM3s8AVOf4jzp8L9/fzSPFZwrCvkeev6yF8q1/V/h" +
                "Gh/8isP/LoO934+EUwnGA//N/HpvcvAufXqfdP/fPlne0XID5a" +
                "uPIjR/22XR9ZFvsri3117e/6PnjNjiTw4FnmM6dU+TjmU6c0/L" +
                "g+wMPv7K9NPfWpBv8RfgDXjvnVKa1+Tk6wo+tPfvqg/pDiN3d9" +
                "hjv/gX6+FdxfeH8+IenvhoQPHd/+F8hRz+M=");
            
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
            final int rows = 1218;
            final int cols = 8;
            final int compressedBytes = 2912;
            final int uncompressedBytes = 38977;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtXc2rXEUWr6pXyZSPJ95oJ85CsF6M0IQgLly4vIGHODKCAy" +
                "6yEToQIZtZzv5GRGKySEaDq4FxZulSJOsWwiwGFxPcCgn+CS5m" +
                "7b237+vu27eqfqfq3Op+L6Q30Zyc+jh16nz8qk5dIdqfEr6fCd" +
                "N/+fCry3cfPbjygZo/vHb4+PKv559M7h++/LejG5P/v/SRrjp+" +
                "EWj/qlBSFEK3/6tLcbb5c79lVGO0n9j/dDFtwP/Lh19fvnv0w6" +
                "U7zfj2Hl9++uqTg/vXX2nHd+8veHxcfv/PjkOPkK/uEdQ483tm" +
                "6J10ykj+4U8/m/LJRF/+pFuEufl3PX5ML0P29wSsbzvxYrV/dI" +
                "/xlOtn69+OHuh9Nf/mmqzt4/knuvVvs8n3nX+Tw/Ufs/+T4r/k" +
                "dukFLX6hxx+b+um1/3F0vP/D+2Mk+1cM+A3NP6F/Auc/azpWvv" +
                "7h+oibHf+ee/xk+3jGHR8C+eLxHTfD3d8ovtgRnRy/IH5J1Z/Y" +
                "8RP9X3L7RP/FlT83fti1fvj2T27/ZEH+C+i0/Fkktw/o0H+9WM" +
                "//42/0d018sff4rXb+i/jit53kp2WqA5fAxaT5txHyU4QfBOna" +
                "HApjhKqMlK2Lem9S96jrRX9b3Lpa/5eRDV1Uzfwa+tk+nds+tH" +
                "9O/6Tjly99/cLy9boYC1yQDeAgOIRR0MXZWP1PHP+W6ad9/GPT" +
                "Y/HDQXxrUXyM4pOE/dH8qqV91PXeK+yKX7Z/ffs4STKVbpuqGQ" +
                "rb/FUhiiMx0xfPjoAf5sE/T7D+b/BPasneXplAKfT5mrmybcZV" +
                "psZ3MfGh6fOXi/Grsew3u30ePyn/CvZvgfwsxneC/TP5sX1g4X" +
                "twfybJRZPjwzz4kk6guzcYXb/cjfP1kzx+j3yZ43/Ri1/+5sc3" +
                "dIb1kan+E/0+o+EXuewDYf5O+dpR5DM/+OqimtfrK8SFa/bH/a" +
                "fyzqSy6txRGwZg+5XdPoL8Fue/zPGD9lv5lQ8OWvmJVn66Jz+Y" +
                "f/Hkx8YfRV75wPUJ6jHu/7UX5P6X+pKo1LwSe3X8euGTg+J6cU" +
                "PMJv++dxXTkX7U/umtux8/7I//+str/jEzP1v/29XXPeNQxcAz" +
                "Mfr580L//rWY3+uE/Q3HB/YP7J8cH3cCKH35f5p/erSwD277ut" +
                "ifchj/UkC2gorPoPaJ/Sefz+20/zT7OF58Ml9ff4d/cOpvcv7w" +
                "c3R8kT1/YdpHbD+X+MWmsbpIs1+R+c9W8DGuX+b3r1jzixgfxL" +
                "cgfhbG35LzZzq+FvqNh/+ICPsTp99VkgJS8fU0/H2DT67cndSb" +
                "elptcHTaZUk4JfP+JZZv7vieK/8Uegx+kzl/ZOZXfPwg3P7BC6" +
                "LOP+q21fxbIev8Y/KJLg7PtfnHS6VDAaPjz3Hw3WJgP0wUf4je" +
                "SqgqHDxzgh9B7Zd58aUo/LjYcA6CcH4bPv+F58PZ938qPkVdH+" +
                "767Zof0I/FWxWlKk3TWaW0UW2/ar6GP+je/n1m5m9mvPkh+Uw7" +
                "xmqykG8pPu/Jl6v/4H5TAr6e2L/vd3MU+++jv/nHv0qzL9TfG+" +
                "Pzz9rC/ecdUxo9VeJdc+tPFvIfFn+W9WCa+5lS/KPmL62o+esF" +
                "mpp2bcH8SP0ril+WnvY/7bf/xaJ90bW/azqKTxC/Nm+Kmi5uNf" +
                "P/Q81S0zv/YRb+5XCDPunR4fgo+YlKP5+gy0/mWp9WPr72tfkU" +
                "yDcs/9zjH65vXP+j7M9o+yOe008L3SBWFN8bRCbef0rMj5Lrp2" +
                "LxKQ3ANrcQUfzPrp9t5xFBLynjj4kfUf9U+4GIsfGJaeMTvnxz" +
                "558x+WkC/tHwq5z55QyMfwbnX63+sF0KvgYwE8dnUvHJlPg94v" +
                "5GlvWn07n3V3LXL6SdQ+it0bn4Lhf/LZvzx9l9XW/hC4WY7z+V" +
                "nzVMzfmj8N/vicbnRdb6Ac79p7HpVWz8hO2DduELclMCnvkv+N" +
                "WgvlLWfsyS7H/Hf2aNX6/xg/6Z9Pb84NEPV/7b6PcbtX6/+uQ1" +
                "fH4y4v7F+JzDP+qVf4T4kwuh0M4IKiM9HL+F+HH9kFwA2SKE7+" +
                "16/jnl15//kB/R+z13+9dkGn90+9T839Txc1L+7zr/6uVnZWT9" +
                "TTi/jG4f4M/Z3wexYH1tgn7EjM+C4IBrn8P57zI/M3Xyu8zPxD" +
                "I/Y/vfVZ22+/4fPB8G8knKrwR9fOT8rnvfYH7sv5b7J6j/pP2t" +
                "NuOjXnwC5m+I40+8v+Ol3xbE+6OtxIuV/dCLKU83N1BQf8Sgf5" +
                "r+Q34ivf++he7jZ6H4mr9+THxNeN4HmY6jH/D+71jzc78vknZ/" +
                "c/X7aVGfc8n3vtBPq/qdhy46zC/33fazotufuF8ZSc99/ycL/5" +
                "bvZ2TFjyJxmYqiBIH2EX81Nv+21i8RX6Tun+h7jvaU6OcI+BKL" +
                "Xu4Y339O3yndthrwP/3j2/V/XJnX2+z6tA6Pz91pELS54N9vTM" +
                "VHn6/P9v1jkbC+SD8y+x9D7ddnX8Pv9/HpIdluY3/Nwu8Lktv3" +
                "jR/MH9//649viW+ZjPoXI9/yhNOZ4582dl58VFTvNH9/X7wvLl" +
                "ltVLFviPo5wvhP9P3rXbfPpafazx5+qrLZ99zvP2f3T9T8jHF/" +
                "LbA/cH1FWD8J79ew3g/0LnIhifLr9K+HH2+sj4rN36LxG/r52h" +
                "B/zKp/pieyYv2fW1K/ud9/Z9Kj64fI+Nra+dso9z8T72fy75di" +
                "+ylC+L/ltu/ZH7azL9z3XQTz/h7Qv8z7k73+lPdPSfvLQ3S2r+" +
                "ntZ/G/IsK+89aH4P+I5+to/c745Rvw7+h9K/b9xOzvI4X3N8k+" +
                "2ND4PO+73ejOh8D44PtKzPeb4PsgzPe/COe/zPeTIs8X/fSI94" +
                "lGjJ9h/X7m/pnjw+erYfnj+7nw/RqefYTv28D4NPH9YBuTX6zu" +
                "p4jU/CIVfxwrfvblLzz/qPois2tUSw0xcP6xKnroi0lS8g8lVu" +
                "+ryM33VdLO75+p98FHkk/a+83c+iMt5NlljrvI/w4Wkyu6+jHu" +
                "+zVh/tz1Ixj/HKF9Dn9UfF9AFGzw/oD/fd6HzvcHyjh8LDc+wa" +
                "Xj+r28+smls993gt/fuYnwlcT7k8fxw03n/Kj4Dawfh+8DpPIf" +
                "329d1s/L9fp37/3WFPyIgD/I3s7XCfI545kfqs934Zgx9ZFcOh" +
                "Uf0Vn84yCZqkB8UKWeX/joub/PwMWHw/XfY3wfWgf3B7M+PLb9" +
                "If4tOfjbePGL1/7zvg9BO785zfF7eCPm/r6qQPXpSK+o+HwaPz" +
                "f+58Y3OD7jxm/U8bvl09ZPl7366Xoh9bJ+2k1fq69e4i8e/BrH" +
                "X1d59S/Wc75E11/Bqf8F9cW4/pfXP/1957T7Ufz3SZn+nYLvqX" +
                "D+EPw+WNL7hILePrJpcHwDfg0iPBFTP4vr36Z9/YuO36j4PvUC" +
                "xhjxIT2+Ed7hUvExZJ+Y33fYOL9Ra+c3xRj+j/19lxT7RMcX3f" +
                "jIRv0s6X0Nt16Q6ie3Xb8Ziq+rgX+Ks89VZPzt7T90PhJzvkGL" +
                "b3D9ZOB8nWa/fPkT9/tcaAOE60OX9D1Al6l0Xv0s9/u8+Ps2nv" +
                "vhU7p+sfTT7d+2ph/bkA8r/x8BPwjHhxn0b/U7/v7ZB933bX7d" +
                "/P4Zjg/X/LM6HnwRPp+I8Z/Qf0ecz6TgP6n3g42gnc8z8/sI/E" +
                "M7Qj5S/KIY8m9jFNr5tk6TPys/I73f5j6f+552Puc8H9wavrL7" +
                "+mfu+Rmy36B+Cp9/BfnR+PD3N/Pia6g+ZZT8hHU+P5b+Iv/tPr" +
                "9s38+ufVRlpGyX+L1hfUio6hOer9wO2x96fQo6f0q9f2fl8Bwk" +
                "6v42qn8B+JBdvJ+20T/9/iPPPqV9HzumfoW5Psz4Jc/36WPevw" +
                "6ff+7+fk7e89mI74fIpPiU+z7N7/fXH1E=");
            
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
            final int rows = 826;
            final int cols = 8;
            final int compressedBytes = 2038;
            final int uncompressedBytes = 26433;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrdXL2rHUUUPzNvDOvjhax681EE3BcTeDyCWFhYbuA1BgUFiz" +
                "RCAhHSWNpvUkhMinwYUgmipaWE1DcQLOyCbeCF/AkW1u7uvd57" +
                "d+/O+Z2ZM5v7cJuXl/Pm68ycr985M0T/fYaGvpef/7h/9+DJ+T" +
                "t2+vTK1ov9V6cPd+5fe++7g+uTf+596SoabO9W2j/cv/v80cXL" +
                "TfvdF/uvTx5O7u++27Y/8cW8vfW3JypI9jHtLdcO9J9RSdZQPu" +
                "/TlXSs+bldE2Yd6+aH+CPg363eQC6MPzz/Xx6v9/+rx+43O31c" +
                "7/+H7f4341+d/L3Yf44FGc+fs2+b7R/cearstKKtvKBTX+/k1/" +
                "LrdHXy671Lov2xqxO2zb/npzIXjE/tPq58JU9m2pu49vz8nu88" +
                "PGfLR26b6NSV4tn2K3NnUhX2nYNmee3+a9vz8j/8ueW8Mwr7qm" +
                "D+mPnfGbl+cCHyO9i/Fbfn+Tep/+j28k8MuZN1R1XRsN+UAw3K" +
                "Hn8R/zdMt90tLVa4XwSd+yxMTMX7j88H+62ur+iuXri+ImJdbr" +
                "195Pqx/dbaV2L158vjtf06qPVPYz9MPf7JQze3H09PdPRPpP7F" +
                "9EvD8rsnkl/Ufjf/zNQ/bE5bhn6qpbosqMxcZinLerbHhPkX3P" +
                "xcwP7c4Oli/Q3337BC5B9ftf4LZ74x2TbZB7RF9HPN/+8/zmr+" +
                "140/yW5+Wsjmp6Ev5v/WKPO/cObbLv2P3vrA/sn5Y4CAmY59sj" +
                "75mDbrd8v1R8mf84+/bp+BTkL2YY/XP0r/GQyulu9Wvz9/cvHP" +
                "Rr9/UMcHpw/PdvQ78l8k8Y1V2Fd1e4+Syk3fpazaznKRA+Mnl4" +
                "n9C6X/PH78zbNpOosfdi438QM9235t7rhF/JAP2G+0+BKwpow1" +
                "9L72GXX8+3JFZIPkV0ivpPa06I2Pzp12fon6j1lfu0IzeP6151" +
                "M4fwuOKKY7JZ02Rd/r8r8M9f8Kz/6J7TOwL3z7GT7k/PiQeP4E" +
                "5j+sH1r9N/XgJznGT9y6SOQdXbWOT7kg/DHKfgfipyp8tKdfK+" +
                "A/VZz9+mumH36Z6Yf3W/3A799g+1X9Mui/O//8o+Mnn4ON9gfY" +
                "L/X5ENg/lp41hCb+7cUHeyyDrNQ/wfG9EJ/bEP8xf1h6Evto/c" +
                "tf4BdZPf4Cv6AB/MJEQtiFRD8z/q8gf6Twf+X9k0d/xfj/CB90" +
                "cvWROj6JjX+yuPHj8EmXcP0x+GzvfA7jm7+nwDchf6L1oxR/uT" +
                "Gon1b0IviudttTrz3mP48fYXqE/UNfwvxKtnY+XJj8ieBLv/+L" +
                "+aebX9b8VvdfLPs37X/fnrvCifVDOdb+DcuHPL+H4id2w+LxWd" +
                "g/oI+MD2rts8g/shr/AuknnX8D8TVU36CujyBoQLj4Stse1SfI" +
                "6xd88qvEZ5ft4+oPUuenyzV8AORXlP0nzJ+Tpj6AtZ9L0L+rJo" +
                "0+fzxbgpkPUXdoBPmbAPujHB/XJ4jH8fGfX7/EP9bF9zp4JYoe" +
                "gK9H1Qfh/Lxcv94A9p8f35dftsv8sh5fYsePy3/bRPlv2L84/0" +
                "D+/Igdwg8KYftMGx+lyJ9Qgvy3UkDHrk/U1j/H4TvB+FOo/sF0" +
                "oXNkAuNHmfwL12+78V26/NVC/rMtMyz/mfD8E4jv2PyKjcanxP" +
                "6TJP86Qn6lrx8qWfybDt+H+QsVvi/wbyLlE+FXt0mc35OsP9r/" +
                "QvUVYH5B/B04nxvnL5+/ma7Gr9TGr736mUJ5/nX4A8IHcP5bjp" +
                "+7AaWJ8e2Cje/E9tlzjnfzg3n9rp3lv2iW/6I2/3VOGD/Gzy8u" +
                "v0Wh9i9BfigsP2CT5QdA/+seswv2z5F/yeQHXC3+9Umpu63pzR" +
                "SPTRrEu2bqR3TzUif+5vGj2PstcH1z/8LbP58fgPi28n4Tpiey" +
                "Txp8wbLyp/MPlPiwHj8G+C6gC85HaPzR+XB+BcmPSD4E7d/Q/T" +
                "ifXGzqflQq/DyIDy4Z/qjXT1r4YrX/XDp+JY8vAX6I8EHgHyN8" +
                "THS/xILzw9C148fjm3vS+A7df1Hih1nk/oXis0B+Y/VDsvsNjP" +
                "KxGvns0aswfBXiR4D/uD5Yje+NWx852vxSxefFqPXPAnwCGSi+" +
                "PnRk/mH872jzd+z6fORfx+H3lEy/huhPp/ZfUvmPKfMvCB9Q4g" +
                "uwfrJgzzfCB8T4QTz/te9nqOr3kPyj+E5dP4D9pxTtLes/2fj4" +
                "Psn92zHPh9Z/1L/voMV3UH2Pjq6O/4X4WRa7PhSf0hB9YVfU73" +
                "sAOuxf6x8B/QTjW6X/G99/6PsgkfGlpH9FfD/O/Y+V49Tq1/uu" +
                "nsKpnKa1f3ir6bTRrzTHR4XKTVv/Ynz4Fn7/bsz86RH378evT0" +
                "D38/7v6wf2RZl/DcZ3Sol969UPsfevkP0E8YFW/6a536OoD5Le" +
                "D/HRA/233vrE+Zvx/FtdfDq2/4jw0ZH9K0hPUZ+tel8F5TdIeX" +
                "8d+X/j1kcnqg/h7vdJ8N/o9lr/YuP2T4mPH/X5wfxCCnxR5B8N" +
                "S538/nRk/ZGyvkZbfyC4Hzou/hD1fugbxV/C8nOh+Sdo/+H9G5" +
                "H8mU58HX4/yNc+Xf5dYR9RfpV7P1xbvwsPQDFufnLk8ePe9wvx" +
                "77T5Ea1/rcZHE92/TFG/E0OPkR85XY1P6t9P18evRxnfehPjM/" +
                "yD9ena91VGpqP4Fce3svyT1/5K6Ar523z8o+tfoB9Z/avdX/3+" +
                "o/glAX+58f8FRFL96w==");
            
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

    protected static int lookupSigmap(int row, int col)
    {
        if (row <= 1217)
            return sigmap[row][col];
        else if (row >= 1218 && row <= 2435)
            return sigmap1[row-1218][col];
        else if (row >= 2436)
            return sigmap2[row-2436][col];
        else
            throw new IllegalArgumentException("Unexpected location requested in sigmap2 lookup");
    }

    protected static int[][] value = null;

    protected static void valueInit()
    {
        try
        {
            final int rows = 59;
            final int cols = 82;
            final int compressedBytes = 4746;
            final int uncompressedBytes = 19353;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNqtXAuUnVV1vuRNVgYkRIm4EMRaTArVJmkIQpszjwtj7IIuHx" +
                "TawJCA1jBOHhpDkkkW87/unbl3QmM1ycSihWkGRLTFdlVahRRB" +
                "WRBeJa0W46toi9FFXZIQAkk6dp/Hvvtx/kSw5Kz/7n2+vfe399" +
                "n3v/c//713Uh2vjmd/Wx23I1nZ2Ji/3+t2ZHsHTqmODzYIAZ+/" +
                "Q21oR3XcTKyON5YBek+lUh0vTq6Op9fBsaU6bufAcGf25Wyl90" +
                "8nVyq1xdbibeD/Kfs48MfDs8F6WaX1j7JZnc99dPuhgaclJvWB" +
                "sykS+SqV9AXy5Lz5HlfLlEql/u1KJbvXW4ppPCPPRlj2xexuZD" +
                "Ldpjt72T6CPAbHg/kEPwN9b3LYdOc7s+cQgdlpqDU+D4+TTHet" +
                "YbqTr1qkODn4zITYg47hvuyh/LPB/5x8WrYasIdbXCNeDqfZ7q" +
                "KSHc0eyCvZr/I2ypY9CsdTNIfZ4exb7QeSAxzLJzP7/QGbGOSk" +
                "7JsOfyxbxbyOwPEvXq8/6WqfYroHL84ez/8ALMBeTGuxz+C5Qv" +
                "wr2SPwuCd7Ij/JdKfzAOs1vZWKffQyexBndp7+pekt9hFCvqa3" +
                "+XZ4nGB6hxNEoY/OUvsZIraPQxdiZKXSNRuwh5EB+hgs2W5rzR" +
                "7w5yNlgz72Qh97ef7sW0O/lxyUGIu4X2Lg/02HP2Zz8wjoo9MH" +
                "57nap5jeoWPQx6q3Qh9L2FkVjwAD9NFanceoGc1/ah9BvgDH9/" +
                "MDfgb6i9nZZrQ+Kf9f0J/xGMSM0ngAZvn3nO8PCe3oyw96v/w7" +
                "+Y8aX8fI/EjN8Ojap0Oeo/l/5D/J/yt/Pn8u35//PD/UquAlOI" +
                "7xmPwHZnToT5OXnP5swP5TePzYPf43HP8Dxy/yX7r5yx19VL2V" +
                "+T6+Ivton8N8CerIR3Ya/vnOD+evtHxTk4JMUbrzIcV5csikQ6" +
                "d5nHxo2Fm2X+P5amSpVIpZjfvQF94fb+PR9beSRf7j/HHGoRuS" +
                "o7pu6UGRVEm+Sq809gevq3RWsvMc9l/xhmJmy7fNtIFs89Jlbc" +
                "OR7c1WgBwjhHx5lI9EzbR1fAytto8Y764z96KvG2OSodVFkY3P" +
                "vd/QXcerSVdF2SoVqkrxjtHaARvTWWVFHAt99L7DZhjkMErnMY" +
                "zz7DIzPHSmx8mHBlk43nEjsljZ/Af0hSvthTy6fhFZ1PkY8XNk" +
                "aH/6h1bPLj1eTRRJlXTcqFca+8P52KuzxuuTFQfbKrMK5CqU0L" +
                "sP+pnTP2RWDfWjnfuiNx1kq59OCPTxu+gP5+NczlB/kCyislU6" +
                "B0egI99L2yUW6ySxktqccl6q3fVxSGeV+TkrVmuPZG0yKX/a74" +
                "dwHcknk1OSl5PT4Xrdk8xqvhGQN8Ee8S0gb4LebgX9HaCvT87L" +
                "boH5/mSuizofjgvgeHf9CsuV/H6yCKx/0dxHHep6FvANcFyadC" +
                "eX1Y8k703eZ/F0Ge+j26dek1xbVMHTWZLrk48mK5KTkgl21tic" +
                "vj94tSVv8BHJG+E4M/tUss7uH5OzoSP/CnFzeO5kXvC9KKzzks" +
                "QkHekoz53fQn1wPldgTcWXW7Ut5f1KboDjw2aFWQH9XIHSdXgF" +
                "zrMBs6LxE0LIF73pIFv154QQn1vLjzhD4zyyiPNxhc7BEejjl9" +
                "LlEot1ntVLm7uMNx3la88f0lllfs6K1dqja0HXAsixAKVb7QKc" +
                "Z2nXguYSj5MPDbJwvH4/soQzYQHqtQt4dKODLPJfzM+Rjn3px3" +
                "Xd0oMiqZLa+XqlXk9H+drzR3XWeH2yYm8zhSmgnwVK1+EC59k9" +
                "pmj+kcfJhwZZOD74HLKEZ6xAvWsbj25cThZ1Rkb8HOn4brpB1y" +
                "09KJIqsbnlSr0O5yNbe/6UzhqvT1YcbD2mB2QPSmfpwXn2VdPT" +
                "vJkQ8kVvOsjWmEsI8bnzcTdnaGwji6isR+fgCFy5dqc3SyzWed" +
                "awln8s56XaXR//TWeV+TkrVus8RswI3F+POHnMjADTBD+D+d7q" +
                "82akcQTurwH3GNxfj9CwKNxfj5Dd+cCuCu6vHeoyBd98muWj0X" +
                "gSLe6u8Ch4uPtr4rIaZ4aaDpuR5kNpk+eE+2tVE2ATvZZP8nO4" +
                "LjxPEXB/PUJ1USbY70Ic3F+zrHB/PRJV8UrrunSSGYH76xHTZ/" +
                "qgj31OHjN9ro99Yb63482mr/nX0Mc+HNDHoAGzO6CPTuIj+MyE" +
                "2IMecX0M/vm09kXkZfoG16NF9pGyYQ4a2WE4H/89vYVj0EcR4b" +
                "CJQU7C2uxaWixHyJNqd30ELugjywp97NPD9tH7Qx/7oI99dt9T" +
                "bE4nFBsGHm1dyWHfU/ttu+/pOAP2PXvsvgcOu++BPWdyDhzvcH" +
                "6tfU+yxe57klv8viewhH0Pf8XW9lYqA0/hvqd6APc98T+37+kv" +
                "2/ckzeah9LbgFfY9oNndGex7Bh6HDG7f41C274G1zAuo2PfIvM" +
                "Wi9CRWxeW476F/A0/6fU/Lx+97dpqd0NudKN35sxPn2T+Znenf" +
                "eJx8aJCF4+EsDFp6J6G1C3l0Osb9xTtkxM+RjmfSO3Td0oMiqZ" +
                "LaQr3SUMUoX3v6BZ01Xp+sONiqBna79tFLZ6nivPqiqTYfJ4R8" +
                "0ZsOzeM14nN71xc5Q+PbZBGVVXUOjrjV7pZYrPOsXtauLuel2n" +
                "mFcp16eFas1h52f16rDM8vfpVM4/cTfr+e/TN+9pvMBr8p7j7l" +
                "nORc/DQZXgfH6FNnOpL5aHdn4cTWXusz/JPoxnf85+HRa/rK5G" +
                "prGV7gvQv1STT08RHwmp7ciEgyEyo8wzMnZ/nPw+U9ms0dfBeS" +
                "lRhbr+uuZAl/XXt7bULygYBclXws8PRQjuq4ud3cDv28HaXr8O" +
                "1+dM7unI0I96FBFo6H50loXrd8NJpt3F+ckRG/RNJ9um7pQZFU" +
                "iV9LzMv9oY/v01nj9cmKg22dWQdyHUpnWYfz7OuIcB8aZOF44B" +
                "ea1+un8OjmbO4vaov4JZJ+X9ctPSiSKsm+plca+0Mfb9JZ4/XJ" +
                "ioOtw8AVyz566SwdfmR7u+6WCPmiN3jtR1TyeI2i3fl4F2dopm" +
                "QRlXXoHBxxr+sDEot1ntVLu5YyXqrd9THRWWV+zorVOo9tZhvI" +
                "bSidZRvOu76ICPehQRaOB36heb0+i0c3M+4vOhnxSySbpuuWHh" +
                "RJlfi1xLzcH/o4orPG65MVB1vd1EHWUTpL3Y/O0zpPQ4T70CAL" +
                "xwO/0Lxu+Wg0t3N/UVvEL5HsZF239KBIqsSvJebl/tDHL+ms8f" +
                "pkxd7Gv//V3x/Dc3gXvxbL74rlFVrP+PWaffN8mH8fPDyh/Hod" +
                "59DX62yGrFvay7+/9muJeXmlcGU+VWeV64+v8eF6PcvMgn7OQu" +
                "k6PAvn2VmIcB/uzaPIB1lI83r905xhuJ/7i+dY5aAR+vhWXTe3" +
                "y+pwXnxCr5RHo2+xVmclu64Ca3W2qWYqyKleOnwqDqj3Z2RFTG" +
                "twnZnq4/QhGd35+DL62jF8MzGJykQ2nd/18Z3lNfG1EK+f106P" +
                "rRTROh9n6ayyIo5htc6+xqwBuQals6zBeTZHIuSL3nRoHq9RtN" +
                "sL38kZhneSRfRxjc7BEdfHuRKLdZ7VS5u7jJdqd338hs4q83NW" +
                "rNZ5LDVw120fvXSWpTivviAR8kVvOjSP1zwH6mY+Zxj+HFlEH5" +
                "fqHBxxfZwisVgniZVgHZqXanfvPG06q8zPWbFae6RfSf9eXhX4" +
                "O3TKPmNN+9wvn+a73zzdU3bnZWfpdVngT+6VVw33bF/Br0SMe5" +
                "m+zuBjem2Zf7aAX8Xk1cNeZ+QqvK12Ofpln5CcYu3vja+g+jpT" +
                "9vmUeea1jTiiHClDNebniPoo6RUzeSTrOlEFA2dzVpkl5iW769" +
                "gSnbWsR54Vq3UeM8wMkDO8dPgMHJVKPtvMyJYRQr48ykei5o/s" +
                "oGZ05+OV6GtHOiYZWs+tyMbn6JctL69JV0XZ4P1xfWx1VYwSLz" +
                "BfD7UfkOuUFRCG1Tr7FrMF5BaUzrIF5/lbEOE+NMjC8cAvNK/X" +
                "rtLR5CU6GfFLJPuIrluzYiRV0rVBrzT2h/fHHp01Xp+sONhWmp" +
                "UgV6J0lpU47xiUCPmiNx2ax2sU7fY9L2kG8mKVrdQ5OOLOml6J" +
                "xTrnprWU8XpZ/4z3rT+js8r8nBWrdR67zC6Qu1A6yy6c528zu9" +
                "zn4cKHhp3B/lHhgT9o6Z2E1nfxaHhdM3/RyV28Ip0R+rhW1y09" +
                "KJIqyc/RKw1VjPK1Z5/UWYlPrxBrdbaNZiPIjSidZaMfnd2d3Y" +
                "hwHxpk4XjgF5rXLZ+MJi9RW8QvkWxI161ZMZIq8WuJebm/2z+q" +
                "rPH6ZMXe5q/jg2+H3cKj8efh+bv4/WXxYf95ON+XFNeQP9978N" +
                "1E8WfIXH+suN5+z8XvbMvur8PvaK4LOT4S/WL8l8VSfQ/vD/89" +
                "l4u6kWopboC1/K5DPxo4r6IqiuuK5Zi3vW3wXED+XH4eXlztHq" +
                "8tltnvuUrvr3eYHdDPHShdh3f4ke3tegwR7kODLBwPz5PQvG75" +
                "ZDR5iec44pdI9hVdt2bFSKrEryXm5f7Qxxk6a7w+WXGwdZpOkJ" +
                "0onaXTj86ZnTNNZ/YDQsgXvenQPF4jPve6nqkZyItV1qlzcMS9" +
                "i/1QYrHOub20uct44f2RrX1wvs4q83NWrNYe2Xvib3zo1Zld4h" +
                "4vph09f9WHv37Yz+9A4H7mmvAd2cKS+5k15XcD8Wv6xPcz+ZvK" +
                "PtNC/Tj3M6s1d9n9zGB7+f1M/KmZrMrcam71j14iZkfnqZ2nIs" +
                "J9aJCF45yZ+Nz5eKqOJi9dleSXSH6GrluzYiRV4tcS83J/6GOX" +
                "zhqvT1YcbANmAOQASmcZwHnX44hwHxpk4XjgF5rXLZ+MJi9RW8" +
                "QvkfzNum7NipFUiV9LzMv9oY9X6Kzx+mTFwbbcwLXKPnrpLMtx" +
                "3rVHIuSL3mY57B8DKnm8RtGuj3s0A3mxypbrHBxxr+szJRbrnJ" +
                "vWUsZLtbsdxbk6q8zPWbFa57HZbAa5GaWzbMZ59ajZXD3qcfKh" +
                "QRaOB37Q2jcRn6vyxzqa/EUnI36OtG+ydZ2oJopENrgSb9Irjf" +
                "3hfLxbZ43XJysOtsVmMcjFKJ1lMc7z88zifA4h5IveZjGcjwGV" +
                "PFZr7yc+V+WZmgEtorLFOgdHoCP9+VyJxTrn9rK9v5yXancV7t" +
                "ZZZX7OitU6j/VmPcj1KJ1lPc6rv0CE+9CwM+ijwgO/0Lw+OFNH" +
                "k5fo5HpekY6prbd1nagmiqRKajfplcb+lcrQu3RWsusVYq32KL" +
                "sXoet7fv7/7/vC9g1y3zP4WzzD8e9nfs3fu27IL3jt3xe2b3g1" +
                "3xcOzZcre9XfF24326Gf21G6Dm/Heb4QEe5DgywcD88TaLV+4n" +
                "Md+KyOJn/xHEf8Esn/RNetWTES2dz5qFYa+0MfF+ms8fpkxcG2" +
                "2sD+1D566SyrcZ5fKBHyRW86NI/XKNr9lvMJzUBerLLVOgdH3J" +
                "3BsxKLdc7tpc1dxku1uz6u1Fllfs6K1TqPrWYryK0onWUrzvNF" +
                "iHAfGmTheOAXmtdr23U0eYlORvwSyZu6bs2KkVRJbZteaewPfb" +
                "xDZ43XJyv2thP/niK/6HX+PcUdr8f7I9Q1/NrfH2uff1Xvj1/4" +
                "DX9PscnAHs8+euk6vAnnHfMQ4T40yMLx8DwJzevtf6WjyUs8xx" +
                "G/RPL36Lo1K0ZSJX4tMS/3hz7erbPG65MVB9t0Mx3kdC8dPh0H" +
                "9Hk/WRHTmpU+Th+S0V2v56BvGUOrMpFN53fnwP7ymjQnZZNr4b" +
                "y8UujjvTqrrIhjWK2z9xv796z9KJ2lH+f5JYhwHxpk4XjgF5rX" +
                "BxfraPISnYz4JZI/rOvWrBhJleQX65XG/pVKo6KzxuuTFQfbZD" +
                "MZ5GQvHT4ZB7wW3k1WxLRmpY/jR3ZQM7r7wlvRt4yhVZnIpvO7" +
                "73xuK69Jc1I2uRbOi/bw+55vQO0H5DplBYRhtc6+1qwFuRals6" +
                "zFeXUEEe5DgywcD/xC8/pgv44mL9HJiF8ixVZdt2bFSKrEryXm" +
                "5f5wPs7TWeP1yYop9vif41YPSkx+jpu1l/0uJb8vRpHdPPH6fI" +
                "5bfO7VfI7L//cdXMuv+xy3cc1v+DnuMrPMP3qJmJ/nl0mEfNGb" +
                "Ds3jNYp22tOagbxkTZqJENfH3RKLdZHVr+XScl6q3fXx4zqrzM" +
                "9ZsVp7ZPYue8x9NzbNSzOGnTZjySH3P18AnsyG944psE8as38/" +
                "w86MYz4qfL/2ov3/pMBnPkfx72c8E8N/hz+zNq/1gOgrk6tbv6" +
                "C61v79DOuxq85VND35aavOmTA/I9Rwlj0fMRNlRC1ZyFGpQx+/" +
                "liwhJLnc2+Xfz4T8PSL6/wApPsDZ");
            
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
            final int rows = 59;
            final int cols = 82;
            final int compressedBytes = 4012;
            final int uncompressedBytes = 19353;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNqlW2uMXVUVHpRH28HHVCsIrS21FgpUfFFEHm7undsyU3nJQw" +
                "kwjE4nHWo1ZqApE2Pbfe+ZuQ+gtY8YbOkjLRVf8YeaaIxGamzk" +
                "JSAC8Y8m+KsG0AQjCCZT99nrrPnWWvt0LHBPzqy11/rWt76155" +
                "77ONO6FW5FV5db4Qrb1cWr3K/M1xFgGY3T8pCH6ty74iHLABQe" +
                "uptcM3bich1LfcmNWcp4oT3Hdh62XXV/ycpqI2KX2xXsLrYxs4" +
                "vXlXkckRgcyMh4wa888lsNWw2U2smEX0fqV1jdlpUroYRmSXkl" +
                "Puzji7ZrOp9WXOQylwWbsY2ZjNdZn8smdlEcGBzIyHjBr7yC+Q" +
                "lbDZTSlvDrSLPb6rasXAkl2ZV2UvLr++XsnX/brul8WnGRO+AO" +
                "BHuAbcwc4HVlgTuQjVIcGBz5qnHExgv+wmuei6h7UlY3z5F4pe" +
                "2AVGQ7Bs4lVrdGoBJK8ln0pOSHfRSzV06yXcFnJ2StMTfshoMd" +
                "Zhszw7zOrnHD8fk4jJj05Gl5yANfVPmUZQBKKBu2PWQkvj4+oG" +
                "OpL7nJ5r3LeCful7N3XrVddX/JymojYqPbGOxGtjGzkdeVDRyR" +
                "GBzIyHjBr7yC52lbDZTayYRfR5rO6rasXAklNEvKK/FhH1+zXd" +
                "P5tOIit96tD3Y925hZz+vsOo5IDA5kZLzgV17B85StBkppS/h1" +
                "pLnc6rasXAkl2bV20hTf1XXPEts1nU8rLnIb3IZgN7CNmQ28zm" +
                "5wGyrPUxwYHMjIeMGvPPIrz9tqoJS2hF9HiOfYmlAJJXmNnpR8" +
                "cMV9vN12TefTiovcDDcj2BlkY3wGH2Efb0KWY9bLLdXZUzNG7z" +
                "nGljFMKVPdbP/43tVXrslyoluY5cY0iwrufc9XbFetSMZYbcyv" +
                "cquCXcU2ZlbxunKejgDLaJyWhzxUR+9PlgEosY+rbA8Zifu4Us" +
                "dSX3WdmqWMF9rjPjZtV91fsrLaiOh3/cH2s42Zfl5nd+gIsIzG" +
                "aXnIQ3X0nrEMQIl97Lc9ZCR/1PfpWOqrrjTLSDkvtMd93Gq76v" +
                "6SldVGxA63I9gdbGNmB68rl3JEYnAgI+MFv/IKnmdtNVBqJxN+" +
                "HWleZXVbVq6EEpol5ZX4sI9/tl3T+bTiIjfhJoKdYBszE7yufI" +
                "YjEoMDGRkv+JVHfiuz1UApbQm/jjQPWd2WlSuhhGZJeSW+q+ve" +
                "ObZrOp9WXOS2u+3BbmcbM9t5XdnptsfvMwqDAxkZr+xklqLTdv" +
                "ZzPl0NlNKW8OtIdqfVbVm5Ekry3npS8sP3GTH7vUtt13Q+rZhy" +
                "vRf2XtjVlf8km2dolfuV3RyRGBzIyHhlN7NQJ8YSn64GSj5Sfh" +
                "1pPmV1W1auhBKaJeWVeMyqNaQ9pNb8dHvcnrCfe9jGHd7D68xz" +
                "RGJwICPjWZ1Zit/YHvZb47YaKPU7Tvh1pPm01W1ZuRJKsk120h" +
                "SPWbWGtIfUGnMt1wq2xTZmWrzOWhyRGBzIyHjBrzzyWxO2Giil" +
                "LeHXkeZfrG7LypVQkjXtpCkes2oNaQ+pNeZ6XW+wvWxjppfXlW" +
                "U6AiyjXW/jCEc1D3mojvu43TIAJZT12h4yEj8//lXHUl9yY5Yy" +
                "Xmjn7rqr7i9ZJd71ub5g+9jGTB+vs46OAMtonJaHPFTH18f7LA" +
                "NQYh/7bA8ZyR+NM3Qs9SU32bx3GS+0c3fdVfeXrBLv1rg1wa5h" +
                "GzNreF37u44Ay2icloc8VMf7XY9aBqDEPq6xPWQkfyw/RcdSX3" +
                "KTnXiknBfaubvuqvtLVol3Q24o2CG2MTPE6+w+NxQ/9wwhJj15" +
                "Wh7ywBf/PvNfydA8Bxm1j0O2h4zE63qJjqW+7FrM0iznDZ97xO" +
                "x6IssuVWi8G3NjwY6xjZkxXmdbOCIxOJCR8ew2Zik6jbE/8Tdb" +
                "DZTayYRfR1oXWN2WlSuhZOIFO2mKDxMP2K7pfFpxkeu4TrAdtj" +
                "HT4XX2LdeJ98MVBgcyMl47l1mKTh32a+faaqCUtoRfR5ozrG7L" +
                "ypVQkvfWk5Ifno9idkZpDWkPqTXm2q4dbJttzLR5nW3liMTgQE" +
                "bGa4uZpejUZr+22FYDpbQl/DrS+qPVbVm5Ekry3nrSFA+U1pD2" +
                "kFpjbqabGexMsjE+k4/wHrcIWTrqVWRhqc6emjG+Pj7A2DKGKW" +
                "UzZQ/dv7jf06tjqQ8drETOInmlUj0HOspuiEm8884H69nGjOd1" +
                "5WyOcDTso8eBDKqogqPgi/t4v60GSu1kwq8jYR+NbsvKlVBCs6" +
                "S8Eo9ZtYa0h9Qac4NuMNhBtjEzyOvsYR1xg2EfByUap+UhD9Vx" +
                "H3cDJfFmFwdtDxkpno+DGpVyAl/M8ptyXmjn7rqr7i9ZJd4NuI" +
                "FgB9jGzACvK4t1xA3UX0dWn5aHPFTHfdwDlMSbfRywPWQk7uMb" +
                "Opb6khuzlPFCO3fXXXV/ySrxtcnaZHh1nSQbX2kn+Qi/w98hyz" +
                "HpuRPIUh2ftfMZJxmjdz5jwbXpWmbgh+5m+8dPJ4fLNZG/aT4q" +
                "mY97p7xSKVByTq0AMVar9dcOYw72st/rbJ7xC4C0j8ZF9cHQZy" +
                "4j9P6E9dyu43jUDqNDfaAMkT2iUbk31fPwpvmlrHMb3y7YJ6fp" +
                "PVdO56+2u4OuehfcbDebfpLlGK0rKznC0fD6OFuiZRV4wAy+eF" +
                "1/ByiLt6pkDxxTr49Gt8xrdbymWVJeicccVmWZComXz1x7XYXf" +
                "+xPymk2va3316Od42XVdOdUy0HWdPCumva4rp7ae069HOl9+XW" +
                "ePH891reeQ13D66iLx6euj2scna5P1A29vH+sPgb31W/l6Uz/4" +
                "1vYxvH7c9Rb28Q/l+1jf///20fYo20e3xW0Jz8stbOMzdQuvaw" +
                "23Jd6nUBgc+apxxMaL53vhtf6DaOslWd16VeLVtbJFKrIdA89r" +
                "VrdGoBJK8ln0pOSH74VidtSlfHZCiXdrw3Ew/ow2xHgV1tlzMY" +
                "oIsDkynkXVWv5ZYA5SJMz88hT+ILASX+g5WCBEt6keoj9hdUxX" +
                "6JlYW/asrtBI6hS5dXat6SVYWUnEhkewjm3MOV7X3tARYBmNE7" +
                "naIkRQHa+BRZYBKPEbdraHjESeN3Qs9SV3Mcuicl5olwpTlVYF" +
                "pouIlW5lsCvZxsxKXmeTOgIso3EiVzsPEVRHledZBqDEPq60PW" +
                "Qkfu45qmOpL7nJ5r3LeKFdKkxVWhWYLiK6XXew3WRjvJuPwPpP" +
                "190+BRFgZRVVsodTM8Zr/B+MzY/2yZphah9VN7lmXOOFck1WFb" +
                "rRLDabH+H1sVv2t121IhmTeDfq8veRUbYxM8rr8Xe40fbpiADL" +
                "aDca3meKqOYhD3y5154lGdqnIaP2cdT2kJFiH0fLNGkNjC9mOa" +
                "GcN+yjmJ0zek57MA54N8fNCXYO25iZw+vaDo5IjETLKmCYBR75" +
                "7XmWASi1k6YHDsq3F1rdMq/V8ZpmSXklHnNYlWUqJN5tc9uC3c" +
                "Y2ZrbxevxEjkgMDmRkvOBXHvntS2w1UEpbwq8j7Q9b3ZaVK6Ek" +
                "a9pJUzxm1RrSHlJrzK12q4NdzTZmVvN6fJaOAMtonMjVliKC6v" +
                "gqvtQyACWUrbY9ZCT+PpbpWOpLbrJ57zJeaJcKU5VWBaaLiH1u" +
                "X7D72MbMPl6Pd3NEYnAgI+MFv/LI7/25rQZK7WTCryPtO6xuy8" +
                "qVUJL31pOmeMyqNaQ9pFaubV3ZWtGq1Q4Fb3m8y/GTqfsdPw3X" +
                "Q3w9zbNT93sOHetuyfgv64PZrxidPiZekqvWq/E9Y2/yrfAQ6s" +
                "vv97Re06jc41XtUPn9nokXOZ/9Wsbr+213fOMT93sOWW1lM+p7" +
                "aP4XHG18Pvzu478ea1wvv5Mf+75TfbCMe+rfZv2s67geqCnfx8" +
                "YL5p6f+j59jH18ufxeXrKPk9MrK8+7nW4n/STLsfyonlU9y+1s" +
                "76U4xziLCslBmK4uwqFH7hMfjvC5ZydQVpXm15Hwucfo1ghUYs" +
                "a8t56U/PC5R8yeW4ksm08rLnLr3Lpg17GNmXV0VC+uXuzWtU+h" +
                "OMc4iwrJQZig+mKKgi+8QsxIq4FS2hJ+HQn7aHRbVq5kNlKkJy" +
                "U/7KOYPbcSWTafVky56e4/Np6pVu39t6xn+vtmETM7qK6W3X8E" +
                "39u5/xhea3785u+bUe/juf/IyDdz/9HtduH1L/9JNu7wbjqql1" +
                "Uv4whH8wgOZFCVY4LqyygKvriPSTVQ6nec8OtI+1Gr27JyJbNR" +
                "bz1piie0RJbNpxUXuRE3EuwI25gZoaM6vzpfR/IYe4TGCZ4cQ7" +
                "iiU4FnPskAlFA2YnvISHw+PqZjqS+5uXs5L7Rz93KVVgXw8PX7" +
                "lJ+6/nvv1ln/g/i5Z9p3NP+x+E7/r7Jc792tkv8l7H9U2O+Xvy" +
                "e2HlLoH4Z9fL31oH4lkvfhy9+veZbizvz3jv2O3Hil5O9cJX8t" +
                "Ub/7rW4r/STLMVr3jrmtlf0UBwYHMjIumcEXZxmT1bVXJN6q0v" +
                "w6UtlvdWsEKqEk760nJR9c3Ft3TefTiotcj+sJtodsjPfwUV1Q" +
                "XeB6OpcjksfYQxVVskcYwknGcGVFPhnRDFPKemQPuWZceL/u0a" +
                "hyVeiWd0+zqJDdJVLq1SowXcw3XCPYBtuYafB6/HTX6PRRHBgc" +
                "yMh4wa888q94XVZ3+iVe7WTCryNhHxvTaUIllFTutJOmeELrru" +
                "l8WnGR2+vyz9l72cbMXjqqC6sL3d7xMyjOMc6iQnIQJvz2F1IU" +
                "fHksrQZKaUv4dWT8TKvbsnIls5EiPSn54fOjmD23Elk2n1ZMuf" +
                "x1c/yb9a+Nj/kZ+rtPfo7Pq03m/45Ufh7zC/zC9F9mTPf3azza" +
                "t+hPlMf6/Ohvjrq+IT8Byk9uzaDVz0IPPzt8Fz6Ns/T50b4XVO" +
                "4qsMv051r7vdD3y39PYWfwX/RrS/9+fbR2NNij5Uf9MZtNseWR" +
                "NFp/POzjrWkl/6Qqssdmokjnq9MpqD+KGPOFfRwt54UK7q67lu" +
                "2OVpufbrPbHJ6Xm9nGZ+pmXo9f5DZ3HqQ4MDiQkfHi+a488tsj" +
                "thooda0k/DoSXh83T6cJlVCSNe2kKZ7Qums6n1ZMOf9Of2J4tp" +
                "7kT+br2n/Un+rf7d/j3xf2+WX//s53/af8B/zp/kw/13+Irmv/" +
                "Eb84eGeH85zGER//Pbg/P5xL6fOj/2Q4L/SfLhgv85f7z8Z99L" +
                "4WI8v9lX5Fp9/3+fhXN6/u5fqb/M3+Nj/gB4P/pXB+2Q/5EX+H" +
                "P8G/g9+v/Uw/y3f7d/n3+p78uvZz/Gn+DP/B4M/bNN+HT5D+rH" +
                "AuiXwX+I+H5+PX/SeCf5Ff5sP3f39JOC/1zleCrfpefV37z4Xz" +
                "Kn+tv9pfE2PX+ev9Df5G/4VwXd/ib42x2wv8qnAOd/0PWTk4oA" +
                "==");
            
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
            final int rows = 59;
            final int cols = 82;
            final int compressedBytes = 4106;
            final int uncompressedBytes = 19353;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrFW2uQXEUVniAiQcjyFFg2EjHimkqQhwRlQ+jZOxMebh4kZv" +
                "NGQImPQtSUWBRIkp7Zu7M7s1WWYBEoEckmiE9K+KGiAsJuhfVR" +
                "Jqb4Y35YxR8rJZSxiEapJFt297nnnnO6+84uUOre6tvdX3/nnK" +
                "97uvvO3HtXbVabSyW1WWV5qYQ1W06VRIiLbLW5fhBR6QdKZG1L" +
                "ww/6HohFfzIaryO3OSGxsMx9Q96YGfdL2jG6jCrjc6+crzapTS" +
                "bfhLlr2YT1tCwR4iKbErVV/0kIWdsStci43jhu8mNwxI3jbyQW" +
                "lrlvyG3smF/SjtFlVBmfe+V8tUQtMfkSzF3LEqyniUSIi2xKvh" +
                "8okbWbj9/yPRCLjeMSPwZH3Di+JrGwzH1nfemN+yXtGF1GlfG5" +
                "V85XLdUyeQtz19LCeu8KRDiHDmrheOZflKBc3uVbE0uMZOBfIs" +
                "3Xfd2+V7QkJdCX0C/nU1+lhjAG1+rayqps8jLmrqWM9d6NEiEu" +
                "sin5fqBE1rYE/nxfwSiW/RgcQT8xTVID8qkvMb+AY99lj3zvXI" +
                "XkD/wsV78KUqmk30AsvZG3Dp5kz3oOsEqlgV8axjF/FAZ+BeyB" +
                "XyAy+A4slXd63OdMeiYYx1UYwbT+3MSY9BnNw5JF2iXO/9IbBn" +
                "4K7QPPxtrJnhC9DPLBE0LvYZThp0z6iVpt2la7cTyOLb39uc/V" +
                "eDbjuLo0xV8RozxamsafWi3th5/2GbXHJIu0h9Z+X4q0AV4rca" +
                "/ZOEa9Sz+1ntyqH5LBrspbrzZpF7bC2Yxjf0xHbRHTxBi1J6jc" +
                "mD20i7U8nuULvR71ywhDTwSxvju027JqH/W1h9Z57C6ubeh7ua" +
                "9RqZmsVX82jlHvYRSYj1WzetRK57maf4ZrsVR1a8u26znVyalm" +
                "VBGD/LW3lvbhfEyXSpYtYa06Cb0oiu1rS/uycVnpt6uV2Tiu5N" +
                "6p5EcYun7ouqFqdaI6OeSu5bUT83U4XirVb3VWE2hrxnFiypEo" +
                "YJS/I2bV40XWU0Wo3yZZtoS16sT2C6Oxx+PaaqPF2mEcOUpRC/" +
                "vYKN9pz5Cyz2qYWt35zlqi51B70V8Ro9zg67rYWtpH1nVlaDdn" +
                "cdXQj2JNkNO6bhebxpF5b8T6aOdptq7H5bpy6QjMXoOMA1Y9Yu" +
                "YjlsUMz20Mx/rC9uoRpvLxnDPpeOa8fUUwH8dtBPI6/HTGp4hH" +
                "PNY4Uzi+/cLY2sv7Ms7XqFXhR+fjmHPHuTaBuKiVUiVb1/pk8y" +
                "m5dW2xkkvlPZUSILW/A5Z+Qc/RF1VyViljlnIbw9mir6D29Mts" +
                "HHcjxx7pFyvMC1Pfr9dVmFfkEzf9kmGdoj+PiD6zUtLngmc921" +
                "eWxc76ohdSq1Nxhxf9Rj6OyNXZtxy9Vt8BWO1QifVbLVfLs111" +
                "eb6/Osym9KtqeXMulDEhl1tiXXLIY96Xx8hWLW/NRJvIt5AMxz" +
                "MegNRf8WNwhbE+mb7c5aPgsdb2u1iozlecRVyhsnWl8vUFmE3l" +
                "P6gVA1ugjAm53BLrkgOl1hnsyvsjslUrWh1oE9Gf4XjGA5DW6Z" +
                "IlFcb6BH2RKHicYhxXFCnjDIMtU9leqvI9FTCbqscAxTrnckus" +
                "Sw55zPeeY2TL84j+ZRQZeWjTmFs95sfgCmN9Mt8f3++jxdG5ji" +
                "JlnKGWJZMJ7qQvYQtgNqVboTWZrL6EmGVCGS2RQ8n6onb2He55" +
                "5GCMGKv6ko1AXpGHWO/z6TbJsiVURShaY2yunryG0XnNHx2Iyp" +
                "GsH0eToyY/irlrOYr1yrXJUbuuJYcOW6sf9PHMf1ZqdRHa+wK3" +
                "bnVyvlB/lCvyI/a+0LrA1y0ZZElKbGzZUyjXRnnfGxf5Ucmf30" +
                "PUapNaqpZm83NpPlMdZlP6dUCxzrncEuuSQx7zOfEi2fI8sp6W" +
                "UmTkoU3vi635fgyuMNYnG9tHi6NzHUXKOEMtrcyozMhm7B5sAc" +
                "ymXgWtlRnVPYhZJpTREjmUrC9qZ9eZXyMHY8RY1T02AnlFHnFb" +
                "l0uWLaEqQtE6G0fF1ZPXMDqv+aMDUTkCUVWfwt+XffkIO8ym9A" +
                "HV567XfZSQi5b1g8gnO/LijeMBslV95nrdF2NxazzjAUh+ve6T" +
                "iqQy3ifTl/t9FDxOcb3uK1LGGaovOZ4cN+v7OOZuxR/Hem8vIp" +
                "xDB7VwPNs3RAnKzS7fmlhizwn8SyT9pq/b94qWpAT6EvrlfOqr" +
                "1BDG4FrJVv6G0nfla6EiW/Vz7e7mZNaXEkN7d0zVR97K/Z7IvZ" +
                "tKu/s98fsU2JfqpP7Ym79XNdX9HnWWOgvOkCMG9d4l6iy3rgWH" +
                "s7kVccgz+XOf3XPcQ6uT831VPAYd2f54ga+bt0t1WLd9kT2Fcm" +
                "2U9533inuPqeB81aE6TN4BucM78DDrZ4fqaM4lhLjcCiyxREl6" +
                "dOv6UuTao/WA9JArE9F4HXn1V+KafFUUzfTlwbDVHmYcO3h8P6" +
                "pUxDHO11/RJ5r19059kr1Pka3GU/Us3aHNOKeP6LPtfNTvMekC" +
                "3aXfmzTtfQr9AX2xQT5oUrdJ85yV+S6iF9h1nTTNzGvqK7W7W9" +
                "16WF+jF+trLWZQd49YL9HX6+taO/QN+uPRvWGd3qRv1reY0q0m" +
                "3aY/pT+rP6dn6BOy+fiQnqlPMS2n6dP1GfY+hT5Hn6s79fm2df" +
                "uF2qxs/T6TPuS8fVhf5hSZq7y+Si+061r3mLRIK91bG9WJzvcv" +
                "w7rRJHPt0Ev1Cr1Mu9/S+iaTPqFX6zV6rd6gNwqtnzbp9tieRL" +
                "tN/Z5SyY6jvJ8mdyO5i9Ru8cektYvueDUWcNvWzvZ7lD3Xbo7t" +
                "WK1RuU9hBMBgf6Q6tDXmE4/7rI3K/Zju/8le+vfu6veKNdSjeu" +
                "CsegjBevqo6hnYQoipnUGtlIMdpoEzVU+yzffoPu1tyLVHa4f0" +
                "wDXxFh4f56PEwjLpwT5C7NCvWdc9FN+yOJPrlSqod1y/WqwWUx" +
                "mSGUfx3Qo4eEasfpAzot/BFqP3Zj+3bfOtbTH3FvLTXaQXNZFn" +
                "OmPJ5ulOroT75H0nO9lLtJXciPJFahGVIZm5/C++ri0nGQImst" +
                "Ui6cV9pkOQuJ1tSYasNdkUr2tgxGPAuia94JGrojOWIDrxuM/a" +
                "KO+7VUgqwxicG93Z+XVmq0nuapTuFpwurZOavc6Y8sUZ1i0YC5" +
                "yWGqQcza4zFrXXGb1tqvmo17lzttfq2/z2kSvhOuNa8+uMyc+n" +
                "/TG4zhhFeru9zjiM7STiOlMz15kaXGfo+Yy8zsQUJy8nL/cesm" +
                "fIzfesrGbr5buSlwfOBpw4dFgUEGwHDnqhGOBZWjcvwxb3PfkQ" +
                "MGzO/XPPqHHgHB4z1IQ80AKo7Qu3wHJtlCKBWt4a6x94RdXQFv" +
                "vtQNc+9ZT/DIuuX7UH5Xd8ec2M/yppruFPmKz32HMubk1XXO5/" +
                "ZGHs1wWW48+5sC9S4VS/nML22g7/O4pNyd5krxnPvZi7T2Qv1s" +
                "tbEOEcOmytftDHs89VlKDcXMut0+9zvlgle7kiP6IZx35ft2SQ" +
                "JSmBvoR+OZ/6KjWEMbhWm9gnOhHOx/SH1Un7nMsgE+z72IT8Ds" +
                "ae02KaKJiPd/OZMNJX8Nx1wkYomo/we8ZjTaBCeA8gNh/TH0j1" +
                "6LXdewCsjxNcm0BgPu5L9pnx3Ie5G+F9WE+fTPbZcSROMoCtZM" +
                "F9AMfUBgAlf2ZXO1laj9yELcF8DPxLpP6Kr1syyBK9gSLZUyib" +
                "/ZH3fQD6KDWEMbhWm9h8HIvMx2fy+TDG5uNYfD6yeTlWPxybj8" +
                "lGuTMVzMcxG6Ht/ni7xxpDhbYcn4/JRqk+vj82LjfaX4/MxzGu" +
                "TSAwH/cn+02M/Zi7iPuxnj6LSI42sJUsuA/gmFoDUPLnsMCaWG" +
                "K8A/8SGdns6/a9oiV6A0WypyE/093gPiP92y+1urYDyQF/XwDM" +
                "pvQFaE0OVCcASwYtE1HJz/NBwxgkz7nfQWuNFtwm3B8RxzMegI" +
                "x8RrJsyaZ6FcqkTETPuVKF1Gi1s/qBcH9EP8RAllpjj/wbwhpI" +
                "Zhz3mJm+kDC1JhkGJrJlDmfLSYbZN47MezJsW6bxHuka7o1ytv" +
                "quIsWoDFURzks2esy3+z3D+g7ayRfvAR4xVWqemgdnNY8QrKe/" +
                "V/OacwkhLrcCSyxRkh7dZ/gX5Npj5G7pgWviLTJ+dv8xqslXRd" +
                "EwdujXjOM8Ht+PKhVxTPJdeb09ck/rIZlx/KP5rjKXMH4mLuU+" +
                "6ntv9PCWka8VzMf17bxl47ies7BG+nhUyBtXx3y7+bieRyA/XA" +
                "E/on3sVt1wVt2EYD3dr7ob1xCiuqtvUCvlYIfJcoDHPZoZ8YZF" +
                "OSI9cE28hcfP1vViiYVl0oN9hNihXzOO3RQftZNPqYhjqJaV19" +
                "kj78c6SGYc/yQxfiacch/1vVcOy5aC+biunTc3k3eST9SEqgjn" +
                "JRs75ptjsveyl/woUmWwBfagGiQzjn+WGD8TTrmP+t4r/5AtBe" +
                "O4oJ033ydqQlWE85KNHfPNMdl72Ut+tFE13x5Ug2TG8a8S42fC" +
                "KfdR33tjq2wpGMf57bz5PlETqiKcl0zs+2K+OSZ7L3vJjzaqNt" +
                "iDapDMOP5NYvxMOOU+6ntvPKw2TGMcN7Tz5vtETaiKcF4ysR+K" +
                "+eaY7L3sJT+KVclfSPQLMD0kseLnXP4vxbj3ymtT/8+If88s/m" +
                "RePrESv0kn48+5Kq/FfId3+OLPufhRrEqttQfVIJlxPCwxfiac" +
                "cqrVD8e8Nx5R0/gPGmT7MeKKUROqIpyXzDjeHfPNsVz76+SLR8" +
                "OjjapL7EE1SGYc/y0xfiacch/1vTe3ypaCcbyknTffJ2pCVYTz" +
                "kvkMH4355pjsvewlP4pVee8B5M9nyhfn2Hnu+cz9gzPjz2f0A/" +
                "R8xuRXkO/BU+n5THObrrKWUwqezvRP9Xxm8N32+Yz+Ruz5jJ4d" +
                "fz4DfQmfzwy+iz+fgf9XCJ/P6FX+8xn9ydgzBLk/Yr2+EneT+i" +
                "p+ryu8cxvfU/y9rLGL3wcrum8WxvD3pvBeHW+H+2bBewAPhTtp" +
                "0Y7PGeF7ALHnCrFnK2wc74nt5W9jHJ/8P47jj9uPY/w6UzyO+D" +
                "4FtKlO1WnWeifmWIZ69VWJqE6LEFtaYYkj4APLzSHIpS/gsx2q" +
                "k8eQ8TPtr0osLFOOSnhfuF/wRRr8qDI+98r5qkt1mbwLcyxDfX" +
                "CWRHgttMISR1ykLiw3W5DHPLBxFNH8+D7H1+znqGTwtHhPuXZg" +
                "y6gyFvcq+eFTndr1+bOh307nHUvxVPKWdq2Ve0tv8s++JzX9v/" +
                "R39B7A248d/6vr6LVRvLenH5Hv7ZVn67Mdgu/tfdv+3/BU7+1l" +
                "nrP39vj7FM0R/t6eyd/ie3sGe9Pv7Zm+RN/bk+9T+Nfr6b63p2" +
                "apWWZezoLczdNZeJi1cCa1IuaXbA52fpIe3Zy4D7kxD/maFdH8" +
                "+D5HMqRPioaxQ79cqewHReTRCJP8dr8LB8+b7u/C2K/E8DdcY/" +
                "x/+buQGO56PfZf/V34H665Zag=");
            
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
            final int rows = 59;
            final int cols = 82;
            final int compressedBytes = 3065;
            final int uncompressedBytes = 19353;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrFXHuMHVUZ3zZ2LRW2S2HLVmraEgSBQq1GQMUyd+deKQhaqG" +
                "IrVLGWVqIR/4DEP9T03G3vTHr/kUTSTWkUmxQISnxEi+ID6YNt" +
                "NNAqxH98BJKipfVBqE0w4tZz5sw33+OcMzO3j92ZnNf3/b7XuX" +
                "POmcfZjZZGS/v6oqVRVprD1m27sxC5QJM1U1o5mbhGUxtZC1if" +
                "Bji4NWlfYjiC60RrYNvVSz3lcaBFag1pFB8tiZbocoktM/oSOH" +
                "U/XoVcoMna6GEraSk0cY2m1ngVsNwuIF0PXPsSwxFcJ1oD265e" +
                "6imPAy1Sa0jjeHOoGapfzQRN6us6zTa15nMFbVin+erbOl+oLt" +
                "L5JTn9XbQH1JV5+V5G/ZBapq43tS1bVauv8lC3q9VZeVfeXuvB" +
                "nKVmqQey2rk6zVFD6gJdztPpHRsXZPRF6mF1WVZbopZCLOoadX" +
                "VWfpDoilWTtG7S6WadbtHpozntVrVSfVx9Qn1SrVJ35LTP+L1v" +
                "7S9qE1DrrOFcy0GkV89EGLHl8ZAEK/eXWfChTM2k0YdMaftRHp" +
                "07ZZxV/eCjotWQntZuqMWPQa15gHMtB5G+w2BCiC0/CUnQsrW7" +
                "zIIPZWrQau1G/+kBsZR7H+KjdqwFRlMxruNHCtsHRQwZx4zrkj" +
                "gfCfMaj5VJoKQd1+X61Szi+xydLij3AGKx47pkVrmplLtKfdHr" +
                "1dZ4q81tCbTsnKfPrZ27LB0xeCKH0lEz1ZfV5klpLkG94vo5xf" +
                "VbagVJgp0nI3Xx4AX3zWeD9iD1nHEeLeaU7+i0w8fp60tuCFwv" +
                "j5Zcj0fLJMokOXrT5VWIurZ7P5LlXpvb4+02tyXQbLvzvXh75w" +
                "lLRwyeyKF01Ez1ZbEckdJcgnrF9XOK67fUCpKINbZ5pC4evOC+" +
                "+WzQHsx4Y/GYLsegzDhj0O7sBQrF4IkcSs/1F3WsbfmzlOYShW" +
                "+Ofk7hNl0ESiK28TMZqYtHFPfBtUH6cYx6rufhyzz3Pc/67zmq" +
                "7ntCx8g51E4vWqWW0GG0++97Gs/U0V2Ncv0X/OKOOi5mxM64mA" +
                "V21Ji/SjAjA9SOlKij3aCMlrIo/HoaT9bqx0qU63/m1bZ4m81t" +
                "CTTb7jwPFIrBEzmUjpqpPlPrTpPSXIJ6xfVziuu31AqSiG38UE" +
                "bq4hHFfXBt0B6knut+vtQzrg9Uj8DRw/VHaXchtRN6njn5ecNq" +
                "D4zrp2tdj5Uo1/+MenHz4minrdky2mlott05aHJDMW3EmDrklg" +
                "t8wIAW3XeLijcAOw3HYsAGoAzXIsACtWFx6KvNAYVaUTuPKeuh" +
                "H4Ml5CISUahdapA9htFr2qKmjtPktsyQi6A9shEoFIMncig9t8" +
                "dqtt69SEojivnq6OcUbtNFoCRibSyuXorHWLkPrg3qq0lRf9Sv" +
                "e7XfllkP98Opx/XfkAs0WTOllZOJa8z68QrA+jQUvzGzJu1LDE" +
                "dwnWhNX2lPuVyUKK7Hp6RVaQ1p4An3v3lJMXPuLdbXb+n797eQ" +
                "OXVvjfV0r5Hy4w0H7ZyJw2j3exn2qncL9Y64uGtMrghxwusMxU" +
                "h8N+qbhMPnpb7SflXOl6jT4MeeovbNEKekH/eE8d3lk9KPe7w9" +
                "9PNyvkSdBj+Ku+/k+hCnpB/Hw/jusknpx3FvD/2onC9Rp37gHV" +
                "rSOLXnQonvrp+MfgzcP/60ThSI6vG32xXvsrktgWbbyQhQKAZP" +
                "5FA6aqb6sn68V0ojXnrF9XOK67fUCpKIbfxCRuriEcV9cG1QX0" +
                "HWiWJfPmJ/HzclZ9O5lb/Mvk1zdN6kmgpec1LG9T4vtVnOP/1e" +
                "xvm1HS+OF+tr8mHKMZQqaYOxuJiNEqtvEvrROzbRdlwxdk+Xl3" +
                "HxJSx5LsSpI+3iu1+elH70ern5/l6i6Nnm0fiozW0JNNtuLgMK" +
                "xeCJHEqnmlFf1o+bpDSipFdcP6e4fkutIInYxi9lpC4eUdwH1w" +
                "b11aTWRP5NegJXMVO37WYMXKBSLk2yRTUCXvdjR2poTWxc4Vvz" +
                "uQ08KcL1yfI3LpCembKxmUtw6WK93swjk/FzL5Cn7lPZcx/bT3" +
                "G2GlCz1Xl6XL+mzs8oc3W6UM1Xu+1+CvVOs59CXWr3U6jsq5Na" +
                "bPdTqHfnWt6nrs1rxX6K7pNmP4V6Rn1YLVc36NqN6iPer5ur1R" +
                "r1abOfQn3W7KdQn1OfV/eoaWp6gTjLfHdV56hB3E+h3m72U5h+" +
                "VAvMfgqdyH6Kxi71HrufQr3f7qdQ16lIjfD9FBrF9lOoj2Uabt" +
                "Wp2E+h7mS+rtPp7mhupPvI5LaEum2nMziFtlwpqFFK9iQ/F+rd" +
                "p23p00DeUzBr0r7ESJ9lCZ40fuCPlPqOKDdO6QVGlyGGo2FdDk" +
                "MJddtujHMKbUFt9DDUQR715JaGod79tS25Bosn/cisSfsSI32W" +
                "JXhCY6F6qe8Wza1yW1QrxfN5Rs5O6azq+XH0cA/z4/M4Y032/J" +
                "j8sc78mPzJnR+lDd/8SJ6IUshtzRzp2zi3NdGOKT/4dJX2Rpeo" +
                "Kly7yVHU60YaeC5M6/jgs52tUqnkc5xdZ8S+vWKdSc8W68yzp7" +
                "jO/MHu25uSdWZ6rXVmurvOqPEa68xgNKjH96Ats/E+CKfux9nI" +
                "BZqsmdLKycQ1Zv14CLA+DcXcx6xJ+xLDEVwnWtP5CZeLEoXeE9" +
                "KqtIY08ET6X1zHLxTjWjxNq/3l73Oqvu51XzmzTzIwP3pHbC0N" +
                "FKVW1LccHYmO2NyWQLPt9LzoSOchpCAW0JikHltDfdm7/RelBk" +
                "Rxn6QmpGSrwSFOc+tUty2NbZ/e9g4aO49IaqdeSLy75qj7gdpe" +
                "6f7ecr3q5YpM/tq7TN33nBiD/3pMXqmnu5zfS/TtTxXj+vze4y" +
                "vza+QvU/cet57t9pqTs9l4LcsHbKI0Q4FawclwjQHfdwWJpTLF" +
                "/PjfsA8hmaDnA9RTGoNfmmLL+qOCP1DGb8y2Kf9N1gE1HerrM/" +
                "MjxyGy1i9F0CMv9y7jHaOHOAb8tzS/NNiW3DbbVdW+u9x2efRq" +
                "Bt0fDvOjOpAOC9zB9oD6Xfn+cEf3i7mH2n7yak2Z1RWjb6Z/f7" +
                "h6oWR+zMfNmdkf3s7+jiR63aacdk2+Er3evkfbJ2/bLA6RTNN1" +
                "gTsCgh55ySt5dZmMt0/+bjHta6lfIOeXBtuSK67HL5TbDkWfc/" +
                "9tU67rPqCm83w4RNa6syLo5B+9y9TBgP+W5pdOxurobn+lnF8e" +
                "Pb/vgX5sTaQXnsx9T/urwfuef03hfc8/6+huf+1U7nui4zbx+T" +
                "E6ns734RBZ69rK0e1RvV6/2ZtMMNoOx4D/luaXTo7X013OD0Vf" +
                "/l0hvbzX7wr8bZl8b9bcMHXvzaztqvdmPKr63xXiY/Gxvj6T2z" +
                "L7cnMM2ulioFAMnsih9Pz7D6vZevIfKY0o9u3I0c8p3KaLQEnE" +
                "Jm/ISF08xsp9cG1QX00q/z6TXinem71U9d5MvVz23iz539S9N0" +
                "verPPeTP69a93vM775E0dEepU7B+C4cGdt/xxMxvVtvewBL9s3" +
                "TmcNPlphnXHG9W18HgutHb4Zy507SnxfB3n7G8W389+4XMjlc6" +
                "Gryz2aX6rVi+tC8iGUqZm0+bdhabBdrrv9gJ8PVNQe0oO/U7vY" +
                "9dh+0HeF9PrWEY9anxVqvlPxjyBTBu57TpzK+x7f6sn4a1trbW" +
                "5LoNl2ejOnIBbQmKQeW9M6bgGqvibulRqAI32SmpAiMRxBfQC8" +
                "LY1tn170HTRzq9wW1crxhWcbIMdxna5wuZCX/qYBRDqt1vW4od" +
                "oCR5katELSyRvlvuUjcJuf72oP6WmthxyeZ3Ttuy4X8hJfnggh" +
                "0rfW6sf1rfW9oUwNWiHptJ9GEvT9+36+qz2Am3B36mjbK6vXa7" +
                "rOVK7Xg1O4Xg/WWa/Tme56Le/D687p6FN6e+gbvxtneB8AWTMH" +
                "eM+GnmdkvKEYZG/C+3DfL9AccP6fSJ27mIkqmrXcHGoOaRtDUG" +
                "YWh6CdrgIKxeCJHErP/WY1W08XSGlEsf529HMKt+kiUBKxyWIZ" +
                "qYvHWLkPrg3qa8ab35zvRJHRTErvsFxoAxapvMUxruZ0IfIs1o" +
                "ei0pDD6UeFLFN88gFJDVunFqpoueX/A5OIGlw=");
            
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
            final int rows = 59;
            final int cols = 82;
            final int compressedBytes = 2874;
            final int uncompressedBytes = 19353;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrVW2+MFVcVn6pFg2kxUASWVogWG6CmiNHYRto3+97IPyvin4" +
                "oJba1KwhZqaAuJfz7IzMTypl1M/VDbCm2aCibqR6ORAkW71sVP" +
                "dqUUEhK/mFRiTIgxbVLFrffOnTPnnPv/7S6b+F7uveee8zu/c8" +
                "59M/Nm5r3J9+bvSrRXvi+/VvQLkqR/d35drXm/aEtF+2eDWFH3" +
                "N2WTSVJezFfVs5tF+4hoaxrMx/NPJolE4Ku3tLF9Ot+Qrxfjxn" +
                "xzYn3ld+f31ON9df/1fGc+kl+Vv0NDXZO/r5EWijakou1fli9r" +
                "tCtp7Hxto721GT+Vd/LhxPvKt1h029nsG6LtSJLixuJDxYJCrs" +
                "I3xex6uY6AKT7AGco1Jmux1Ba/uMXQLBe1DCUz8ioWea2rbFpX" +
                "7OIGQ7N6wGzWgtT/arO2h9X22Orv09ZxS7FlyrXPE7UsGcSj/z" +
                "Un12d8fvuXWddxSTLLr+KuVvqyto7fngbrNrEyq2cl/y/ZtAdu" +
                "nipfPj4IOn00fUD2qjVbRIXWun8AZT/XYHojk0cHQ9GsVR3u2H" +
                "5uV2zC7uHJrxbtPe0ecGej/VP/CbGWPyK4V0SbyD840Kf5KsrD" +
                "8yJ9vuKzDs+TGeZzCX6+aOKYmZ+h+WsrsaDBfsK7jgvyTd7Mtu" +
                "W7A5/vKPRKkq/siGqgyyaLLrX7ueL1OiqEK3ocRbNOR+3HR7Tb" +
                "OdV3vSs2eod4SJaHW+kZ7dN4ZQpHl2Z7LJ5NktHrZuX4eMi6Eg" +
                "ujPsOF043e+aJq9Ps6XUGtvA9x2V6jS+IyCUfgKMzd7Q21uLiV" +
                "vvglZ3Wzu3jkdt3ZWnP9pv1kj/GtXtn5ubV7H7Gs49KYdcwmwx" +
                "E4SkowyyZVlsZZ1O3+3JraX0A78MgR2VEK7ont9pg9p1pofWLX" +
                "dPTgzOy5NKfY88fieNQx4cS09ul/qdZwnQJt/1kbDpGx3O06/m" +
                "BwnxgM5K90du/+mhju/kf99kGqL15qjykj2jFmozkvL3qP2xup" +
                "z+iTV/YbRsXSs7TX4qj95Rn8vnu8/XS6Wi4b/HNL9hsoZvS5K7" +
                "yOG9xZ9Yejah+fbg7qPLw8zK6vf91aF9fWZwpxFhM+D88/ZrlP" +
                "cVas45GoTO7yn4cLLnk9MDf/m34eXss3FBPWNR6KOg8fyjfZ7l" +
                "PkX9DPw/N7479n+tqn8/3x4tXIbdq+jkdnaI8577Xa13Gxn7O8" +
                "E1DFtO4CpI9Br6R6Hd/QrFl9PfMY/S62HR8pIs1sen8mIVx9PU" +
                "NQkLXMR+gzd32hHFyx0TuOhx8fM+3+TrrZP7fE30wxoz+9wsfH" +
                "ze6ssqh7VcVfZvB7Zm+7Pb6l5bnJnJvbY/FXiqA+oz+7wuu4yZ" +
                "ZlY1sUxbBoipEv6dcDoKv3yUvmObV+tQHrqGOt5+E/d+cQcz2k" +
                "YzBTWoPjPsWlGO70kt8ed63VbE+vt9LFwevzrcnwkWQWXvZ1nJ" +
                "3Yot6HoSfr+PckeeQQt0If5rLU8pO4TEIRDryXo6QEM5c3xNat" +
                "xfMxuZvsrhyzPdCT6+vvmlbo9f3a5LLcp/hF1Drucfm7UFKCmc" +
                "sbavFzF/9weBvsLp5sB/RFewXZ/7xphd67jjsc63gsah13uPxd" +
                "KCnBzOXd3+rPrVnHN+x2k93Fk1bQp+2vMtVyzdpD2Xe8pAjlo+" +
                "u9x/oqBkdRkHX93VPRiGZsP7crNnqHeNKD0Kft3a1qhWZdX7xF" +
                "7el6H5dCUEwadd8sPRjCFf/mKMhaxhL69b6c/Nyu2Ogd4snG2i" +
                "z/267jTdyqtj9EOvfrMcd5zwtR+/UYjxBGSQlm2Zjj/uN/zNws" +
                "V4dX2e3IjpIjr33Ql+0/Vap1phV6b40OxOjJqHXcF4pQzuEoKc" +
                "HM5V2+059bg3q33W6yh1cBv6+r241PdVr3KXqHBjkTq+5wVjt3" +
                "8PsU8bGnd58iexD67FRbSWpaoQ9zWWp5Mi6TcASOqrN+0O8NsU" +
                "PcdrvJbuLKa13X1+U80RYk/2cv+/GxvGaWrmfubyO2d+qqjaaV" +
                "yyEubZv4cVwmoQjlEEdJCWYub4jt5y6vt9tNdhsunaMaPz6mc8" +
                "rlNhwio85iCLr31OA+MRjIX+ns3hA7xO23D1I97Nfln6vPzuw2" +
                "33t6ZnjKGwffr2cqtneVr1atyfLDoK222HCIjOVu944Tg/vEYC" +
                "B/pbN7Q+wQt98erj57CPqy/adgtdW0Qh/mslzj3huzitlD4Qgc" +
                "JSWYubz79/hza7ahW+x2k53j0t3pbtWrEXRqXm3jGsQCGpvOoy" +
                "T0rmO/qDMgiuekM6FGx3AEzQHwapSxbbyYOzDzqDwWZaX47tvd" +
                "t/U6lE624cXKCnPAKpnObBiTmfrS0YZCVsCZWJ4DNp5Zc/9xsa" +
                "51R+fZ+nVNxMvdy2K8DGNtuQzz/v2goRh8o4XqG34mcZlzWTI1" +
                "+LmGx7Sxgidiy1/plZp4rJXnYMaguYJv56jq5Sj7TvMrM2oAo8" +
                "YO+xW6Y/wmjRwcjcw2T4yu42woyqZnKL+vab5c0ivQs7Bnacbi" +
                "enIk3Qs9/q5QrjWt0Hu/AxyI3h+ivmf2hiNwlJRg5vKG2CFuu9" +
                "1kd/Fkuyz3zR42rVwOcU3pympXyL/ay1FSgpnLu71vtitw32yX" +
                "ryJk57jObZ3bVC9H0MC8vwutoNMlOSo/vXFGkKmGM9CcqIXH1z" +
                "EcwTkxWpI8st20ogeNzqPq0VDH8c1sXWcdyqqJz/1brL51yoJ2" +
                "OdL7uMih+ylL7zj1db8UAmPYOWm2NCvsQZJj7zjNhHLS2tGPVw" +
                "m+HGvdcne2e8DrcF1Y3mpauRzi0o5Rv43ar3eGI3CUlGDm8obY" +
                "IW673WTnuO5r3ddUr0bQybe4vv4OaCgG32ihesqMfHUtf9S9Ea" +
                "Vnxfm5xsxbZwVPxMrYvFITj7XyHMwYNNfadq57ToznYKwt52Be" +
                "fQ80FINvtFB9w88kJfd+p3sjiuVm8HMNj2ki0BOxMjav1MRjrT" +
                "wHMwbNtbad754X43kYa8t59Rbb4wHQUAy+0UL1DT+TlNwb070R" +
                "xXIz+LmGxzQR6IlYGZtXauKxVp6DGYPmKpvtfz9KJ1v1ePML4a" +
                "T+bAX9JRDxHGMy906iDf5ztf9z1uPRJI2h8CYjfwoD5P3LbM9m" +
                "9E7qWhujmUdIpyLn7XPs2Qjex1XPsYvt8YfwHHs2Uj/Hrp6OG4" +
                "Hn2Bv8qvwgf45dcqnn2LV1PN14eJ9jz0ZEhOBz7DWqeY5dSAtF" +
                "G2r1tufYT6vn2IXV+xx7xv6PD8+xg7Zm30416jn27tnuWbFdno" +
                "Wx3lLPwrx6AjQUg2+0UH2zvTNJyb1Tujei2L5i8HMNj2ki0BOx" +
                "Mjav1MRjrTwHMwbNtbZNdCfEOAFjbZmAefUUaCgG32ih+oafSU" +
                "ruHdO9EcVyM/i5hsc0EeiJWBmbV2risVaegxmD5lrbznTPiPEM" +
                "jLXlDMyrMdBQDL7RQvUNP5OU3DuheyOK5Wbwcw2PaSLQE7EyNq" +
                "/UxGOtPAczBs21tl3oXhDrxe4fKF33gjg+jktJythgTmfKBiPV" +
                "asfHcbRRH8tKCn11GiPBG/l5DHtkiu2N61p3dBohpKORO2+qxq" +
                "9nOm+WHe1qrMYBsrwj5uoEeUUtUf/v4T4xGMhf6ezeEDvE7bdj" +
                "9Z7fcPYY14VdO2LKvxf+fjZ+ibdneWVjd1Z2Vqq+sxI1MM+e76" +
                "yUjep0SY7KT2+csa7lJcDaGGhO1MLj1+ccjpx0TowGsU1emimv" +
                "AyPSaKjj+FpeLd8t02rVRL5HVQMd7RGLo67V2XsvdiL+gwRoO5" +
                "vOCTlBVqinkoxt46Y6Xj2vkr7dWaXzVeP7dTq/3GjDITJqLyPo" +
                "3suD+8RgIH+ls3tD7BC33+6s/n8huOHl");
            
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
            final int rows = 59;
            final int cols = 82;
            final int compressedBytes = 3020;
            final int uncompressedBytes = 19353;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrdW3uMHVUZv2hLxBet3VrX7bZrbbFgmy4kPrCis3fura22xR" +
                "IJ1i2IFIomKinWUv8Ae+6sdbgmhhiNLXQLWxMftTUoJpXEB4rv" +
                "RwRjA//KH2aTLTQxmxCMNp4533zzPc6ZubNlrwbvZM7jd37f7/" +
                "uduffsnbmz0z7fPt9otM+3XQ0v6GX91ilAOYdG+a57wON62OZI" +
                "Vh78YMN76Ry0cYbvCcYPrtTOaC6+LmmRQ86Q85cuOD97RSuyDU" +
                "eyFvTSOyTGS8Kp1qivLkfCL2RXxZAmekJXhPNWo3FoV0ibY3L2" +
                "cpZ8C7ky+8wCWy40F5tXFNirzWvNpWaJPc5TZqA9ZZHX233ILE" +
                "+223rErDJrzGW29Ra7r7X7FS5qnd3X2300V3mbeWfeusa8x7w3" +
                "b7dduclsNu+z9RbzgdCRNDvNjeYmc7Ntfczut5jd5uPmE+Yi87" +
                "L8MzBlLjGvtCOvMYvMYlu/ziw1y8wbzSB8Hs1Ki73J7pc7tQ3m" +
                "SvuZ2Guusu13mLebq2290e7vNpFp2jo2rUYj2Zbnfr/dt9rd9s" +
                "12c63Ddtj9Q+Z6c4P5sBk3u4TXW+1+W35sR7KteEdGYLdr4aTE" +
                "eEk41Rr11eVIyedxpEpNa6IndEU4b+FctDbH5OzlLPkWnONQNA" +
                "Ql1NjOtnggHoiGJu4khI/7UdjiCOaAdqbHEa4gPYUdIS/9TNiT" +
                "1EQ+1JDb1+0clx50Vpmfq3J+NBwN23oYa2xD/95/SIT3/Chscc" +
                "RlGsZ2+9tQhxTYcRTZdH7N0Z51jU4gt6/LvQNbZpW5uKrk+6/O" +
                "37GV7KhegWObeq1RyUg/2+jrC7KFXaX7+pk5Go1GoYxGCcF+9+" +
                "U0iphuZTXE6V0qurnchdyQAvfER2R+zZEMqUnZbO79/ihF8Owy" +
                "q85GmOQ3Gs1zgEDdPIefx+a55IZGY2IAxzm3F+KjGSbR9IAfpT" +
                "nhbBNLqxwcXKnnpFm83Tle7T08E92Kj8RHoIQaMeh3FyLCObTR" +
                "CMdJmetxVGvhGLrz9SXi+9aqGEnc9G49U59Pc5Ue/Bz04rHyFX" +
                "8DW903lI3QK5nuzcHXob1V+aoi6+lXMdJ7+vn3MX4gfgBKqBGD" +
                "fvdyRDiHNhrhOClzPTeXjo6WEdyV1JeI71urYiRx0316pj6f5i" +
                "o9+Dn4EXRjk/GkrSexdiOT2O+uR4RzaKMRjuf6RZtazSd0tIwo" +
                "vHn6EpE5fQZFEjfLLWfq82mu0oOfgx3HSe5cX3nn63qDzf9UaK" +
                "T8VcXhWnNTraPDv2fqx1Rr1V7XD8UPQQk1YtBv3osI59BGIxwn" +
                "Za7HUa2FY9yV1JeI71urYiRxYS6+LufTXKUHPwc/ghTbWVKcBS" +
                "y3V93F2XJ3o/r94ETv7xkzWvFZPRn8VeLkXN/9Q7NVo50r6uee" +
                "t++ZB+MHoYQaMeh3r0aEc2jLesm0xkmZ67nvmcM6WkZwV+RIx4" +
                "R8a1WMJG76dT1Tn09zlR78HPwIcucl14UfUdddt1f3A1dqt3NO" +
                "+5vz8+4n42XZyl3NV+7KlSDWNTuOH53fPO2H50fngtb1w31d18" +
                "fiY81zWQl1dqUDPdf/Wnwsuy7McOLQlqGA4DhwSAVzgLKMTu8n" +
                "HYjA60Kuz5XR48RSntP3hDzwgtllBLY7xykTuOWjofmBKl4X8l" +
                "i1am4tWrepdbO1uh9YaVs5p32mz7/3bC131d/cdNemtUrfQ7Lf" +
                "M5vL7mFRmUz3us/Fsj0NeXrd55J3mPR9rJB7zJypw30ufWbafl" +
                "qjIcXe57atVaH7XK3Rlj1PyUqoHXMU+90diHAObTTC8TyfaEG7" +
                "/ZSOJpbw6ulLROb0GRRJ3Cy3nKnPp7lKD34O7tWNrW6tzvurix" +
                "GHZXt3J6DYl1y/JzmkWPxWMIkczBFi8WgscQuzZPbQnGzuoxot" +
                "z84zlDnjDIuNtLJ7QCNYu5GRov8kIpxDW9ZLpjWe64uWbEutgP" +
                "sR7kjHkHqZJ4okbjqlZ+rzaa7Sg5+De6VYt+YOYJl8svie+ZQ/" +
                "imXlX5USRvqDWn+zD/TOIFlZC3tl0en3q71Ve/fVJS8+HZ+GEm" +
                "rEoN86gwjn0EYjHCdlrsdRqRU4G/P0JeL71qoYSVyYi6/L+TRX" +
                "6cHPwb1SrJrFo8Vn9i9lIxVnpI/Ojd+Hc+Jg1vR4/11Fz8Murw" +
                "uj55M7Qzxi1tUuzlovmXtMHQ76Bywcjbl7aVePl80+PhufhRJq" +
                "xNy2yG45wjm00QjHuTLpudYiHU0s7UrqS8T3rVUxknEX6Zn6fJ" +
                "qr9ODn4F4pNr8a2Ov9TnGXul7Y++I+981X/TfWddhlf3NHy6Jl" +
                "UEKN7WyLt8RbJMJ7fhS2OII5oA16PJLipKewI+KFPUlN5EPN58" +
                "J1uXdgy6wyF1fl/GgwGrT1INbYhv7YuyTCe34UtjjiMg3yNke4" +
                "AjuOIpvOrznas67RCZ8L1+Xe0WFontqF5ruVsBh2ua7HFid3qx" +
                "XjeMSstcoYO/3h3GPqcNA/YOHoLx6up109Xjb76Gx0FkqoEYP+" +
                "2EaJEBfZtGsdaNljdwJRUJIKNCI9aSVCNEcyuAfk01xCuuSdO/" +
                "RdahdqRjPRjK1nsHYjM9hPT0mEuMimXetAi6KxLRVohB3HGZ2D" +
                "I5ojGdwD8vO5nAzrknfu0HepXWh+4PfHLxT3ucYbL8FX+L5rkt" +
                "S6Y5HOn49kQdH60jyfkfzrf3cc05/19bznueg5KKFGDPrNKyVC" +
                "XGTTrnWgRdHYlgo0Ij1pJUI0RzK4B+TTXEK65J079F1qF5Ifz8" +
                "az9txqFmt3ljeL/fQRRDiHNhrheH6eL1qyLbUC1zOevkRkzpAq" +
                "RhJ3YqOeqc+nuUoPfg7ulWKjF2BX19cvJIfVJ8XxiFnrE8/YY/" +
                "+ce0wdDvoHLBydPlZPu3q81+zb+9v7sxLa2EpPhzg0TnWox+PC" +
                "/HI3nBuKkYqYgcfxflZPbOJxXJO0eJyOhxySW3W/SzwJdcTn+E" +
                "8/Vd0V0veT6v1PFL/HFY7Rd7i4K/ie0c9zwVzk/bPQHbnQk2r+" +
                "M2Nlr+w5pLH86S1zj90vddf28pml5cnn4DkkW16WY2sFY31Q23" +
                "sOqdfL7HTlzXnvlgDDPYfkWsVzSLZ212cwD/0cEswlew7J1ew/" +
                "l+A5pKInnkNyyG/kc0jBd353ezeUUCMG/fRHEiEusmnXOtCiaG" +
                "xLBRqRnrQSIZojGdwD8qGe2BzWJe/coe9Su9B8fc+af6Kbn///" +
                "WdcwlwtZ1/o5zV7r2pZH1bpep9b1tS+FdQ3H0VvX6y50XevnC4" +
                "P3ZwdaA1BCjRj0u7sQ4Rzasl4yrXGuTHrufblYRxNLuyJHOibk" +
                "W6tiJHGz3HKmPt+yFuqsMpfvOB8bbnnPdAGW7d1Pwyj2kUuo7E" +
                "mOr9xcQGPADbGQ0b2DtHELOeXZfZRya7Q8O8/QC8PMndWdN3eW" +
                "dPIVnP3fXlLcWet+ufca7AwF0Q0eMmK/Sf89P1eznWWVo8H/20" +
                "t/XsL2jlTnrXN0c1XJX6fi/5pbTfU7w57qfuCXiT2c07yoz3dm" +
                "9pS76nfuwPH9avF7j/of1rFt1f38220tZ3BOe22fj+O2eq4qzh" +
                "Qu0GH8bPwsr7EN/fRxHEWUj/Jd97gi8qUS1wy54jlok761Jz4X" +
                "7iyrJ66XETKa5+a7nr/vmPIk3ytb193D3mf1r73fm4kb8e9j8q" +
                "3kVBF5xr4vv5unX0i/U7mengyhh/5WR3nipvp/H5MTyXfVp/mZ" +
                "bKMe7PY4HpEYLzEOnvvAvivXaC1U5yMVa+sZrkZ12DHPQP4kw+" +
                "FrQtocI4d8Jy3cyl2V/R7evV/9/dmePCL7weOwgjM4p72iz38f" +
                "t9dzVfEevmiHY1/BElruOB5Vo9dRm/plWsDgHMKrndThcRa6zn" +
                "JZ/LoqT9XaZbkpupfO2H1YQssdxyk1Ok5t6pdpAYNzCK+cy311" +
                "eJyFrrNcFh+v8lStXZabouvpJL/g3zPNa7zxJ2qeO8H3zI+Tx/" +
                "n3jMN+Hcj6U1c+5uH2eyn5s8J+VfU9k/xEf88kv3TlH/RcksB9" +
                "r+SPdc/Dk9/a/ffJnwrgP4etwLY=");
            
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
            final int rows = 59;
            final int cols = 82;
            final int compressedBytes = 2520;
            final int uncompressedBytes = 19353;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrNW2uPJFUZbhVdJCqKEGJcoxH4IiaaMQsJ+8Gq7mlCxpWdhY" +
                "bZBbwlRk3koxsTvk1XCeNOor/BJUEl4ZKNJstHE5XEb8Ky3JPl" +
                "koVkFzEao8tisN96+q33Uuecqu7pxqlKnTrvc573eZ9TU7fume" +
                "n1aMn+gw3L6q96U5R7vIAnzC4Ls71Wl5zYQlqaw/6BhbNlVjup" +
                "nZ59/klsWDaPMlo8G+IJs8vC7HExe05sKZ6zHPYPLJxdnOqmnR" +
                "5PzX51lO+ZMPZIPM3ZQz2OgNAmzE7HZI+uFPcQy+miy/6BhbNl" +
                "Vt11Q6Px8f4FtLSnFjEjxRnglcoR6SPWkeixRv9CfoQ4Wi/lAp" +
                "kpHljFK1qNnbMf7UqUNKL1x8fD3v3R8bUsXl3zL6ClPbWIGSle" +
                "A14dt3XpI9aR6LFG9kK+ThytF73z1NVTPKnvfdIGP9qVKGkkpe" +
                "+rczVfy+K0DPahpT21iBnBOHr5Qekj1pHoSWZ+kDhWL7xI9RRP" +
                "6nuftMGPdiVKGknp++pyDGwtiyv+AW7RC2H57YJyLJHXon1+O3" +
                "IsnloGB2hNM5gnGUDhR1cMeWqrHjsuvqrlja8dXzP+9PjL02iv" +
                "PK8nV//+3sKW8RcWqHV1cvRLwTtrZC7jzy3IU+I4bv2lQ/5ng+" +
                "hXdttxLH/U9TiOr5/RzYo5f9e5HaxbrDjLvXwkKMcSeS3a56N8" +
                "1MSTV9Y6rWkG8yQDKPzko3hWm4dmbTkuvmrjjP8bWtpTi5gRjK" +
                "OX3yV9xDoSPcnM7yKO1Yu80dTVUzyp733SBj/alShpJKXvq8sx" +
                "sLUsPnlyn8bKETYsx34zz1XX/3HwjeL04q7rtFZ4NOxqkb6yF9" +
                "HSnlrEjBTngFfn32HpI9aR6LFG9mJ+mDhaL+UCmSme1Pc+aYMf" +
                "7UqUNJLS99W5mq9l8fRz5thv53nO9O9b9nOmOD/H8/q+ZT5nhm" +
                "eGZ9BiTzgi6h/7AyOaI6uMaDx7nVVQh7la29b1S1PfIk3fXpUz" +
                "hZu97mfa5MtcrYdmDe2Vc7OX0NKeWsSMYBy9/JD0EeuovhJeks" +
                "z8EHGsXuSKqquneFLf+6QNfrQrUdJISt9Xl2Nga1m8OuPPo6U9" +
                "tf3zjPMIc/I7pY9YR6InmfmdxLF6kadTXT3FA6v6nsL5pA1+tC" +
                "tR0khK31eXY2BrWbzqv4eW9tQiZgTj6OV3SB+xjkRPMvM7iGP1" +
                "IjOoq6d4Ut/7pA1+tCtR0khK31eXY2BrWbx6o7wZLe2pRcwIxt" +
                "HLN4TPsURasfgn9vkGcnSlyBtwXV0cxFjanThnP9qVKGkkpT/x" +
                "/g9fUWdzvsWra/wNtLSnFjEjxb+AC5f7OtsirOGZTa7VQWaKJ/" +
                "W9T+3QRx5J6fvqXo/z/QyT7z1/mue9J3tUv/cc+/PueO8hV8v9" +
                "fF1Uv7kZrBXvUlt+cLA2xZ9Cj1rGOI5FFvNjIW59bC72emWveG" +
                "+wRmuU9W9WERZ6tmb5oem3E5eEPBXvRK/rtbBnW6u4UNf5gLqT" +
                "nkNLe2oRM4JxwaSvsy2iMy2eeM7U1VM8qe99aoc+8khK31f3ep" +
                "wfmmF1pG/jFj3BbM/2fWQxPxbiNnNpTTOYNz3nPqxdx3LbxqV6" +
                "7LjY8abO4Ea0tKcWMSMYF0z6OtsiOlN9A3hpk2t1kCkOYiztTp" +
                "zb2Xh38RmEvfuj42s1ZzjYj5b21A72M84jzLF9nW0RnZnm2jFk" +
                "ioMYS7sT53Y2vmJ8BmHv/uj4Ws0ZZs+jpT21iBkpPwpcuNzX2R" +
                "ZhDc9scq0OMlM8qe99aoc+8khK31f3epzvZ5idxMoRtuk7y5Md" +
                "vnU72UBe7cbbwTd9J2cfDbtapK/sLFraU4uYkfIy4MLlvs62CG" +
                "t4ZpNrdZCZ4kl971M79JFHUvq+utfjfD/D5Hv43+f6/vGnu/H3" +
                "XORqOe/hdYW30NKeWsSMYFww6etsi+hMi6ddIDPFk/rep3boI4" +
                "+k9H11r8f5fobZE1g5wjY9Hzv8hZ6wa+Thbrwd3ImemH007Gqx" +
                "vqon+Ijbwchitmf7PrKYHxuM2n2UHx+M0jx2at3FarZ58iw/Xn" +
                "7MZsd0UvfH7Y/M9T3FY0He//n+GHa1uL+nyJ7EyhG26XH8fJf8" +
                "xj3maAi1yjv1PPto/2hX9rwuBxla2lOLmBGMM1Z+Svo621wJV+" +
                "jMNNeOobo4iLG0O3FuZ+MraiSl76t7Pc4Pz3CC3MotejHM9n1k" +
                "MT8W4jZzaU0zmCcZ8ZptnprVY8fFV22c8W+jpT21iBkprwQuXO" +
                "7rbIuwhmc2uVYHmSkeWOVVWo2d29l4d7EZuL/HbVT3epwfmmH2" +
                "Drap8k8Y3b5k0p7QvOzEZDPfghZvNu4vJ7z2zPe/lpzhu5bD/o" +
                "GFs9lVm3Z63M/eLvkN2LDU/69ww/DBiecHPU+Y4WXrlNeufjqz" +
                "/L9CSwXypDnsH1g4e+vpbtrp8fbZ57/gFr3qef0JO9rsh85Hz2" +
                "jHPasLT7O067baae1YbcmO6STfH6+d6/3xkd34+ZpcLefzNf2e" +
                "q7wueRyvm1wP5n+Rtk53OI4P7crj+NAyj2NoWa2f6NufmRw586" +
                "3m1nOeHXjOPN9b8rJ66+yjy3c1qXERm33OZBe3vxriCTOi9prP" +
                "qX5ql8/mJ3kGHLAc9g8snM2uWt1fTI+25ee/5Ba96ny8yY42+y" +
                "mt7rhndeFplnbdVjutHast2W06m5fWn+vq6317GGR+seUn+uvo" +
                "dzlf63Y+bh5Jj2/fPOFcpvhXTLaru7nabHlv2VxLjh7evDd4L7" +
                "ll9Ra02DM2jQ8xojmyUlS86fHscVEWPd23WmFX4sjnhHx7Vc4U" +
                "bva4n2mTL3O1Hpo1tNdqbGN1Y7Lf4H01ssHx9kFGNEdWGdH4VN" +
                "/00N962WcLy3hr6FvE1mwyJFO4D1zjZ9rky1yth2YN7VVy833Y" +
                "pnfxs4yWX598gvy9uktUvHzfLM+w2djdcsiT5rB/YOFsnocf1f" +
                "Nrrx2b/XBluIJ2uCIIx9u3DVf6vxNEuDoLmdyTzSpyXyNWQXvS" +
                "I7Z+NfeIJ68p1ZDjR2mFlq5uq1pHGrP8wL1U/r/wZ403jlPtZx" +
                "NlBd7Dn1nce1qZJd+K/hpz1fG9+vrFuBzXv5MtB+3s0Ofr6BH+" +
                "4yL8zafSLWvrv4v7aavz8f65HN/f24XLcl319/b3osWesWl8o0" +
                "WEy2zZvA56ks19qyAj1pNXEsRzLEN7YL7MJaQr3rXDpkvvwvMD" +
                "d5/6P7TLby7uui7pu/+7348zr/xG8DcGnWr//PK574ap/78+N8" +
                "/3ZhW6+/7/+oH3+/ue/nHd6x+f+V5xvNvI7Mo7yYzlzO/C/JR+" +
                "WN5dfqc0n2fL79Xn49tzaX4riH5/sv1gQVfuPcnR785yPjrWt+" +
                "e09D+UUs01");
            
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
            final int rows = 59;
            final int cols = 82;
            final int compressedBytes = 2001;
            final int uncompressedBytes = 19353;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrdW2mMFEUUbhEUgiKweGtwBQxyrbLwY03U7DC90WAIkQgaD/" +
                "BC5bf+MZosM1Xi2GOUiPqPH8aQgNcv/6kQPAIi8Yh4C+oqrEaN" +
                "ityX3V1T/d6rqu7p6a3pRnrSVdWvvve+V2+qqqtrehxHP9gKWf" +
                "IWaXXjnKYHG+848/pNNWZpPkc67gBlw8vqZsdZ+Yi8Kk1R6yuf" +
                "p7NT6QqtvVV9N5LsaDB8YGB9J0w3afKt/vmxInvfP9fH+v+2z/" +
                "QpkbwXptvUtlQ3GrQ/Cj2dniJOW/zzw+r2JExpuV4a+lG0LZv8" +
                "KfvSwxH3VOekPCpPZIhj7m2Bcc0es9j2arFxZI+2O26ldSIN8i" +
                "AV11giMSKXZaxNJdIGRYNlkyawqzgTClvTPVSvVAm2X3nJ7Lsa" +
                "HZWLyo3f9nBZql3pnDIH25g7YzQCvYeGbs1bAeXatOLiuOpgm+" +
                "fsyZVJlY7KjMbVJXh+9B5PoX9x/LqHSC7z4zjDks/nJ9Yav61a" +
                "zGqmcqkmmd6iN7P9Nee+efvC9Wcjl2Vx7T0ra6UU1+KTYqhFkN" +
                "ZmApuKJ6thhQM+GKH7hNuCPQvyVQepBtXG3PhU26Z7DDzB4S52" +
                "F0NZnH67Z2ENgZEpYDHC9H0FGhhvRul2zByqv9InsAypLAU524" +
                "Y9wTZx20FPbaXQpVjT0btGpqIUjuu1tFYv+6v6wThb6eUqKg0O" +
                "o7DXzbiTbcdxg7bZTnlpealIRS5l4tp7WUowBj7BVXVQlWPLYA" +
                "+XqS3da4zQdUx+q1alJmCf3KS2VMdDW6kPOgf2FXSVMXM8unM/" +
                "fzKuYHr/Tq7vn2i8z1xV3POMtz7FE/tg7nHcm5+WnXVP6Sa76x" +
                "5rPmdY98S1ha572Nps657k/uiyU+d5pr1tcQfcAZGKXMrEdW22" +
                "lGAMfKAGy7FlsIfL1JbZK2qfSnS/VatSE7Bsu9pSHQ9tpT7oHN" +
                "hXcSaN69rcU2dc1+a083kmiGP8uK5dk7mlXcYn9zcs7QCsS6x9" +
                "xRjHnlTz44bW49j8PuPOP3X6Y1xbII7szez9sdRZ6hSpyMM7W6" +
                "e89jZQCWAlGk7VjiiBdtiWDtUCoNCdtVPlwBIVQxHYB4kXecBt" +
                "sgu+S8uUlXJhqxRvGBebo/Xjq3bvauXDxd2v28vt9rv9IhW5lI" +
                "lr7zUqAaxEw6naESXQDkujVQuAoj6plkCiYigC+xCxiuvRZrvg" +
                "u7RMWSkXtorx7Gu//+1p9EP/eYt9x/6R8yP713s9zI/551eNuU" +
                "D73ZV9G6Y7iWyvmB/ZF2xXNIvsYIdd47zGjrAv2QD7mf3OdrNB" +
                "9huLdqHYfv88SrDfh89Q4XMW+7Eh+4Egfgp+d2W/+Ocf/vkn+y" +
                "uUHlS52Tex813K+ZEdYIdkuc//iFTkTnQVlL0tUoIxfQjRp9jo" +
                "a9hwlJIouxNUbUDhQ7ffZ/Ax3ifQBGzA7RjsOsSuo7E6Rg7sq2" +
                "NsA3m+3qp9Vy29B+A4fAL0R78t59iZjaqJz/30PYBoJkjNnbY/" +
                "8rF8fJp9iupn3icW1yp+HMsHioujLe5M+z07h94fvV0ojicKjO" +
                "OJ9vbHxN+5Buyuw8uWfrPLsg6P47b1XBjtz62WqSiFcdxNa/Vy" +
                "zF7f6tbkKkrgvD1pUKrXzbiTfcBWzdpmO7ybd/K5XrQXyy/n02" +
                "R/5DO8X9OOaz7Z1B/5RD4F5kfe5Z5h1A2/Tx6+c8DnNBlLk5qN" +
                "az41GNc86lP8ijCdpXLzy4Y8rmfyxD320hq91MoRr1U+WuDzTO" +
                "7c8L4Z78mkHzurusMK3McdVlwcS6sz9cdYLff0AuOYO3fpGb2U" +
                "TV9ry7gC45g7N+z31FN8h/rvhfza2LaMKTCOuXOXXtRL2fRPpo" +
                "Nfl3scX9BL2fTbNmKWZIjj9W1dDfiHSEUuZeK6PlJKMAaO4Ko6" +
                "qMqxZbAXji1NG1CqV+CRqmPyW7UqNQHrOmpLdTy0lfqgc2Bfg9" +
                "Ptdrt9jm6Rh23tlh8/jqOgVsrUUnVQaAoJPqnFkPO4xFJeidQ9" +
                "0PlVDEVQm8AmuXW72FPaDmDEbCCjeMOMvCm6z4y2PNefWeB9Jn" +
                "duN3r2q4/Ncr9OsHxWgXHMnbsavX1cH2c5jhcUGMecuHufk6ko" +
                "hXHsoLV6OclWermKSoPDKOx1M+5k23HcoJ3OTnDA/mP9Isurgy" +
                "OW1oILE2sXtJM7hvNmvI/Ll9B93PosDa+9Hd3KPq57oaUdgCzv" +
                "U8Rw031cfou99/Yqx6LvsJdw3B/2VrJvx+9sPj/y26O2nMfvTe" +
                "/fymUNfcM/LPkdTVnR/3b4fQF3WHqwIbkV1S7j9yi6DzQ8aPRs" +
                "fluY3sXvbuk+E/2fq9419L5TR3ud7mkF3mdy54Z9s/rVlu/XI1" +
                "r6BmZbjeOIvOOI5sdey31ieIH9cXiBcdReFNB/nzH3x5j3AEZZ" +
                "WuFmeQ8gNbet9wD4U1Ecb7A7rsvHiuuP7eeujomdH2/0azsszo" +
                "/nFjiu28pd7in3iFTkUiau6/OlBGPgAzVYji2DvbC0X9UGlOoV" +
                "tU8lut+qVamJsPvVlup4aCv1QefAvkrd+P7In27eH/8vR/Xs3B" +
                "mjJ/r6AsvrnrEFjuu2cpeXl5eLVORSJq7rC6UEY+ADNViOLYO9" +
                "sHRI1QaU6hW1TyW636pVqYmwh9SW6nhoK/VB58C+gm7sumeR5T" +
                "4xssD+2F7u/wCAA6y4");
            
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
            final int rows = 59;
            final int cols = 82;
            final int compressedBytes = 2456;
            final int uncompressedBytes = 19353;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrdW12oHdUVPil5ECGl/lVoFZV4EU0bewO5JCK5596Z0Zoqtq" +
                "WkRpra+lPbIojG4JtmzrnH3LGn9Qd8KSJcH7S0EGqkbUAfBBGM" +
                "Qikp0l6hKBrT85LGPhSfGtx71qzzrbX2zHhynZPBzGb2z7e/9a" +
                "219pmfM3PujXfFuzqdeFdctJ0Oj3x/eBsjkoOCGYl3OqyCHvWT" +
                "DdYaLLmF+hrRPkMGLMH1vnWmIR+56hhCHzLWfG5nvNO1O7nNZ3" +
                "byeLiHEclBwYzEC33Vo35yvrUGS8UW6GtE+wwZsATX+9aZhnzk" +
                "qmMIfchYae9d2dvYu6D3DUJ7l3Q66T7mDO+0GQ5etEjv652SrX" +
                "dtgFzucjm308jWu7h29poytMp379IA2XSa0WxxK7k33ks1tfkK" +
                "7+Xx8GFGJAfFj/ojixefk+pRP+taa7DUZ7xXRmRtoF4VEyzBXX" +
                "7WZhrykauOIfQhY4Wt3nA8Js90zppturnE58TnUE0tYzTOFhmR" +
                "HBTMSFwqQ0/2tVZ5VFpfI2HcVpUtwR2ss5mGfOSqYwh9yFhha7" +
                "LYEPYa+tT+097xOG3fdfeZLJrA/jTuM43FvIb7zNKBSe4zg0Nr" +
                "u8/UXx8HLzd8THzc4vF4xn33jo/X8c8N53KyxXU82eI6Hm5WOb" +
                "ulvXVcfmiq3wZuT26n2reMUImui67DLGPcgxVZco84xJOKvkd6" +
                "0lIqyJjkjPQfcjRDa8Ib+w51eR7KOmvrDRjzbfwlx+OrDZ9bv2" +
                "nxvJ6q72R3sptq3zJCJdoR7cAsY9yDFVlyjzjEk4r58biDuWUK" +
                "MiY5I/2HHM3QmvDGvkNdnoeyztp6A8b83GY1WnU+VrnNs10tyr" +
                "wrBTJG58ezYwupQRw3micUenkvsAZLbqG+RrTPkAFLwZ23mYb8" +
                "gj0vNUvyW9Wx5nPHomNBFjnm6k2uHCOE92gTj7kGX3K8pVX2er" +
                "AouCX+oQlPXMpZ8B6iwrtBq7xHm6SHstjsqDwHvj72jw5ea/ga" +
                "9WSL18ep+o6OR8epppYxX/pHo4QRRpfO41lYSI2cc74bJYRCL+" +
                "8l1hosG5XW10gYt1VlS8FNbKYhv2AnUjPMT0fMttXPhf2jwwOf" +
                "/7lwuIznwvip9p4Lq3w38/6xbBu8Xqz0TDTjnksfVKs/M8ExPl" +
                "POI71Gvs/fvIYzr8J37/lJmWt9nnHXxzcavkb9qsXr41R9J9uS" +
                "bVT7lhEq7rzOMMsc2/Mt2dldK+a57GdumYKMSc5o/5ajGVoT3t" +
                "h3qCsj1XnAo/QGTPKT7cl2126nNse3c3Hr+FvMMsf2+iOyJETu" +
                "WjFfxx5ztV9m2gigBMRyNENrwhv7DnVlpDoPeJTegGm+37qX+c" +
                "JKvkej4XMakzXj/RHPS1SuC9TjR/VM+cbscjWryTFxVMBlz/su" +
                "05aYzl5nKUtNVFf4ghHt7nh8UWOyBo7WolY9fkLPVKzjFXVqVp" +
                "Nj4qiAy573XaYtMZ29zlKW6qgq7zNHGr7W91u8z0zVd/Ju8i7V" +
                "aLm44/F3FsVIWkFH7r5mH9SPf01tmYKOyWoDsRzNCLU5EvId6s" +
                "rYia292lWBquQnq4l7zvY1Wi7u+vgni2LEPXefKfq+lntSPM8z" +
                "3+WyRK1WIL5YR+XN+rcczQi1ORLyHerK2ImtvdpVgarm157Xf2" +
                "343Hq8xfP6jPsW6/i3hnNZbnEdp+q7u7W7lWrfMsLj4UHMMmZ7" +
                "/RFZEiJ3rZjn8ghztV9mhhGE/i1HM7QmvLHvUFdGqvOAR+kNmO" +
                "R357pzrp2jNsfnuLj7zB8xyxzbc+s4R3Z214r5Og6Zq/0y00YA" +
                "JSCWoxlaE97Yd6grI9V5wKP0Bkzyo5VoxT2Zr3CbP6evUHHr+B" +
                "IjkoPiR/2RxYun/XEfvfiAtdYW4zcFKzIia8OM6phgCa73rTMN" +
                "+chVxxD6EG81VmTkaubg+J3Xy1Uz2Oh5pp4zvkY9Vu+voXeqBy" +
                "f33eBdpe794wR/B3A6f5cSZy2+f8zO9PtHcb9+p+F75tMt3q/P" +
                "uG+xjv9oOJdBMzqDf7bnu+Jacig6RDW1jPnizuu/MCI5KJiROJ" +
                "SlXp5Laq21hYxK62skjNuqsiW43rfONOQjVx1D6EOuoIxcbuLv" +
                "9r7fOWu2pQNTPouvrF7Hxe+tWfXaqa7JC7WzfyhDq3LR95ml35" +
                "/+fSY/0zbHm7mnMb9nPyCUx5objjQHitojzcm2Oi6uudRFjz3M" +
                "yV1TFy1a7V1HW+VTey75Rrh+/Bn+sPQqP5r2OTg4PgHnfdH/wO" +
                "3/rudzLoMPP0P3vdrZYxXrPRPPcE9jfs92E8pjzQ1HmhPPlHuk" +
                "OdlWx8U1l7rosYc5uRW4yaLV3nW0VT6l5/ShdH14fUy/7OoL3P" +
                "PM4fTCHPmq29337fThgpErpVfR80yaf+9N3Tf59Jtu/1bB2Zpu" +
                "s8rZj4u5G9Jvpze69qb0O+Xxp3vSnJv+NK/vSn+e/iJdl37JsD" +
                "akXyl6F7n9a2O8+O0kvVr43pMW35fT4lep9Pq0my7Wr2N6awn2" +
                "IzW62+33xLPxrFvPWW7zFZ7l8fAVRiQHxY/6I4sXn5PqUT/7ib" +
                "UGS33GszIiawP1qphgCW52h8005CNXHUPoQ8bq9+RUcqoYbxy/" +
                "a84xvy+8SbM8Zi5QPaI9uxcq7nx6wKzRRppLTg3u9/X+75b+3n" +
                "FK+iA+Ys1+plnw7tX3X6Yjo41zkXHDvvJ3l2A+3qgxuS5V95mF" +
                "f332faY/+mLcZziXKd1ntsRbqKaWMRoPX2VEclD8yJ3XBs9+CW" +
                "Xoyb7WKo8KEVmbsritKluCu/x3m2nIR646htCHjJX2uv9Dmugb" +
                "9xfk/5AWHpvke7hnNfZ/SOP/9lh4o3PWbNPNJXo7eptqahmjcX" +
                "YfI5KDghmJQ1nqSVRrlUel9TUSxm1V2RLcwT6bachHrjqG0IeM" +
                "Fbbl782c7/82fEysb/F4nLLvuvcUn0O19D1F9mB77ymyB6b5ni" +
                "I6GZ2kmlrGaAxEclAwI3GpDD3f6/7fWoNlo9L6GgnjtqpsCa73" +
                "rTMN+chVxxD6kLHCtvK8/p97vv+ouWO/Ka216FTZNBNTciQ5Qj" +
                "VaLjSvUYy41x9xn9mwgg/0JYLWxiRnbEyaoxmhNkeS9axHyUQM" +
                "1qtdFbkykl97PH7S7LU4a/Hv9pZfmqZ68lbyFtVoubhjfsGiGE" +
                "kr6Mjd1+wDfYlIBR2T1QZiOZoRanMkMhepK2PnCMvytFFofvdE" +
                "94S7Bp/gNr8bnODx4o0aAZfZ2K0O9WDNfa2AGWzamxyDq7GwL7" +
                "WRS5kuYpcRhlHaKAz/U2mlUlI=");
            
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
            final int rows = 59;
            final int cols = 82;
            final int compressedBytes = 1672;
            final int uncompressedBytes = 19353;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrdXE2IHEUUnoMHGRQl6xJ01x+SFUWFQALrSZjunrmI4HU9KS" +
                "bBi17cSGAxxu2ZnckQUMghxyUIESNJxIsBUSHgIaAuCsp6lBxi" +
                "1MPkIMSfCHbN2zfvp2p6e3qru3e3m66f19/73lc1VdV/m9Rq9t" +
                "Zeqe3CbeXvLKj2KX8R23eNYv/rty39fnX92LtTTpzlu7HUOThq" +
                "t3NkLu/LG6N7f0YtL2XA1Fl5T3Ls3WQ8ntnAzm/C+3zq2YXl11" +
                "324FBwCFLI0QZ1bSEsounQPMhA1uR3eU8zEEpq0kxck8RIBNdA" +
                "bTB5746bl7Qjs4wqY+meIbxj1EyP1C5mWAVu7oz1MUtbkrbv9R" +
                "ex+9B275PuTA6fBzOhZvMpatxq3IIUcrRBPTgmLYRFNB2aB0rk" +
                "jWXJQGekJs1EFo2RCK4B8dQWFy9p5wptlVqFatGgMUjyAebDMw" +
                "Os989IC2ERTYfmgRJ5Y1ky0BnWjwMdg1s0RiK4BsRDvvKxm5e0" +
                "c4W2Sq1C4+0tvjEa5Y/6XR+be6pbC4qNHd2ObkMKOdqgHsyhhW" +
                "NopzPczpmT3+IebcW9W+dntCrJLy22bokgT8JCW2xejqe2Sg12" +
                "DK6VfGu11vHWcZNCGUvBAe6BGDpvcj4e0So340H4LL8u6RjnIx" +
                "kxAvfjdZNDW9CPcxIX99P+EENiN7lm7Stq9Acz1c3r/qWyI7L1" +
                "8XHP/ThbXT+e+rT0u7MnRqUnd897iv5nZURpLbQWqAxHMn4esT" +
                "GYEpYjxnFzvBtl87hjaL2oiZgpxZLJTVtICefkbSc/3UrwldhN" +
                "5vWx0W/4ye4Zj917y464/JaHNfaAZfnJ45u9C6mxf3CuzUcyre" +
                "AJKn66+H6Mf9yx/Xg4Uz8e9tePNK+37TXjixz3Cq/4Q5U3rx2/" +
                "zn0euV7I0Y+v+kNl7Mf/Clvrn6twDH+ZCfXVTujHKrfgZX8oew" +
                "unw2lIIUcb1LWFsIimQ/MgA1mRT3O5NGkmrkliJIJroDaYPDjq" +
                "5iXtw348qqPKWLpnqAfKHI/NXyp8b1ZC7NazZqcaHBIBKErRDu" +
                "97oE5WN3vzuuYdp2Y8m+ZETahK2omved3FzW2y9bKVfHepaiw2" +
                "FiGFHG1Q1xbCIpoOzYMMZE1W8a81A6GkJs3ENUmMRHAN1AaT9/" +
                "5y85J2ZJZRZSzdM4Qv7b6nnYyJs57uw3/OMa/PFrpqnGuegxRy" +
                "tEGdLBxDu6m1b2o7Zya+4Sp+SXsTSqsiRdrHpVuzoidhTWzZUh" +
                "tPbZUa7BhcK/mW9TwTXKjwvqfg2PFcvD+eip/ZqM1OOq/jmYzP" +
                "148lbfH0/ihO/VofP+XsxzGx44cty4TP1/HBsp8L+99W91zY/2" +
                "Ynvjdz92NwucLn68u7pB+T63VwscL18eLuGY/Nj6obj75ib4t+" +
                "PF9hP54vut/CAc/DQafN650HwoGN3cwSDlxxpLX/ne2lMe5one" +
                "l0BbpNGsXL8QfpLUpjd5/NMh7jF7c0Hj8sYzy+6/yrJF+x88zr" +
                "5prfee2NK8+8XquqH6OpaCqZfeLL1ta+cxk+L8/XOb5zjYttz+" +
                "syvhdOeK+xVMR4DJZy3T8ulT0e8fk6momSZ77Om/6YI09/J9X/" +
                "3l9sezz67sdarfX21tl8cPjYitbR+Bx2rMGxEfudLP7pVuSQzF" +
                "vVPPnZcW2x0b5U0vrYOuFhHJzYHtdrH23J3Y8nPfTjSXq+9rXl" +
                "eR/uoy0pd1WrzVVIIUcb1MnCMbTTGW7nzMQ3vPtf096E0qokv7" +
                "TYujUrehLWxJYttfHUVqnBjsG1ku9W7nuy/nsFf+MxvJpnPIZX" +
                "M90J3NgJ7yn8bL25XO9xf632fU94xeMbkSvV8Yzz8aWpzO8z3S" +
                "OeNOf4PtP/cwxafJ/prOb/PhPVITW5SaM62k2p+xrYCRvVxXNC" +
                "3XpyGHFopI2VPOCZhqP4WidXqGvaksavo2s+9He1sIz1cTgmfq" +
                "vueaa3v+Cn6RLndXi6unk9Lra/eR3+A6nJTQp1tMB5slGZe0sL" +
                "95T2lOvAKHoajuJrnVyhrmkL55fvKezomg/9XS2UW/eNwub1H2" +
                "XM4K7zf5Do/1503GgeUpObNJpHO55BjCxzb2nhntKergI803AU" +
                "X+vkCnVNW9L4dXTNh/66hY1rsGMNjgnevVzLZp2cedKY6Wdb72" +
                "dBG9SkKgFf5nWm5enfnOa5zoyL7evvexrrsG/07TocE/wa69ms" +
                "k7HmiZl+tnU6C9qgJlU6xP8P+BK/bA==");
            
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
            final int rows = 59;
            final int cols = 82;
            final int compressedBytes = 1853;
            final int uncompressedBytes = 19353;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrdW01oJFUQbkE8iCAbFzyoMCGLuCoRFDzIHma6Zw4L5p7k4E" +
                "E8eFDcHPbuziQzZoKg4s1F0IuuSETiRiVrNru6gSQHj94COZmD" +
                "J/ciimL3VFfq1fvp1HvdPePaj9fdVfXVV997269/zNg56hxFUe" +
                "eokx+jCC08xz1hqGVW71j3RxGy0Bmc95f0bEKpm4owKxK7SxNl" +
                "Enb1rj5SE09j5RrMGqpW6N1z3ZnuI91nwdt9PIquXI48tu5jVu" +
                "9zhqcRVbZ1Hy2MPm3zrv7pQD/BUH+lnmc81Txv9/ffjO7prf+G" +
                "zdt5T5IrQ1ln8x68Hnu/+V+Pnfcl12OGCr0e409gnx2zPdjogT" +
                "j56FzN5h41k/vdaqh6EY7q6zpVhbqle4r49eo6H+brI2xuQkML" +
                "unyTov2ZQ2vao513JWgXSqDpW2hoQYdteJ8kX1pHji1X0x5958" +
                "ty+svcH+MXQu+PFtyEn9eusfD7Y6XPu9qe18P7J/e8HvxRb9Xm" +
                "T9DQgu6TL/P6M/vWLI4OH5Ciw1Q2f4SGFnSffMPzudXrzexT8/" +
                "RopkqoP0hl8wdoaEH3yTc816xeb2afmqdHM1VC/UEqm7vQ0ILu" +
                "ky/z+jP71iyOxktSdJjK5g40tKDntbuSfENx1+blzCXnccc/6h" +
                "qLXWmApm1oaEHPa89J8mVezlxyHrf9o66x2JX+F76vO+dd39dZ" +
                "ZFLvj67a2vf1ef/v69rmcdo5j9MTnMdp0TxOh85j8zY0tKDn71" +
                "xLknyZlzOXXNe3/aPDS1J0mMrmFjS0oOf3lClJvszLmUvO45Z/" +
                "1DUWu9IATTehoQU9r31Wki/zcuaS83jTP+oai11pgKZb0NCCDl" +
                "vrZ2N1vG3m21nxbGXJxjyKvBU8j7f8o+ZYXOhidmfVG9DQgp7X" +
                "3pPky7ycueT1eMM/6hqLXWmApjvQ0IKe1/5Fki/zcuaS83jHP+" +
                "oai11pgKY9aGhBz1fxqiRf5uXMJedxzz+6clmK9lUJ+BreHxvO" +
                "98fGBN8fG6L3x0bo37lqmMdZK67hioxpHmdF8xissLkBDS3oPv" +
                "lynB9zaE17tPmPBO1ChWz1/V2h/V00hs3+d4Xx1B7TPG5OcB43" +
                "xzN7yRzuk7kiHz/XLe7TY8mcREfWJEqpiqrQlXtaXOWyz4teVb" +
                "s/HEBDC3r+LZUI7lUHMi9nLnl/PPCPusZiV+qvprkPLfftQ/dg" +
                "2Zd5/Zl9axZHW2cl6AzlqxLw4/ydVPv65N57XLXN3wGE/k4qac" +
                "E+O2Z7sNEDcfQtn6FzNVvdlqfUzGIsj0F1UuBCqepIOR+NXlH1" +
                "FPHr1XU+zNdHWM/1aH8PHz45uetxMCO9HgPvNd9AQwt6frcw3u" +
                "6HX5v5p7yFPGRjHkUeDNfsH23NhnB5aLoODS3oee0ZSb60jhxb" +
                "rqY96hpLVZrSNZ7vk3yfnPizs/5ycoKJIvVczeae5IRT9xerSE" +
                "5aMYqzJZGuULeiwhHYtUcOvkRT4d787o+94+ie2PqX6uUves60" +
                "Xq/2vaf/2uSeM66x8OfM8scV/v8KH5x8A3yUPlnO+Y4yy/KN+G" +
                "29I/8cV+3up7WtgA/rYh4+Nbl1PZipkz0exAPYwxF9YOsewiI6" +
                "HvSO0ct5kIG86dm2zkAorklnUjVxDEeoGmgMI3vbzkvaVYWmSl" +
                "0FjY7OR/ePI9y3LKtG9fG47TnTcqy7lmg9to4kOBWlqj6tdjG3" +
                "qzZlu3jG+X0d71SyhnaCfme/U+f3dedq5yrs4Zj5wcJz3BOGWm" +
                "b1jnU/sPMzOI939WxCqZuKMCvGu6ZunRUzSUlWm4/UxKd38Bf1" +
                "qryWqZhyx/f+GFfy383CWOL9Op8z/S/81nX/Wqn3x88m+P4YS9" +
                "Z1hgpa14edQ9jDcXSlHqKtewirZqge7s2v/BO24Us6jlDKSing" +
                "NxXZFHBuOA5m7Lw0B1idV7Wzm3jLuv674Hr8Srja71q/Xl+d3P" +
                "ujrPbwQqr996q/C6t+Xnd+rehutOG/rl21q3letzfaG7CHY+YH" +
                "C89xD76kh1HKUDkAk/7r95CB2NGrZhNK3Ux+7jF166yYSdisNh" +
                "+piQeUirSNjyumXJ/ndbImWEFrNlz34WRNki26Al4OWNdrUlRV" +
                "Kvtb0f9w638vQgX+QrN9sX0R9nBEH9jkQW+yglHKUDkAk/6bri" +
                "ADsaNXzSaUrorzc4+pW2fFTMJmtflITTygVKRtfFxxHptvz6fH" +
                "eTyOIvNokwe9y2cwShkqxwgzleq5ggzEjl41m1BMm8HPPbymia" +
                "BMwma1+UhNPKBUpG18XHEeW2wvpsdFPI4ii2iTR8VQy6zese7P" +
                "+dkZnCev6NmEYtoWVUV6DrG7NFEmYbPafKQmPn3vSfSqvJapOI" +
                "8ttBfS4wIeR5EFtMmjYqhlVjqPmj/nZ2dwnizo2YRi2hZURXoO" +
                "sbs0USZhE2OkJp7GyjWYNVStlKs9Ddl8x+tuy/Elu27HxeuSbN" +
                "G38np1Oeb4glT+C6a8h5s=");
            
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
            final int rows = 59;
            final int cols = 82;
            final int compressedBytes = 2132;
            final int uncompressedBytes = 19353;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrdWk2IHEUUHvQmgmIUJWtAXFGIgQ3CqtFgsjUz8SqGHJJokt" +
                "1kiZ6Cv6BkD5mdzIQ5BZRcRA+BINFcIv5FyEFyE3YxmAQEWVFz" +
                "CIgg/qAHUbv6zZvvvVfVsz0zPTOabrp+Xn3ve1/VVFd39a6bcl" +
                "Olkpty7bxU4hqXpQVYRuOyPMwAa6nUetIyAIVDR5N1YLUtLEtu" +
                "ypuTcV5oZ2YdVceyIwN8eNR2lYZ0lPeWxnYMN3ZlW2UbpZSzje" +
                "q+7M6QnWzuDLfCQ3IQJvmFzjAD2N2Z0Bsoq0rzawuzax5ZY0/0" +
                "0fvonsoeMdbXJDLWP60YvvY48kr2uNcu5ft9au2ZfvT2juVycl" +
                "8/Vcyvv3i6a+yLMeuxl/Oy1x7Mhzt669HbuFydrk5T6nO2oI5W" +
                "ttkSYRgpL82Yro9PMzbGIDXJFh3fYjRCcyJaMvaHwlZ4yOg6qo" +
                "0mR0biI7/HgaGtIr+Pb30cfewhjuNPYxzHocaunqieoJRytlHd" +
                "WoBlNC7LwwywJn350TIApTVZJqlJYzRCakAffO5jx3ihnZl1VB" +
                "3Ljgxja49H5uMj7TeFqXLytKi/qN4epjJn8WaJieGIL+L5cK+/" +
                "fuultuejPbz3ZCivncyL7Ha4CTdBKeVs82d5sjzpJlqvwuJtXC" +
                "I0LvB4DOEQw5eJD2xoCTXpGNKSPq+/07awLLk5epy3dlL23edx" +
                "lVYF8P7qNh97XFU351ijDhczHzuePczHeOzCVt/dld2UUs42qs" +
                "MiMTh9bfGatUtm8KW/3QHrDZRVBUXWJ6bbsrInsD627mmIR1+1" +
                "hjCG1Oovt+AWkhgLnKe9XeC6tQDLaFyWhxlgTUr7LQNQ4r5esD" +
                "GkxWI0QmpAH9L6/jgvtDOzjqpj2ZHpYA+7ZL77lPK05XCnvktb" +
                "gGU0LstDJXinpV2WASgxjodtDGmxGI2QGjpRO32J8UI7M+uoOp" +
                "ZklfjySnkleF6lNn+5ndTKdcbCKvHwg9WM0E60SZ/I03VFxiCc" +
                "5tcx4pEllvuiceWV1mtdn/Irq9msJrF6d3aiM18EbxxHc7yVNN" +
                "Uu9IXM/emhUb6Hh31pq60Xwe7Wu/WUUs42qlsLsIzGZXmYAdZE" +
                "83HLAJTWZJmkJo3RCKkBffB5czLOC+3MrKPqWHZkgK/dV5usra" +
                "ltaM/Fu7t/72l8E8zeiW7fe4TlniTetwXtXO/s2ro+OmMyYtfW" +
                "qf59nv97T4fhoQy7+sLUeKu4e8tdLoanebW42OF+pp+jfrAevL" +
                "/VZwfcsz8QjXQgq6UP1c90a218m19Vf6j4ONb31ncWOY71Z7PG" +
                "sfXGKMaxvi86hzfkYt7T59yZolPbBpyPU1nWQZnzKcyOPyhzF7" +
                "+NdGrbgL3cmGUdlDmfwuz4gzKPdn3MiOTv6zdHcV9nPJseG+bb" +
                "6TDWx+q6zOfMuvGtj/liF6UwfO9xPb9j1DK/ULmrxSjshyfLpx" +
                "hNbpPbRCnlbKO6tQDLaFyWhxlgLZUqz1kGoLQmyyQ1aYxGSA3o" +
                "g8997BgvtDOzjqpj2ZEBfrX5uNqxeC3/fKw8XxrbMfrY3faFA6" +
                "wWi0lfthfE1ehjHLcPc8yaNzZ+pf114+dme3/dKOAvvc0bgl3G" +
                "H8nz+u1iVDd+69r6d1TRn6uy/tXO//kvz8d0Ttwyxvt6yLG7fe" +
                "9pnc/h38P3nsI09/G955jL872n/k5x33t6m4/hc6bL+rhmjOvj" +
                "mlHfAe5gWCqSdTxcRcaPsM+7eUopZxvVZ+7VFjff+hBl9pAWbU" +
                "1X7ZvZqpncfOMm2aI1ZfFbjVJ/zIY++r7EeFsfSF5ukaMQYw/x" +
                "SXqaUp/71LX/uxAWxlDu1H8fuuB/EcGh0WCOeSK6xcVQki1UaG" +
                "vWIvmtirjKMJa2D/4eHnnDGPpTufVx7z4zW4tDRX7fI+4IpZSz" +
                "jerWAiyjcVkeZoCV+SxXTJNlkpo0RiOkBvTB5zNb4rzQno7jFh" +
                "tVx7IjgxGIfC9ZLF2Hx8wTxaFy3pd3XI/j2PokF+rTPu/raTdN" +
                "KeVso7q1AMtoXJaHGWBlPssV02SZpCaN0QipAX3w+Uw5zgvt6X" +
                "ws26g6lh0ZjEDkOXO881Xz9etnPo6mL9Ud1R0o0xXHcAqsRGRx" +
                "U0v1fBa39ZBs2VqgVqpCyqW0fF4p2RHvO/xsL8lXY/Pur3Pt0X" +
                "rYX7e+HN/+ujmZZ3+dWvraX4fj2LxrWONYPT2+ccyKXcw4uj1u" +
                "z9ZTPqW8VOKar/v2radgAdYj6SIvtjCGcO2VuI0nJq67PdVfuM" +
                "UfPppHyGgcQ8YnrLZpD90n1mZ7oZEUidRqlTqWZGUl8I0/Z8pr" +
                "y2uTe/Ficauw5xvXMdzYlX2VfZRSzjaqwyIxONEi7ZIZfOm99Z" +
                "71Bsqq0vzaEuq2rOwJrI+texri0VetIYwhtaZtc5W5JJ/jPG2Z" +
                "4zosEoPT1xavWXubX5WoXH3fegOltM1JRdYH7Fma4Amsj617Gu" +
                "LRV60hjCG1pm2zldkkn+U8bZnlOiwSgxMt0t7mVyUqty5Zb6CU" +
                "toBfW3TMEAFPYJuTtqchHn3VGsIYUqu/ysvl5WTtWOY8XUmWuQ" +
                "6LxOBEi7S31yPBwaXW19YbeLWWBfzaomOGCHgC25y0PQ3x6KvW" +
                "EMaQWtO2pfJSki9xnrYscd2X3TmyA4MTLdLe5hccXHLnrDfwSl" +
                "vAry3Ek60JnsxGPrqnVAYXx9ZRw/5pxdJ3VH8vdF+N73k9+tjl" +
                "z9QXkB+yWlb3tnh3cfT6w9imf98XtDfosi90rWL3M0Udff2ffW" +
                "u4+8JR3tfVd8f4vWfIsUf5naJ6doz767Orzcf6R8V+pxjeOLoL" +
                "hTwxLvR1X1/Ic1837/8/jGP1yhjn45Whro//AnZtxDA=");
            
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
            final int rows = 59;
            final int cols = 82;
            final int compressedBytes = 1259;
            final int uncompressedBytes = 19353;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrlWkFrE0EUXhRLEdRDDx5ULE1Qaa0FEaW3ZpNcSovUS4viod" +
                "o/UFBp8WI32LQN/oNCwYMibTREClIvHluEUrFaCl7ESy968qAF" +
                "wSTT6Zt5M5PubmZ3NtsJ2Z15881733v7Zmc2Wctykk7CaXMuW7" +
                "XinLWsqYeWh+KckUp7BEm7ZaVvW1qKc7pub6dMqrLtnGNb+WsV" +
                "SZdHNldrxzpxzGz6jaME125pK37iqPKFj2MjJXcSaX6wf3XSld" +
                "42q8nKk/NSL0+EzQPi6Ooq7Ajjiyps9rW56AZr2+6wO8iRnKmM" +
                "tDOfeQlgKdruyO1QKa+H1GA0rfMaoIfnhDWBBGN4BMuB4sEXmV" +
                "7gzjIUWWIWGF8vHzNf9eajyeLGlwa0j2XGyJGcqWyvvUUlLAY+" +
                "0MPKWc2gj63zuuSseP28ROSNtdKRDHYLeyriwVeeg2iD5Qpj+Q" +
                "Lr9dxPKzZlZingVaXOvifVi9FPXzayfzS57xF9Ue17/OwfM+OZ" +
                "cXIk51qmjtM2SFgMfKCHle/lO1fj67wuybwW9PMSkTfWSkcCdn" +
                "YXeyriKzPwF7Yq+sczhrHqea19d3Dc3LwO37a3OIrrdR1fWgzG" +
                "scXc/VEs098auT/aR83dH1W20fP1Hb/P16HOa4PrdbC27X67nx" +
                "zJmcpIG0sAS9HwxXqoBpBWaq1YA6B4TlgTy4nH8AiWA/hQa7fK" +
                "9QJ3qpm3ytvCkQG8WPJ3A7tqRwzmY6C2s1PZKXIkZyojbSwBLE" +
                "XDl+lLgARG12oJrAFQPCfeBivBGB7B+gI+1NoJuV7gzjIUWWIW" +
                "4B3U0X3zXVBXbe6vuXyc/RjtfY9kBj2K4nNhsKzy9yWyew0yfu" +
                "yRw2gocXwc6P2xUsiRnKmMtKv1mS2QAJai4Qt9fS9AAvpondcA" +
                "PTwn3gYrwRgewfoCPlTPVVYyvcCdZSiyxCww/qDfw2e2vV6bvu" +
                "0ozuvwWXn7X0EygyYjeX+cbLY49i1FMh+Xmi4fJyKZjxNNl4+L" +
                "kczHRXNxtBcqe+ddz1d+wXuPt5L77iMfFwzGcd7XDJr33hPCvA" +
                "7dNv88M/dD09U5VfHlvSZdAz7iqLDtPA/n/qgrjjVfVgzm44q5" +
                "ONo5X4xzVgRL+KwaXa/VpXDMXBzziWjH0dP/XKtaMms1vFERje" +
                "OaljiuhTcqmnFMj5qb1+HbDjCOwwbjOByjOI4YjOOI2TjaZXVL" +
                "cScqy3F22c1oV/e6sr4xon+6WKI4ltQtBbOSHGeX3Ix2FZOSvj" +
                "Gif7pYojgW1S0Fs6IcZxdtTW/q+tGjGsM/z6i4R2wffsHgPvxP" +
                "fJ5n9JTU7/BG6Ylj4ZIVm1K4aDCOnfGJ42yvuThm/8UnjkH7Uu" +
                "890uxNF+Ob5D17lS963rM/IB8HY5SPg809rwvdh2Ne111nemK0" +
                "Xl8xmI+3YjSvQ/eFWWeGhBhvurwW4jrzRR/D3Ku6tj9J4zjkOo" +
                "+6tOfjQIzyccDcvNZ+j7pu8Pna2P9c6e50Za0t3GD70i5W3ypG" +
                "hiP6tMxrH++luLeti+V+HJPppGAj6YJHUo6T6QuvuLeti2XUf+" +
                "/xV1If9KEOdRzf6kNJVrH17Do5kjOVkTZIWAyLZkexLaoZ9NE6" +
                "K2F7MCvWBnxUvHnOmFn1nFrGnrKj9+O4zHuG/RcZsz6j/eP+E1" +
                "RqA/dNv2lozXxmMB83AlX/H1f43Cg=");
            
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
            final int rows = 59;
            final int cols = 82;
            final int compressedBytes = 1581;
            final int uncompressedBytes = 19353;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtW81rHVUUn8SFWBAXpRFRIdISgh9VJH9A58570YXYRUlUBM" +
                "miKzfaki7cKL6XpEmnjSUg6EaCO1tBuhJFkbjpwlVRF7UiaCi8" +
                "TVduBX0z552cj3tn3rzJnTfJ2Bne3HvP/M7v/O559775fEFgL5" +
                "3lYF9LHAYHcFn/edwROz9iLfwpaMxSbV/aW+0t2EKJNmiHf0gL" +
                "YRFNH80DNfLGumSgPVKTZiKLxkgE14B46ouLl7RzhbZKrULj88" +
                "Zjk5bwAX8oezFLZgm2UKIN2tpCWETTR/MgA1mRT3O5NGkmrkli" +
                "JIJroD4kZTjh5iXtaR4ndFQZS2eGMuAYj1erGhOn/qlvPFYbe3" +
                "5ifgK2UKIN2mThGFppD7dzZuJLj+QvaW9CaVWSX1ps3ZoVPQm7" +
                "cU731MZTX6UGOwbXmu6bnJ/sl5NYpnsmsZ3UTQ/shKGV9nD7gF" +
                "/UoG562ptQQpvFLy3Ak62JPElJ4iN7CnXiwtgyqt0/qZj7yuXD" +
                "C1hrnRo+oru9gmdTv/qbRd0vciPdcp7VvjruXxLKo3nZ41lpzX" +
                "n02ZdR8+j17L7mPIZThc57pqrIY7xoafylYNaeP2h5XHu48Df+" +
                "jKeRsxw0cAkf84cadTyGR6qc1/FrY53XR6r8llp3W3dhCyXaoB" +
                "2/gRaOoZX2cDtnJj5el1xuVZJfWmzdmhU9CRu/rntq46mvUoMd" +
                "g2sl38zx+FCD5nXFfemc6BzvHO08O2g9IfL4XAH/xwseZ6b7v/" +
                "VnPWl+NHfv0848ZvSl8yRvrXw2+nGm82J/RO62dmELZTpSd7Ed" +
                "PoUWjqGV9nD7YLyLmqxLLse8tvilxdatWdGTsNAXm5fjqa9Sgx" +
                "2DayXfrON1+EKD5vXY+1LRefgjHrleGd3n0tdFUPHS/TwOGY9v" +
                "+UPVncfwWo15vNaceR1erzGP15uTx/h8fXm8eLyuPEbHomP93+" +
                "cT/vKY8NWVx6zYnc8P4Xi88H8cjxXk8b3m5jHvurCQ/wjXheZy" +
                "fdeFWbHldWFqKXFduP/xWPj5jLfzR7NTZjyanULn4e8fhnnd/q" +
                "q+ee0rtp882s8V3OPR/Vxh4zc/Csvcx20Vft/M13OFZl4Xti/7" +
                "Q42ax3i9OXlc/3Lc8xrvm0VT0VQQrJz3xxx5eqoZL/uLfRjPw+" +
                "scj/FGc87DzYP15dFX7IOQx/Df+vLoK3aZPJrtUufJ25njcbvG" +
                "8bhdZc7MGXMGtlCiDdraQlhE04f2hW+ThbyxLhloj9QkY3BL+l" +
                "t3RdrsOueGMlHl4iXtXKGtUqvQeMe3Ld7HjT/w+K19H9S2VBvb" +
                "bJpN2EKJNmhrC2ERbTa7PbRKHmQgK/JpLpcmzcQ1SYxEcA3Uh6" +
                "SMP3Lzknau0FapVWi8I7dXeW308Wgy3y8339U4Hsce23xs18r5" +
                "W+fCP9SXx2Kx4y3/1zP9jKyUyuNKcACX8asyn9q1nHsvvWx/a8" +
                "9NL/pujs9rXzo/sWsj5THTKzpb47yuNHar3WrDFkq0QZssHENr" +
                "0ur2tJ0zE1/al0XtTSitihRpH5duzYqehE1iy57aeOqr1GDH4F" +
                "qTT3uuPRcEyTYp0ztwc7hCy9wgC2+RFyLxk2AAxxn74/RGYuUW" +
                "ybB3D3COx+BtxElN2oN7Ih/46L3Uo725NNBOnFIRt6Fard9xXX" +
                "g0aMxSbV/MaXMatlCiDdrxN9JCWETTR/NAjbyxLhloj9Skmcii" +
                "MRLBNSAeykvvunlJO1doq9QqNH5c93vSs7Nv6xuP438PoPP34A" +
                "g3E83o91ISy9Aj44wbB3w+lu6fJY7XGbHt++FlVJoFswBbKNGW" +
                "rNHJ6KS0JDasAZo+xJNgAEcxkjrycQZCSU0yBrdojETwviAeo7" +
                "t5STsyu1VqFYSnesZ4nI1m9fOZxDL0m59144DPy+9DiXs3WbEd" +
                "47GUytz/K1hPgtd+t/yHvJeyem5gmdaY1XdK//KU+b/CrQy0eC" +
                "/l4ptl30vJzeOdBuXxTpE8ru3s9/2e8B5uoUY2WZN1p+J7WWjp" +
                "mcUj/d0IieKqs1mLqM+KTd5D4/yFW6i59tr1PK7ido0qguMorn" +
                "pY7HzurNjkncWTO69vW3NxcdR5vWeZ9nhmVmZe3y4yr1cXSs7r" +
                "/wCBcX+u");
            
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
            final int rows = 59;
            final int cols = 82;
            final int compressedBytes = 2468;
            final int uncompressedBytes = 19353;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrlW01vJFcVbUEUMSZiARKORKxEeISlZMR8Lex4EsbdVZUFGY" +
                "2QEGwmWMoviAYpP4A27p7BrfyIWUfySB2hbJDIAjbAZrJBGiMg" +
                "0WAPSAMSIFjwVa9u3T733Peq3W7KdjD1VPXeO++8c899XVWuru" +
                "npdHTrPtGjtIBxi9uprfukic0zm3R4fprBLOu6WXUW902xMbtJ" +
                "p3++v9z/Qv9C3Xuu0/ne25PZP+8cuvW/5JGdn5ToRe1t3655L3" +
                "je9ludObf+4tTRF5Mr0ZBLfylCXjqimyuHrWO8bX97hnX8qV3H" +
                "Ce+FTmvbPOt458NZ1nH7W8exjt0Lfs5g7/B1rNDJOg6eaVrHwc" +
                "KJno8XZlnH4a151zHezDq2cA7pOiZG5l7H7W9MHb2ZXMcWr4fk" +
                "ah7pup7l/sjn4yfluu6+Osv5GFjtX9c7e//9df1JWcedh7Os4+" +
                "CD+dZx+OnBn2UdB38a1us4+Ovk0/nOvJkOPxV9An9rbx0Hf5k6" +
                "+q/k+XhoLoN/1PW/W/q0/zn5DH/VOTNb0/nYztbd6m7JUWrFpO" +
                "8RcJWN3euoAlDV81opT17JemIOM6wH5FD130zrwnvFfdNH5Vh+" +
                "ZbAC087Hs7Td/dFMZ+1v2os4/PVR2FsH/yPX9e9POuLWU1Jnz2" +
                "XlX50fnG9POeid1tYUu3+vDfX8cf5YjlIrFkq2lC3lj7//XcHB" +
                "QcGIxa0y9KpclvxssLwr1mdk5w/et1fVmXASYnOm0u7fs7ljXq" +
                "znM7T8fDVfLetVrauRVe0DsRyU0Ns68HitTy1us1ZiHVetIz8H" +
                "6k2eMBPcnSc+05iPXNlDHMN6rcbW8rWyXtO6GlnTPhDLQQm9ch" +
                "0dXutTi9uslVjHNevIz4F6kyfMBHfnjz7TmI9c2UMcw3qV/f/l" +
                "e2HxzizfZwLrGL4X/v3srOOdd4/3/WPi7/Wzttf7YXv5t6U1j0" +
                "7TnHY89a73rstRasWk332FEXCVjd3rSAuztc0KGGFPXgmI5zDD" +
                "elA+cknpwrt1GLv0LlxGRa8o60LraqTQvkfAVTZ2r6MKQDud0Y" +
                "JXAMusY+FjWMRzmGE9IIdQD5fTuvCuyhyVY/mVAf/Q63q3uddw" +
                "ju+meb3dWWbPdBXttjcnzm8e9fz1/HU5Sq2Y9IFYDgpGLG6VoV" +
                "edj5/3s8Hyrlifkdi3V9WZ4A6XfaYxH7myhziG9VqN3chvlPUN" +
                "rauRG9offVERy0HBiMVrfWpxm7US6xjpM8IxU6o6E9y7L/tMYz" +
                "5yZQ9xDOs17K899Vr5bTocpQ4j0gvt0aIiloOCEYuLPre4zVrx" +
                "Osb6jHDMlKrOBHfnqz7TmI9c2UMcw3oNe3G5uFw+fV6Wun4Wva" +
                "z94puCWo4ZNbvvCc/qadsiHNc8DbsYKJYRe4I6O0Musa51ynlY" +
                "xbQLy5/2HF7cPEPfZ26e+HN4vabZpeySf28WkEPfUF1K80SvlT" +
                "d7v53jvdmlWd+bteZS13ElW4lirMzgeCXNS+md4PvHlfaZ5q/N" +
                "ufycHKVWLJTsSnYlP1e9f5xwAoKCEcwKnNLLFUGhF7B4NljeFe" +
                "szMlryvr2qzkSOITZnKu3+PZt7qC0zlR871rnT7o+9W+3eH2fR" +
                "O677Y1Psdu6P+dP509HZUGFh7z6QUe0rV9q2l+LEynaurVMsqC" +
                "ov5rIH7Oys/ne9Bx5tjs5up2N1xIV8oawXtK5GFrTf3VPEclAw" +
                "YvFan1rcZq2E00ifEY6ZUtWZ4Eousa7lI1f2EMewXsNebBab5T" +
                "PBptTV88GmljL2hxiVMnoeo6hlnt8F377N6oix/RYrTJ5QNm0M" +
                "jh9zmMGuEI1zsbpwCodW00cDZvnFraK8b4RjUd8/pC397i8wqp" +
                "hvhVrm+Z0VtW0RVphkQtF8fM9hBmsiGudida1TzgMRbTRgzE/c" +
                "N383eYf8uXafPUZfPsXfAdw+TvXijeINOYZaEe13f4lRxXwr1D" +
                "LP76yobYuwgvVkRzi+5zCDNRGNc7G61inngYg2GjDLzx5lj6Ln" +
                "0AoTPHvU+1jawDDm+cyJlYOWjgk3xbKz9agl1oGjOLLV7n3sUV" +
                "FUrYZn8keHYRp56u+aP3N2vl835dLO82N2kB3IUepqhQ+0H9q9" +
                "jwQHBwUjFq8/J2pJu/eRnw0WfcaRPiOi0+wJM+EkzOFMpQ0tjc" +
                "1R4/zYcT22n+2X9b7W1ci+9rufVcRyUEJv68DjtT61uM1aiXXc" +
                "t478HKg3ecJMcCWXWNfykSt7iGNYr2EvrhXXyvvkNamr++Y1LW" +
                "XslzCqmG+FWub5nRW1bRFWmNzpKZqP7znMYE1E41ysrnXKeSCi" +
                "jQbM8ov1Yr2s16Wu8HUtZewXMSpl9BWMopZ5fhd88AyrI8ZggR" +
                "UmmazbGBw/5jCDXSEa52J14RQOraaPBoz5Yds4H4oqhZb0us8z" +
                "Zo/AUXs0Vt+Y4VeAyp42B5rqSV0Bty3NxWtbjLPnLG2Z4mo5FP" +
                "RkZ4awcASO2qOxutdNruPyNDWvqZ7UFXDbKtexm9K2WHXmdKFj" +
                "Hdgyaybm+8xiy99nXj697zPD5eNULx4WD+WIWkuZ94pH0bOzoG" +
                "P34iFioG0Rq8CevDYQz2FGrK1O7ryb8sze1WEqT++C+cVesVfW" +
                "e7bWUrEcip6dJW1tWUQ0tD16VVVjBbOOFM3H9xxmxNrqZLic8s" +
                "zekXGcZ2plwN9Y21grr/U1qavrfk2L9HrvAwHXzlKm31mxeg5/" +
                "X7kphcn9iqLZvvKaPHlNRNPYsa5o2egclR1ZzPI3VjdWy3pV6g" +
                "pf1SI9i9geZlmm3Vmxyv+BclMKk0womo/vOcxgTUTT2LGudcp5" +
                "2FXgojzLT3ybaPydUGrE/7+P7Mi/Msp2273np/VGXzvWf418L3" +
                "tPjlIrJn0gloMSeuX3QodD2epZ1GvpmHUFR35OyrdX1Zngjq77" +
                "TGM+cmUPcQy7gtY5jdyffIYbTSNTPpn7R/4s77d8biT1mn7X3N" +
                "o7qBP8nf2od3rvze7+7Fjfm42zsRylrj7PsfaBWA5K6JXXtcPr" +
                "s8JoaGuU+9k8Y3JGja0jP0cZzZ4wE9xR5jON+ciVPcQxzNk/ts" +
                "7tdrTz8UhX3Aen+LuUmWKPivnU84v5RW0xJn0cgfHY1kETJ4+u" +
                "7ezHytEYKZadrUct09xjj3MKsT3aHN1GaF4xGzm/ml8t66taVy" +
                "NXtQ/EclAwYvFan1rSHn3dzwaLvEX6jHDMmIGZ4A6XfaYxH7my" +
                "hziG9VqNLebRPVswwfPF3ljasvfGguJoe+D0xrFyb9wbI4JwU/" +
                "HhAdpaVIlZiB6jiO5RUeyN2SPUvUIaqyP/B5EPsKk=");
            
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
            final int rows = 59;
            final int cols = 82;
            final int compressedBytes = 2349;
            final int uncompressedBytes = 19353;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrlWkuIJFUWjfXoIOqiF6PlQAvaM4NSQy9cZlZU4kKxdNE0zD" +
                "TMuBBBe9ppQRSZRs3sT6G5tkHoTelymMWs5+tnsOwSwR/ib+xN" +
                "24KLKt1UIQwTL27eOPee9+JVVJlJVWsGed9755177rnRmZURQZ" +
                "dr5VpRlGvlZCwKXelcIzg4sGPxooCKn42XOBt8+4r1PeJrxgxk" +
                "gnvuIHca89Gr9xDXsF7rvdVytRpXdax3VnU9+JMiloMDOxaf6E" +
                "/m46Mx6rUS5zHS94ivmVLVTHCll1jX8tGr9xDXsF7rvfVyvRrX" +
                "dax31ifHfHVMkAadb3abDKshnGo1Lyj0aizKBst5i/Q94mvGDG" +
                "Qa7jx3GvMn7Hmrmehv3Xut9zbKjWrc0LHe2dA1EMvBgR2LT/Td" +
                "TObj33A2WM5bpO8RXzNmIBPc6ntNncZ89Oo9xDWs1/AerA2q73" +
                "eIGPUoiv77jGJls2SuM4uEl/JkbhGrgJevxvWZ4xmxtjqxvVhd" +
                "610dpvpkF54/uDi4WI0X7ahHzSIUK5slc52NjwERDTu3iFUw59" +
                "FV4/rM8YxYW50sv5fy7L2j47jP1JkBv3+4f7j6tzqsY9iRlc4X" +
                "vgQCrrLxZh1VAFoUC1+yAlh4+Wp2rdygk/LkPaCHMNocqyu49u" +
                "47YnV/Zjx/eOvw4PDG4a8EHd5UFM890XR0iHs8+ykjw58Vidfw" +
                "zgj5eTG11/BAdvcXKTTuZcK+2a7O/bZCfrlDN79O4ziP498V+/" +
                "J15v7s7n0pdPnPs/WU+zyOf98h/yr5PL7wSZfPY43s6vOYPY8P" +
                "/tjO49l/T+97PTpjVwuXp9f/tLR2o9OWM83+2s5j/1jxg3nNtp" +
                "f+fH9eooyKyZoRcJXdnx9dUdTrqALQ6u/Eo6wAlvfEStaT53iG" +
                "9YAewjh+JK0L76rsq/pafGbAT/yy3Terf7Xx8b37PJ47OEv13k" +
                "ZvQ6KMioWjPFAe6G08fysQcJWNN+vIDHr1vegBVgDLe2IlIMzx" +
                "DOtB+TKG2ind4Yrt3XfE6taF55db5VZVY0vHutstXQOxHBxhNb" +
                "rC+OT+3c1kPj7B2WC5e/8t64hzoN7mCZngjv/AncZ89Oo9xDWs" +
                "13pvs9ysxk0d651NXQOxHBxhVZ1Hwif6bibz8R85GyznbdM64h" +
                "yot3lCJrjjx7jTmI9evYe4hvUa3oPjg+pvVogy1nfex3XNCLjK" +
                "xpt1VAFo1ctJVgDLPKc4zjUswhzPsB7QQxjPHUzrwrsq+6q+Fp" +
                "8Z8BPXlc0V1fKRXV0DPLovr3tm6mpxbnFOooyKyRqI5eDAjsUX" +
                "TkAZenbutdKuvL5HYt+sqpngLpzgTmM+evUe4hrWq7zj+8LRSn" +
                "Od8uwP6L7wjr27v+6ksoPzOB7t3Xlsu36c1nlsf25WHioPFUW4" +
                "fjS/Toe21w2cFE/0pnLnemnnOW21hytdmV1eg6ODo5jLu/rb/E" +
                "HM0QiuZSSeBp4MGZav8zOPtbuxailVVrSuEHUWxtALnFhN2zvy" +
                "uEvJ9dyuzx/7H0f3jA9ftc/DP+7yvV5+6Pt+r/vnNcqMXJxPz1" +
                "uuMc7vDGdWF55lWdfb1c5rt9VG9rZ1XtQos9RuPM9pdceZ1YVn" +
                "Wdb1drXz2m21kd1Np74iWolnU7hS3WOtadbvWPHlyfgXnU1Tda" +
                "+0plm/Y8VX4tk0VfdGa5r1O1yZvep+r2+PfpuWdqT2t9FrZ6+d" +
                "ZF43wf6ju2d/0vD+Ucd/Rfmr1fsdwt6otO5trfj3onj2Foe8Xs" +
                "eL3Mvon4nstc6dvVm93xq9nePgPC5+NMVrldEU/7V34WuavSTU" +
                "LyxekCijYuEo58q5xQvhfsZzcGDH4lYZevWdwhxng8WuvL5HYt" +
                "+sqpnghtq+U5kPV6wu8mI97tDzc5/H7/2s6pmZPAF7Zv94cfdY" +
                "L0kMY4iytohyZNS5zfaIajAz5nodyczxrCP26bthd20dxB5T/c" +
                "W1Uh3O7PN4aiafx1P7x4t9LXwnMYwhylqR048LDq7ObbZHVIOZ" +
                "MdfrSGaOJ6zlp62aOvfdsLu2Dvzznrg662l+3GF5jcQwhihrRW" +
                "QfGOY22yM20+OZ51pN9RwP9dmndcgrRnL6XJ31ND/usPeFxDCG" +
                "KGtFZB8Y5jbbIzbT4+3nEdVzPNRnn9YhrxjJ6XN11tP8uMPeXy" +
                "WGMURZKyL7wDC32R6xmR7PnMemeo6H+uzTOuQVIzl9rs56mh93" +
                "2PtcYhhDlLUisg8Mc5vtEZvp8cx5bKrneKjPPq1DXjGS0+fqrK" +
                "f5cYe9SxLDGGLvkuK6oxw/t9kesZkez5zHpnqOh/rs0zrkFSM5" +
                "fa7OeprPHWafh3/Y4X7vanke/mGX5+E1ssvn4Qv/kxjGEGWtiO" +
                "wDw9xme8Rmejxz3dNUz/FQn31ah7xiJKfP1VlP81Md1r/gSxrL" +
                "pRzm57zyGO+VHZ4WlUvhyDOUh4z2mtt5iqu3nReu2qJwRGN5xG" +
                "N+5ue88hjvlR3+n8vpn5ZH8jx16t211dzOE7N4//S1Prtdp/df" +
                "iWEMUdaKyD4wzG22R2ymxzO/M031HA/12ad1yCtGcvpcnfU0nz" +
                "vM/s7ctj9/Z0Zf7+J35rZZ/870n5IYxhBlrYjsA8PcZnvEZno8" +
                "8xyhqZ7joT77tA55xUhOn6uznubHHZYLEsMYoqwVkX3FTl+Puc" +
                "12f1FusJl5rt+T6nDQxrLu4Nx3wxUtktPn6qyn+XGHva8khjFE" +
                "WSsi+8Awt9kesZkez/x9bKrneKjPPq1DXjGS0+fqrKf5cYf9Jy" +
                "WGMURZKyL7wDC32R6xmR7PfK+b6jke6rNP65BXjOT0uTrraX7c" +
                "YXm3xDCGKGtFZF8x8NMrxUbfpvZSXLsn1eGgjWXdwbnvhitaJK" +
                "dfef8m9hzX8rjh36NRZtWv4rsxBmZ65THeS3Hj3HDkGcpDRnvN" +
                "7TzF1dvOC1dtUXhAo8yAjS5bzM955THeS3Hj3HDkGcpDRnvN7T" +
                "zF1dvOC1eNcu+SGMYQZa2I7APD3GZ7xGbmuX5PMuGgjWXdwbnv" +
                "hiu2d5D2zmeHa8Ud9j6TGMYQZa2I7APD3GZ7xGZ6PPN73VTP8V" +
                "CffVqHvGIkp8/VWU/z4w57VySGMURZKyL7wDC32R6xmR7PnMem" +
                "eo6H+uzTOuQVIzl9rs56mk8d/h/cuIIO");
            
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
            final int rows = 59;
            final int cols = 82;
            final int compressedBytes = 1490;
            final int uncompressedBytes = 19353;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtWztsHFUUXQlooaBACiAj4cpBQsIrVzTemVEaJCKRBuFdQM" +
                "KCDgmFNnJmR9aAojR8urhIAUUkUiAaCB8FjPgF6oARUBlR0iJZ" +
                "zJu71/f3ZjJez+zsOjOjfZ9zzz33vrdvZ97O2r0eHsFzWEKLMN" +
                "mSbd2TmLb5uNbXneUM5EE/uY9nXeR7JztF90fTdqsTPA2lq10J" +
                "fUTAThi1ubdEuGc5V9rAkzIoYvHsKHM5Gh2xeAT+3PXs6Fj+Ef" +
                "Lj4pu9Ixzjv6tzL73ba+249E7TEQb/QulqV0IfkeQNwImLbe4t" +
                "EdTQTMuVOuBZxgNWepGrYeZyNDq7ohHEV/2569nRsSSepjbX+H" +
                "xj6/H9Ftfje83qx8vx4/GD8ROT3iP8cx3+qtnpZeP/sFf1SYM8" +
                "VmPOD5VaV3yoHcuE/SjvJTsZcvqI2Tw16+tjcLO99dh07NL1uF" +
                "fBf1HW415b6zFYDVZ7vbeXxbu6WuGdX/Xx4jHo1XGM/5piPRbE" +
                "lveZqmMsPtb/xBJaPqttl2lVxzWrCo+zeNZ3il2uXRSbvP060Z" +
                "XoCpRQI+bOYCVYQQRRh9BJFvJynOw9XQGU9BxmvYmls5L6ErF5" +
                "a1X0JC6MxepyPrA50zc+mTH3Pc59xrPHe703h0ezWUU70Q6UUC" +
                "M26Z9FhHPoJAvHuTLp8bbU8mcl9SVi89aq6Mm4Z/VILZ/GKnOw" +
                "MXiu5Ftwn+kHfXMN7le4ovf9PJ/eDPc9/fqZlfc9t+dz37P98R" +
                "T7nttV9j05UtO+Z/uz3gk80o8qsa5Ppx6eCc9ACTVi0CeEc+gk" +
                "C8dJeX2L9Ajl3mTRWUl9jjhVnbdWRU8a4/qWHqnl01hlDjYGzz" +
                "W37Yf7Wb2PdW7Zxz4hnEMnWTg+0c9a6xdIj1DuTRaRm9HniFPV" +
                "eWtV9ES1bB4v6JFaPo1V5mBj8FzdK1qL1rL7zRrU+f1nDc/M/h" +
                "tZEdMtV4OffklFbHNEKhzeAUU0HV9zJENqUjQ5Fq7LM5XjoIg8" +
                "GmGSX7Z/DH8/OdfHZscSboQbUEKNGPSjc4hwDp1k4ThXJj3ell" +
                "r+rKS+RGzeWhU9iQtjsbqcT2OVOdgYPNfcNgpHWT3COreMsE8I" +
                "59BJFo5P9EVLtqWWZx6NvkRkTJ8qevJM9Egtn1gyBxuD55rbhu" +
                "Ewq4dY55Yh9gnhHDrJwvGJvmjJttTyzKPRl4iM6VNFT56JHqnl" +
                "E0vmYGPwXOEVHZhvZQcOA5xKwqBOP+U9adMtXwTp5c9B8iXmY0" +
                "G9tVQcWWZqFSt9lz7QveggeTV5QfOSl5u4Eiev1Ki1MW93MTeP" +
                "yYvJ83XOYzJsdx6Tl46hPJqfeYyWiubRb5nNPFaLPW2G0anoFJ" +
                "RQIwZ9jRCXe3BEohRDtqWWL6cifZuRLwOpzWurS3OA0WVUv7rl" +
                "lz3vSW+dnOc96c+zfN4zvln+PDx+9ki/R90Yf33o+cAE+9bD+y" +
                "IvvzL499nrF4XtZlrPFEb83N2vBfJNXv5omF96vH+qPLLvstcP" +
                "41vVnj9WejcW5HfXQnZjzx+P+/vM3XoMPoTS1a6EPkeQAzW2ub" +
                "dEUEOySdnnSdE1z8fiajZD3dMI19dZ+LO0sSTefa6b+3uK9I95" +
                "/wTF23OYU7ceG1qPg9dsq4arcMtadcavOL/3dvfebh7n6BnQuJ" +
                "uDWtbj+W4O6jjoeW/6zyLmv7XUyurr9j0Nfb8ObtjWIh3tZN2t" +
                "x3rW4yzncbBbyy58d5p5LIot5zH9bxHm8WR/LxzsQelqVw72EE" +
                "cEOVAPxH+iDMz/pZCGZJOyz5Oia56PxdVshrqnEa6vs/BnaWNJ" +
                "vFuPi3h97Oaxuz5281h1HpNPpl+PwWawCSXU+T52E/uEcA6dZO" +
                "H4ZDfMNDSqtdB2uJM2+hKxeWtV9CSuHoX0pug6qh0f5cn58bKe" +
                "3zp+L7TrsdYnUh+UWq9N/7l+656jr8cm79ce3sLcZ+bpe2E3j9" +
                "08zv9zimDY3jwWxVbXx/u7fU+r+/D/Ac82D50=");
            
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
            final int rows = 59;
            final int cols = 82;
            final int compressedBytes = 1385;
            final int uncompressedBytes = 19353;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtWr9vHEUU3i6FAQmloOC3E1sQDEYEUjlSzO2Jv4AKWXFBD0" +
                "VwTc63RpZCg5Q2JRLQUdHCf4AEHalRmjRI6ZC43Xfv3ve9N7u+" +
                "u6xz4TI7mpk333zzvfdGu7N7losiXre/KvK18DW6PLo0ujjamY" +
                "5eWXQfRy8n0d2AvNFjzC91zl5ZSOtVHI3vTZB3Fozmg7yPfh8b" +
                "JO/jCu/HfD7+/87Hk8/X9X4c3BnckVb6GpeR2toax4rNIC7qqO" +
                "FR1oqRRX1GYtxeVVca12fBq5Vr66KezxD5g0eDR81o2qttuLaK" +
                "2SxW5rAio4jgDMXmfFhBRowJc8HIUlnwavSN1a+MEU/nHg4eNq" +
                "Npr7bh2ipms1j9CBVNDZVQM+yj82EFGTEmzAUjS2XBq9E3Vr8y" +
                "Rixz5fXyelHUrfT1jIzU1tY4VmwGcdFni23WivsY9RlhnylVXY" +
                "mR+Ewj31gcQ/SBsera4xfcuXlLrdPXJrMX1+N9evz8k/Z4+5f8" +
                "FZN/F+bfM3kf8z4mz8d/n91T7vSTHt8z+ff1wld1tXqz+gjGm9" +
                "UV3cdqJ3EP/9Giczn1XFevV1sz5M9qt2Vt81xUbzfth2fEe6n5" +
                "hvmhg/HW6PdJO3s2q+2mfS/wWs+ZeZ/r6t3q/c7n/Va+v3o5xW" +
                "f7+PF3y6xfbtUanrWf5j3I3+FPz3fP8U6+u5a/9v/WVqzUbLS7" +
                "tObHPWseHrIw6rN8d2u3+bbVaZ398f5YWukVk7FHjKtsq15HFQ" +
                "xVPa8VYh57H4h4DjMwBsshlQUzNVPOyKvHnTH+YufjyV/rfT5+" +
                "89mT+X297vt48mt/75nRM/z7utfvxy/yHvSyj1/mPXiM75672o" +
                "qVmo12l9b8uGfNw0MWRn2W727tNt+2uk0n/57B0bL/T3F6lP/e" +
                "c15X/jtu3sdV/r3nPPZxtHueMY+/75z96THOxx8XPx9bv3u+zn" +
                "dXL19Av612/SqzWjb28lp5TVrpFZOxIcixYjOIo7Lpoc1a6ahY" +
                "n5EYt1fVlRiJzzTyjcUxRB8Yq611J8Y/+ZnM7+vVva/z7xmH5P" +
                "/v6djH02/Pdx/zc93L18DR/pG00ismY48YV9lWvY4qGKp6XisV" +
                "k1fCmJjDDIzBckhlwUzNlDPy6nFnjJ/PRxxVP/f3XJfPRStfT9" +
                "P7unxxdfdjm+9+3jPlhfJC8NhggltrmNo4SnGiMq7FPsVi/1rS" +
                "LPbuI0PP3Ld5j2vasanHjXJj0m9o38xs6NgQ5FixGcSn+mSxzV" +
                "qJSIM+I+wzpaorMRKfaeQbi2OIPjDWug5vDm8WRd3WfX2JrWOb" +
                "VcxbwlEmVlZUGxFW0Iu9ef+ewwzWNG+ci7fRv/fqvaEaqB4ODy" +
                "f9ofQNfqhFRojgyFYhEysrqo0IK8wyIW/ev+cwgzXNG+fibfTv" +
                "vXpvqAaqB8ODSX8gfYMfaJERIjiyVcjEyopqI8IKs0zIm/fvOc" +
                "xgTfPGuXgb/Xuv3huqGX/wYPCgKOpW+npGRmpraxwrNoO46LPF" +
                "NmvF8zHqM8I+U6q6EiPxmUa+sTiG6ANjreuNzRubRVG3dS+XjG" +
                "RsrWI2i9WPhId6aiPCfu3yPqwgI8Zk6hxZKgtejXFj9StjxDI3" +
                "3BvuTe7LPemb+3RPi4wQwZGtQiZWVlQbEVaYPVnkzfv3HGawpn" +
                "njXLyN/r1X7w3VkN/s6VZdZju8JZV2fUtY1hpuvUejutdNXcru" +
                "WmOaGpNGZThabdp+3rLnLLF0RLVdFxtJZYawrDXceo9Gda+b3M" +
                "ftLjWvqTFpVIaj1abt5y17zhJLKqrh/eF9aa3XIvOM2ghXmQ5W" +
                "UTCe6nl/imJMXhtjYg4zorZFkoqZYzdWzDO1M8D/D9KNc9w=");
            
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
            final int rows = 59;
            final int cols = 82;
            final int compressedBytes = 1671;
            final int uncompressedBytes = 19353;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtWz1vXEUUfVJaOkQoAAUBFSAhpQAsKGzv7q/gN8QFUuQ662" +
                "jRpnUHFgtBFNAgG6WBAqWNEupIIPNR0BK5jITEvnf3+pxz7+zi" +
                "3WwsE817ejN3zpw599zxvt3ndbK5sbnRNJsbm13fHhb7GLOOxc" +
                "g4zuRLFT1mRBX80Gwxf+QoQzWRTWuJMeePWWM2VmN+PoZ/eTT+" +
                "tKnHmY7end4da613zMZAmIMTM4xDmfUYjVo+x65UX5HsO6r6Sn" +
                "BjFboa2WPWXB98Mr+wt4dzd/3wDD+Zw6V/lodrfm0cXozX6PiL" +
                "0+jLesc+wT7ePo2+qrtxxnvgXu+etdY7ZmMgzMGJGcahzHqMql" +
                "bZleorkn1HVV8JbqxCVzsX67JerJD5vZPeybQ/8b6bOfExEObg" +
                "xAzjM32JNFatwj4mfUU0Z0nVV7KTWGnmg6Uecg722l6DB4MHTd" +
                "O26P1sWRHFiFdZ7BEjpsExI6yAQ7PF/JGjjKwNJyXP6h2sXGdp" +
                "Z8Dfurp1tWna1vp2xkYeMwKus3FFHVcA6npRK74aNRuPwVUsx6" +
                "zNfdaFd3aYXUYXkT98Y/j68Pnh27Nn8Jeb5sb1Zd5hhy8V0XcS" +
                "8ur63tWHLy6cfXMprVd4NLo7Rd5a0k23o8vt4+jXZ3sfP/lotX" +
                "0cflDA31uptg9X3pV3V175/oV+fvyhPg+u4xi9UPdgLa/Hn+oe" +
                "rGUf79Y9WPr3wse9x9Za75iNgTAHJ2YYZ2XocaxaZVeqr0j2HV" +
                "V9JTuJlWY+WOoh52CvWBveH1+rr69lj8G1wTVrrXfMxhEB19m4" +
                "oo4rAHW9qFXyFJXYk3KUwR5QQ6kKZXqlWlFUzztzyt0Z7Ez7He" +
                "+7mR0fRwRcZ+OKOq4A1PWiVtrHnZiDkchRBntADaUqlOmVakVR" +
                "Pe+Mc/tX+leapm2tb2ds5LG34ODEDOOmr5HGqpXvkqyviOYsqf" +
                "pKdhIrzXyw1EPOwV67ueP+8bQ/9r6bOfYxEObgxAzjM32JNFat" +
                "wj4mfUU0Z0nVV7KTWGnmg6Uecg722s3t9nen/a733cyuj4EwBy" +
                "dmGJ/pS6SxahX2MekrojlLqr6SncRKMx8s9ZBzsNdubr+/P+33" +
                "ve9m9n0MhDk4McP4TF8ijVWrsI9JXxHNWVL1lewkVpr5YKmHnI" +
                "O9Yq0eN/6pzzFLP/ccDY6sbXtgPkbrGGb5iiNXhp7HjGhedcU5" +
                "cJZ967y6gxNdoas5N1/aRhfgb1/evtw0bWt9O2MjjxkB19m4oo" +
                "4rAHW9qBV3UbPxGFzFcsza3GddeGeH2WV0ofz+pD+Z3t8T77s7" +
                "fuJjIMzBiRnGZ+8bEmmsWoX3x6SviOYsqfpKdhIrzXyw1EPOwV" +
                "67uYP+wbQ/8L6bOfAxEObgxAzjM32JNFatwj4mfUU0Z0nVV7KT" +
                "WGnmg6Uecg72irXh+56fn8Y78XBvfVp7Dy/eJ039OxePbn5+Pn" +
                "/nqvs4bx8Lz4/Xm3qs8gz5mbVt37Y2ZsQ51nvMqxVxjcjMXNWx" +
                "lYt47Cj61Gqiu3kVZI+l+nKuWGG9rxOy4n3de87atm9bGzty82" +
                "PDwfWYVyviGpGZuapjKxfxjDV+yGruXKuJ7uZVMLxd9q7rcq5Y" +
                "YX09rutzZvtra9u+bW3siM0DQ8yrFeGVis9fieyRl1mq5s61mp" +
                "hjXgXZY8llzlWqMDyH/1I/e9fxHL7c7zP1c+b0c+aStW3ftjZ2" +
                "ZPo5c8k51nvMqxVxjcjMXNWxlYt4xhofs5o712qiu3kVhM+ZlD" +
                "3q+fpY4Xl+zox/f6Y/Zx5Z2/Zta2NHxn8aDq7HvFoR14jMzFUd" +
                "W7mIZ6y9P1jNnWs10d28CvT1mLNHPV9fqvA8fi9c5/c9F+/Yvr" +
                "9931rrHbMxEObgxAzjrAw9jlWr7Er1Fcm+o6qvZCex0swHSz3k" +
                "HOwVa+v3FOs7tn7z1qLSbI4XaZ0dj6yz8JjFrv8r92Ltebmxuq" +
                "wzmAwm1lrvmI2BMAcnZhhnZehxrFqFb3WSviLZd1T1lewkVpr5" +
                "YKmHnIO9Ym29r5/0GH275P/7+Kb+PlN6fhw/Wm4fx3/XfZz3HJ" +
                "7u6wX/LmX0Xb2HS0f/qH9krfWO2RgIc3BihnFWhh7HqlV2pfqK" +
                "ZN9R1Veyk1hp5oOlHnIO9mrX//F73NH3F+++rvv4tN4fRz/W97" +
                "u1/LTrPi59DG4NbllrvWM2jgi4zsYVdVwBqOtFrZKnqMSelKMM" +
                "9oAaSlUo0yvViqJ63plT/r8h7l/H");
            
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
            final int rows = 59;
            final int cols = 82;
            final int compressedBytes = 1615;
            final int uncompressedBytes = 19353;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtXE1vG1UUHQoSHxIVonxICV9qYNECQUJdRGoWMLaBim03iA" +
                "aKaFUqwSYiP2Asy6m8yqrdsWPDLkKsuuKXdN8VEiA2iOCZ65tz" +
                "z73PTsYZJzTMWPM+zjvv3HNv3jh2KjVfzVezLF/NJ32W6UzHFg" +
                "FX2bi9jioAVT2vlbmLo9k5uIzFsdW2fdSFd+swuvQumN/d6m5l" +
                "WdlKX67ITMfagoMXViwu+jziMWtl4Yr6jHDMlKrutE58ppEPFn" +
                "uIMazX8u7d693LsrKVvlyRmY4tAq6ycXsdVQCqel7LV5Gj2Tm4" +
                "jMWx1bZ91IV36zC69C6Yny/lS+NzuaR9dVKXdO4RcJWN2+uoAl" +
                "DV81rhuV7yMSziOcywHpBDKgtmaqackVePlVFu52HnYZaVrfTl" +
                "isx0rC04eGHF4qLPIx6zVnyuoz4jHDOlqjutE59p5IPFHmIM61" +
                "Xu3l54rvZKTHC0wHgtPeNRKgLvSntgPmMp1qzI3at+NaV4mIv3" +
                "aF2KJzyv2CrOjttz1fiFqn1pfC9n2ejpCeOtsh09OZldrNp3ZD" +
                "Z6al9nbZqT4qPik+LjavTpFMZG8YWZfV18U9wuHivOONazxXNm" +
                "tv/UF6/PqsPomQlrvfigyOvXsbhGsxvj+2aW9d/sr/TP9SdV6L" +
                "9S1nGGh7Me6S+neP33AvJGln34T9bI1X955urFFDotdv/VgLxd" +
                "0837VVurjgmVGnVs6pqnjlPZVMfR88dTx9GLp7uO7Xlspo7DX+" +
                "etY7zyd+OozjXfrkVfi3WVX8gvSCu9YjL3CLjKxu11VAGo6nmt" +
                "lCevZD0xhxnWA3JIZcFMzZQz8uqxMsod3Bp8Pvhy8JnNY/DV0X" +
                "42vfMpdHBj2kr9a3Bt5ur1w7uaj5XYt9xbllZ6xWTuEXDtDosw" +
                "ihg8Zq2Up2n60VHKAWvbPuqiBhqdo6bVPT9fy8eflstW+uqkru" +
                "ncI+AqG7fXUQWgque1wnO95mNYxHOYYT0gh1QWzNRMOSOvHiuj" +
                "3O5ud3f8nWlX++ob1K7OgVgOXlix+OR7GI14zFqJ73BBnxGOmV" +
                "LVndaJzzTywWIPMYb1Kvej+Lln+POj/vnxzq3T/fnxzs356rj9" +
                "+PB3qePwt+1JHYd/Hj3T7TPhDP3V4Hn8Y+bqnN/ih39P+r1mXO" +
                "a346hJ1ZPRajJ+4tycwvM4eu34z2Nbx8U918PvFnHyR+ezY7qG" +
                "357QN/ifpC37spW5RZQjvY7tbkZUg9lQTu1EdM9LsaxadOhnHr" +
                "H63kXaZYzFePL3+PdZe9W8Onc7d6WVXjGZA7EcvLBicShbPYt6" +
                "LV2zrlifkejbq+pOcH0WvBvRfdSYH3xafrzqfZ9pr6ln9Mf6K/" +
                "U46R2H23kwq76DBn7HXMovSSu9YjL3CLjKxu11VAGo6nmtlCev" +
                "ZD0xhxnWA3JIZcFMzZQz8uqxMuC3/z7TxN8p8o18Q1rpqwpv6N" +
                "wj4Cobt9dRBaCq57XCedzwMSziOcywHpBDKgtmaqackVePlVFu" +
                "Z7OzOX5H2dS+en/Z1DkQy8ELKxafvEsZDY96LV3bf4cL+oxwzJ" +
                "Sq7gTXZ8G7Ed1HjfnBp+W3z/VJ/P3xtNdx8ENbx6PWcfDL/Oex" +
                "s9PZkVb66onf0TkQy8ELKxafvG8YDY+yVuITYNBnJPr2qroTXJ" +
                "8F71Yu9kU9nyHz2/PYvj/+X+vYuX5ydZwWuz2P/63z2P69p6Gf" +
                "bXse2+e6fa7b57o9j+15PNarrWP9q3u5e1la6RWTORDLwQsrFr" +
                "fK0LNj1kq7Yn1Gom+vqjutE59p5IPFHmIM67VaW++uj/t17auV" +
                "dZ0DsRy8sGLxiT6NeMxaiToGfUY4ZkpVd1onPtPIB4s9xBjWq9" +
                "zt75lFfX4cXW3rWPvvjw86D8I3+goTHC0wXgPf7ksr2wh2T5ph" +
                "I+kL+hwjHZm1PTo9etwzHfOezMr9qar3D/7pWM5h+M1fB0VdhK" +
                "t8JV+RVnrFZO4RcJWN2+uoAlDV81opT17JemIOM6wH5JDKgpma" +
                "KWfk1WNlwG8/PzZyHq/kV6SVXjGZewRcZeP2OqoAVPW8VsqTV7" +
                "KemMMM6wE5pLJgpmbKGXn1WBnwj/P/Azi9v697/V5fWulLXGY6" +
                "tgi4ysbtdVQBqOp5Le+Lo9k5uIzFsdW2fdSFd+swuvQuHP9frX" +
                "OIFQ==");
            
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
            final int rows = 59;
            final int cols = 82;
            final int compressedBytes = 1772;
            final int uncompressedBytes = 19353;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtWr9rLGUUXYz/gYWFCoKC8AwIVoIS3HU25StC+sc+jNikCd" +
                "rEyo0mkDa9CHaClb2gzz7/gMEmpgiv8KFiI+ru3Dk599z7zWZ3" +
                "2Txewszw/Trf+c4999uZ2R9Jr5ePzz/tdceCx/GDjB09fLoejk" +
                "a3fx+H/WHfamuB2Tgi5ILNEnWgQBR6UavkKSp5T8pRhvfAHEpZ" +
                "KBOZakZRPe8M+YVr9GF3ny56jN8t7ONHV72PF1B6b+lny4dLu3" +
                "/nWd7b7vm4xPNxc7hptbXAbBwRcsFmiTpQIAq9qFXyFJW8J+Uo" +
                "w3tgDqUslIlMNaOonneG/PHr49fGL4zXmzvl5UU/94xfKqJvJe" +
                "TV1b36h9/PdHRvIf+vJOTNBZ8ub/d61ZPqidXWTnEboY+aHJ6c" +
                "8bipa0/7qpWdZX1Fsu+oipXeScw088lSDzmG9zotw9Ph6eS6PE" +
                "VbX6mnGBPxHM/2q/zIeOyx7xE/I3d2iMHTM7InqquzUha62sf2" +
                "Ja7Mjm1uc21zrdeb1tZOZ2yEPmpyeHLG46avPe2rVt7HrK+Ixi" +
                "ypYqV3EjPNfLLUQ47hvU7L8GR4MtnPE7T1Dp9gHBFywWaJOlAg" +
                "Cr2ola7GkxjDI5GjDO+BOZSyUCYy1Yyiet4ZcAcPBpNvhtPa2u" +
                "mMjdD3CLlgs0QdKBCFXtSK+6jR/JhcxXLfa/s269K7d5hdRhfK" +
                "ry6qi8lz8gJt/eS8wJiI5/DkjMeb56/0tK9ahfeZpK+IxiypYq" +
                "V3EjPNfLLUQ47hvVq5jZ97xi/e1OeeL75a8nPPeXVutbX1Dp9j" +
                "TMRzeHLG483rJD3tq1bhekz6imTfURUrvZOYaeaTpR5yDO+1nl" +
                "ur0vulYYazJoa+H5U41Vrp1cNa35ZYGh9nmaXRozMfWdu26HlN" +
                "O9ZE3K/2J+0+2npmH2MinsOTMx5v9KWnfdUqOE36imjMkipWei" +
                "cx08wnSz3kGN7rtAy2BluT95sttPU70BbGESEXbJaoAwWi0Ita" +
                "6f16K8bwSOQow3tgDqUslIlMNaOonneG/MJz85MrtyfLvAsst+" +
                "p2H4P7g/tWWwvMxhEhF2yWqAMFotCLWiVPUcl7Uo4yvAfmUMpC" +
                "mchUM4rqeWeuuKPBaNKO0NYzI4wjQi7YLFEHCkShF7XSPo5iDI" +
                "9EjjK8B+ZQykKZyFQziup5Z66424PtSbuNtp7Zxjgi5ILNEnWg" +
                "QBR6USvt43aM4ZHIUYb3wBxKWSgTmWpGUT3vDPk4+o9RW4+Y9r" +
                "RfOvqP29i6sk1H15cZyvKu21Xncd8Wm6uvjfMbauuVZnN/ltb8" +
                "eGTNw/Ms7/q62LO122JzdVnn+LPue+Eqfg93O36Bul/45cBjpf" +
                "l29jx4ZM3D8yzv+rrYs7XbYnN1m85i1+PhL3f7ejz8cbnr8Wjt" +
                "8A/bx8Pfj5p9PPxrBX9LfS69An+v8O9cf86c/XdJ1X+a9r/F1/" +
                "bH/bHV1gKzcUTIBZsl6kCBKPSiVslTVPKelKMM74E5lLJQJjLV" +
                "jKJ63hny83HwfK87VvH/Db92e9Dt47Nz3Mz/kY4P7vTvPZeDS6" +
                "utBWZjIp7DkzMe98rU833VKrtSfUWy76iKld5JzDTzyVIPOYb3" +
                "Ws+dDc4m7RnaeuYMYyKew5MzHm/0pad91SrsY9JXRGOWVLHSO4" +
                "mZZj5Z6iHH8F659mnc13f7qC6rS6utBWZjIp7DkzMe98rU833V" +
                "KrtSfUWy76iKld5JzDTzyVIPOYb3aqX7nWIVv1MMNgYbVltb3/" +
                "EbGEeEXLBZog4UiEIvaqWn40aM4ZHIUYb3wBxKWSgTmWpGUT3v" +
                "DLjVTrUzuS530NZX6g7GRDyHJ2c83lzv0tO+ahXu66SviMYsqW" +
                "KldxIzzXyy1EOO4b1a6e7rVdzX1W61a7W19Q7vYkzEc3hyxuPN" +
                "6yQ97atW4XpM+opk31EVK72TmGnmk6UecgzvtZ7bq/Ym7R7aem" +
                "YPYyKew5MzHm/0pad91SrsY9JXRGOWVLHSO4mZZj5Z6iHH8F6n" +
                "ZTgajnq9aT1s/o5ofYw5Cyz2jAOmL6qIvkdUAYdGi/EjRxmqyW" +
                "iaS+z7+DFqjObVPL9wv1/9Yn78dfcJe5Hj/TemJ0dWlGEs1sTZ" +
                "RjSrR902N+1qUROe4Iq477Vpx3lmr1n6c95MPvh28ZnFOOUV86" +
                "28nrW4g5s53H39TXevPmu/P3b72B1z/N6zXq2jp5iNWRPTuXYO" +
                "FTWizfm23RdqnLPcs+Scst9Z0fOaUkzJ517VfIuqrr5NGWZj1s" +
                "R0rp1TFb+fGQcx2lkaH2eZpdFLOWW/s6LnNaWYGjkfB1929+kq" +
                "juPvuj1Y4lNO93vPCn7v6fZx1f+35+7rH7q7dCXPx0d3Mquf5m" +
                "L9vKT8/xlOLXI=");
            
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
            final int rows = 59;
            final int cols = 82;
            final int compressedBytes = 1443;
            final int uncompressedBytes = 19353;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtW01rJFUUbRezH3DhQgVBVyrIZJlV6O4iv0A3LoNIyCJZ6A" +
                "+wx0k0kr/gbwiYnbiclSCzFAnOCBFmK7MUwa66dT3n3PuqnQ6d" +
                "9KR5VdT7OO/cc897qa6vYZrD5nA0ag6bvh6NvOdtRsB1No6o4w" +
                "pAXS9qjcKm2bgPrmK5zdpcZ114Z4fZZXQRZnTRXMzrC6v7kQv0" +
                "UTqGUT5iz3is521GNC+tZMiBnRnZE9TVWWkWGs25+dAyumB+3r" +
                "76clS3FWynP9c1WH6bvTd7d/b67MO+99bi8/Gbz1P8m0XVjxLy" +
                "zgo9v7Fw9P2ltN6W+X02Rz5Y0s1WXcd1rWNBZaPW8evvr7uO9T" +
                "6zjutjPR+Hz8edP61s67a0viM2DgxtjlaEIxUfdoPsi3jIH32y" +
                "w9iLyCL9mD3qeXyc4W2ej6e/bvL5OH5hZVu3pfUdsXFgaHO0Ih" +
                "yp+LAbZF/EQ/7okx3GXkQW6cfsUc/j8wwnr1nZ1m1pfUdOfzMc" +
                "XG9ztCKuEZmZqzoWuYhnrId/sJo719lEd8MzKHuPqxNzlWZY79" +
                "er2MZ/WdnWbWl9R2wcGNocrQhHKr7YhUUu4iF/9MkOYy8ii/Rj" +
                "9qjn8XGGy91nTn9f/3PP+PF17jPjxy/1PvP37bzPnD6rz4/rfZ" +
                "+ZPdzka2PztHlqpdWOWT8i4HIEI4oih7ZVq+RpSD87KjlQba6z" +
                "LtbAs2vWsnrm59/1t7+86u+Fxz/c1O+6Q1Z0fazruJrr4/GP9V" +
                "lw6evjSXNipdWOWT8i4DobR9RxBaCuF7VKnqISe1KOMtgD5lCa" +
                "hTJ9pjqjqJ5XxrnT59Pno1FbWt2OWM/bXoKDHSOMm762tK1a+a" +
                "+b9RXRnCVVj2QncaaZD5Z6yDnYa3tMLieX6R2zwwxHCUzHwOc4" +
                "RwfeYi81pszgTL5DX3OUM6t2RIez55hhzDJPz6fn8/U897pb4X" +
                "PvA2EOdoww3v+dpKVt1Sqcj0lfEc1ZUvVIdhJnmvlgqYecg70i" +
                "tn6nqP+u8Co99+xstTt6dsjX9S1joQSurBIK9Z2t//fl7EUx0H" +
                "RP7go4t4a04zhmr7Pk/WVnMvui/kqX3ab3pvfKmOEogXmbeyVO" +
                "VuZYrod9een7kHvOHp1xZq2HsueYYYznHO7rP+XWXdrW47reZ2" +
                "7qO0Vdx7qO61rHyf5k30qru+vLvveBMAc7Rhjvr1KkEdGo5WP/" +
                "XeGSviLZd1T1SHDjLDQa2WPWPD/4JNWzydm8PvO6GznzPhDmYM" +
                "cI470+aURUtQp3iqSviOYsqXokuHEWGu1cxGW9OEPl3+bverK3" +
                "vt/1UO6b+45b36/rc0+9X9/tdWyumisrre6+lF95HwhzsGOE8f" +
                "57u7S0rVqFf+1I+opk31HVI9lJnGnmg6Uecg72ith6fazXx3p9" +
                "3KR1nG5Pt620uvsStO19IMzBjhHG++9J0tK2ahW+TCV9RbLvqO" +
                "qR7CTONPPBUg85B3vtxvam8+fTtrS6G9nzPhDmYMcI472+tLSt" +
                "WoV1TPqKaM6SqkeykzjTzAdLPeQc7LUbO5gezOsDr7uRA+8DYQ" +
                "52jDDe60tL26pVWMekr4jmLKl6JDuJM818sNRDzsFeu7Gj6dG8" +
                "PvK6GznyPhDmYMcI472+tLStWoV1TPqKaM6SqkeykzjTzAdLPe" +
                "Qc7LU9mt1md/78s+t190S06/2IgOtsHFHHFYC6XtRKT4+7MQcj" +
                "kaMM9oA5lGahTJ+pziiq55UBP2/fPahPg8tuzZPmiZVWO2Z9IM" +
                "xhNkdxz5Wh521GeCS64hzYh3yr5+isNAuN5tx8xMjsmOcc3mf+" +
                "qefXstv4/vi+lVY7Zv2IgOtsHFHHFYC6XtQqeYpK7Ek5ymAPmE" +
                "NpFsr0meqMonpeGfDr+Vjfr1f/Xlj/H/vdOh+PLzf7fDz59Ha+" +
                "9zz6ZLPX8dHH11zHfwGp4QLZ");
            
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
            final int rows = 59;
            final int cols = 82;
            final int compressedBytes = 1418;
            final int uncompressedBytes = 19353;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtWj1vJEUQHRl8f4CAABBIXMRxQiIAJEhudtYhZ2JEhkSCCJ" +
                "BxRICXY82xCMl/gIAIyUj+AfwVgiMwMRIGEuvAPTV1r15Vz5zH" +
                "Gstrq2e0/fH61avXfb0zO3OuqsXtxauL5xavV+2xeLGq9r6oRh" +
                "yLF7LoGwF5pZrsWDw/OPraKK2XAnJnpJs38/i4dbyK45vtwdH3" +
                "r8LTuP24/O1m78f9D8t+vMr9uP/M8i/Zj8s/97v9uPx7At2NsJ" +
                "P/nc718mRw9PEFVU+7+r/xsff27u1JKbVi0vcIuMrGx+uoAlDV" +
                "81o5T17JemIOM6wHzCE3C2bqTHlGXj2uDPjlfj3F/Xrr1tYtKa" +
                "VOuPS0rSU4ODFicVHnFrdZKzqL+oxE315VI60TP9PIB4s9xBzW" +
                "a/o095v7VZVKqdOI9LStJTg4MWJx0ecWt1krrmPUZ4Rz5lQ10j" +
                "rxM418sNhDzGG9tmPbzdm9L5VStyPb2gdiOTgxYvFOn1rcZq3M" +
                "OgZ9RjhnTlUjrRM/08gHiz3EHNZr+mxtbm2e7ctNrduduql9IJ" +
                "aDEyMW7/Y7tbjNWpnvddBnhHPmVDXSOvEzjXyw2EPMYb0i1l03" +
                "/9DW6oOqHOc6Zo9mj/KY4CiB8Rj4Ni6vbDPYmH5fWuoJfc6Rz8" +
                "zaHu3PHmP6Me/JjBz2qh6e41/mcPS/5eGYyKezxju4pD36y/iR" +
                "cZx8xPkin84a7+ByjvV/vr4eR1nH8Udzt7mrLcakjxIYj/VzoM" +
                "gZZczW/b601HPIPT5xTtHvUPYYk8tJ87nTdE+TzZOnSsGkjxIY" +
                "j/VzmuxzqnA0Rz+L8+uZZ3H23Jyi36HsMSaXkzOX94/lfXh5H3" +
                "7z9mM8Vh+V++/ob8i7GeztCym9d2EPb1048p11XtvVx2V/lXVc" +
                "o3X8pKzBJOv4aVmD0c+Fu82ulFIrJn0gloMTIxa3ytCzbdbKu2" +
                "J9RqJvr6qR1omfaeSDxR5iDusVseU9xaV8rz8ra3DZz4WZ+PJ3" +
                "ANf2uXA9j/pUylSnsj5VPLW+uy04uNq20YyohmdGLutI5BBPWF" +
                "//btXUOc/Gu+ubweKnvHe/Oj6Xn2H5Xk/zvS7rONX1cfaslKlO" +
                "pfQVkXFgaNtoRmwk4/1ukH2Ih/zep3Xoex4Z0vfZvZ7GxxnWJ1" +
                "KmOpX1ieKpdXZ9PFGO1DX9FWcd/qYTGp5ZD/z9J7IP8YTVXh+d" +
                "T+vQ9zxi9d31MWT3ehqfm2G5X09xzDakTHUqZxuK64hyuG2jGb" +
                "GRjA+7kMghHvJ7n9ah73lkSN9n93oaH2dY/yNlqlMpfUUefC44" +
                "uNq20YyohmdGLutI5BBPWKsvrZo659l4d30zcN/rkN3rabyf4b" +
                "j79bc/3Oz79YMfy/PMuh3LX8sajD3mD+cPpZRaMel7BFxl4+N1" +
                "VAGo6nmtnCevZD0xhxnWA+aQmwUzdaY8I68eV0a5zU6zU1WplL" +
                "p9M7mjfSCWgxMjFu/eb1KL26yVeY8b9BnhnDlVjbRO/EwjHyz2" +
                "EHNYr/Ipz4Xl+Xq93z/OH1/va/5XL19F1nH7cfV92Y+5/dgcNU" +
                "dSSt1eOY+0D8RycGLE4t31l1rcZq3MfSboMxJ9e1WNtE78TCMf" +
                "LPYQc1iviC2/w6c46p+lTHUqpW8R5UitbRvNiGowG8q5SGT3vB" +
                "zLqkWHvucRq+9d5F3GXIzPdme7UkqtmPSBWA5OjFgcylbPol5L" +
                "x568Ywn6jETfXlUjwfWz4Ghk91nj/Gb0v/7d2MHs4Kw+0LodOd" +
                "A+EMvBiRGLd/pGw6OslXl7FfQZ4Zw5VY0E18+Co5WLuKjnZ8j8" +
                "8vux/A5fn3WcH8+PpZS6/R1+rH0gloMTIxbvfs1Ti9uslXkSCP" +
                "qMRN9eVSOtEz/TyAeLPcQc1qt8yn6c5Hv9P5/ZfBU=");
            
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
            final int rows = 59;
            final int cols = 82;
            final int compressedBytes = 1275;
            final int uncompressedBytes = 19353;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtW01rHEcQHQQiJ58CziEOGKKTHQjkFHB08Owu6G9EOM5Fp0" +
                "B+QCZCFtr/4IPxzQiDwRDIWSf9iLDCf8AyOZlANFP7VFWvukc7" +
                "60301TNsdffrV6+qWj07o5FdVc1G83XzefNN1R3Nvar67ddqwN" +
                "F8mUS/Dcj9amVH80Xv7INBWl/Z0e7zM+ThwGy+62xZR6zh2zmy" +
                "1DrGY9g6lmO+mmU/MrLEfhw9HT0VK22Lywh9WOXoqTMWF3WrwS" +
                "hrYQ6ZRX2PxLxZFZ7K5Sq8t0bnqLE+zdPyy35cxX4s61ju11fp" +
                "fj2+O77LuGCCq1XMz+U5UdlGEG6OBRwWZ5qVi+z5jOajR588hs" +
                "hlP67m+zEeo5c5j/zMME7aYzHPi1nDM/j0Y7w2XktjgqtVDH07" +
                "SnGisvW1bT4vWJy57G10zsxG9m0uevTJYxJx+jrynm3/vz/LZz" +
                "9e/99mJpuTTbHSApMxI8oFWz+sAwVFocdaqZxYyebkOZ5hc9Aa" +
                "UlV4Jir1FbF6XBlwm0dxbQ/On9enbwZ82/6w7M/y4MnS95vvr8" +
                "x+rCe1WGmByZgR5YKtH9aBgqLQY61UTqxkc/Icz7A5aA2pKjwT" +
                "lfqKWD2ujPIT+2O7KsfQK6P3uh6kdLuv663JllhpgcmYEeWCrR" +
                "/WgYKi0GOtVE6sZHPyHM+wOWgNqSo8E5X6ilg9rgy449Px6dnz" +
                "zyna7onoFGNFLEdPnbH4/LnK9XzfayWe0IK+R3zMlCo8bSZcae" +
                "Qry+cQY9hc1dcf0z/K990qjvJ3hU85Hr+DlV5qNvb7tBbHmbUI" +
                "z7Js1hfF7tfOxVbvxXS639P+KruqXNdlHa/5k3h5j7uC97jjnf" +
                "GOWGm7J6IdjBWxHD11xuLz5yrX832vlXh+DPoeiXmzKjxtJlxp" +
                "5CvL5xBj2Fzbz+hkdBLeJ3eY4GoV83PKt35AM2+sT7xPmmEj4V" +
                "R9HyMd2Wszmo8effLYvI7ZaBZ4HSa4WsX8nPKtH9BMdjPvk2bY" +
                "SDhV38dIR/bajOajR588xjmZmcOs6uHF3xaLcNIei3lezBqeQb" +
                "nPXJX7TPI91qX8PG/esfdnWYNVHNNZWYOVrONJWYNyn7msf292" +
                "9pxwR2zbtlbGQA42BFcu+tbbI9BgZuR6HfHs4wnr95lVQ+a+Gs" +
                "4uV0HzIp2794uxYoX1P2LbtrUyBiLzimnfenvEeno8v44avY+n" +
                "8TlPmyGPGOnT5+isB/9Y4WhdbNu2VsZAzvbjOjjSom+9PQINZk" +
                "au1xHPPp6wuv1IedoMecSI1af9GKKzHvxjhfXfYtu2tTIGIvOK" +
                "ad96e8R6erxnP55H7+NpfM7TZsgjRvr0OTrrwT9WOPpMbNu2Vs" +
                "ZAdn8RXLnoW2+PQIOZket1xLOPJ6zpe6uGzH01nF2uAtqPITrr" +
                "wZ8rLPfrVdyv6+P6WKy03V49xlgRy9FTZyw+vwJcz/e9VuLqCv" +
                "oeiXmzKjxtJlxp5CvL5xBj2FzlM2w/Tj9c/n6sj5bZj/XRIvtx" +
                "/2P5f3Hl95mb8N6srON/9f6xvDcbfkz2JntipQUmY0aUC7Z+WA" +
                "cKikKPtVI5sZLNyXM8w+agNaSq8ExU6iti9bgyyh92Xe//fLOv" +
                "6/2flr+u61di27a1MrYIONKib709Ag3PVuWUp0ZnXopl1WKGPG" +
                "LE6nMW6SxjLI+X+0y5X5d1LOtY1rGsY1nHso635f1j9S/mIuaN");
            
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
            final int rows = 59;
            final int cols = 82;
            final int compressedBytes = 1686;
            final int uncompressedBytes = 19353;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtWsFqJFUULXRwKQwuXKiMoCtnQHAlKAOT6tqKG79AMaEJZJ" +
                "FkPzZjJoxD8gf+xJBsXPoF+YBZZePsBBU3Eq2qV7fOuee+qqST" +
                "lrRY9ah33zv33HPvfdOV7kynKBYfLj5YvLV4ULTX4t2i+G6/WO" +
                "JavJNFPw7I+8XKrsXbo96PltJ6z1ZPTjrk/pLVfFIU5Va5leZk" +
                "GzztbG0zOBjwMJ7UWUNR1TKfVRb1PRLrVlWLBFe78NHIrlljf6" +
                "iT+fFa7vU4Xd2rcnquFbnGcz2doz/HJz9O53ir7zO75W6ak21/" +
                "cu7aHghzMOBhvPv5SxqKqpb5+p/dQd8jsW5VtUhwtQsfjeyaNf" +
                "aHOkl1s9ys7abZ1rNpeyDMwYCH8U6fNBRVLfP1tQV9j/icOVWL" +
                "BFe78NHIrlljf6gT/Nmd2R19nSYs4ZiB2Zp3OU5U5li2OZbPby" +
                "PP8tm1Ms7s7VD2GDOMpYzly/Kl8hKWcMzAvA98jjM0f4E7xDLc" +
                "ZhvQ9znymb22osPZY8wwljLP5rN5fZ5zs+0Jz20PhDkY8DDe/T" +
                "u5lV97rcy/eND3iM+ZU7VIrkQ7jXywfA0xB9fa3FVZlUXRzMk2" +
                "nrSzNSPgGhu36pgCUNNTLT1Fn4334HosrlmbbdRF7VxhrFKr8P" +
                "yNuxt3i6KZk208aWdrRsA1Nm7VMQWgpqdaeo4+G+/B9Vhcszbb" +
                "qIvaucJYpVah/Onz42o+h9v16Beb08pfjOX8w+yr4Mq6Co9ZXP" +
                "Vluce1h3Ij+tI85zanVc4b12NaV8eVdRUes7jqy3KPaw/lRnRe" +
                "5+nrB7+l5/rg16fdc33wx82fvKevKXLw5+qe64PfR70X11T9q7" +
                "N/Lx+78WrjVZqTNSztgTAHAx7GWRl6vPZa+aq8vkdi3apqkVyJ" +
                "dhr5YPkaYg6uFbHT/z/+G9d0jqu5Hv18u/G32dV1ay/Py/M8ln" +
                "DMwLwPfI7LK3MGjhmuy2Yb0Pc58pm9tqLD2WPMMKY10fvWT9Mz" +
                "uYrrh2+mM5jOcX2u5/enM1jJOT6YzmDZq3pRvUhzY4HZHrNh8P" +
                "KtO1OGnq0Z8Xl9VZwDI1+39/vqUImP8NGcm28/axWkelqd1vY0" +
                "2c5zij1mw+DlW3eJx3q2ZsTnpdokBwYzYk1Q95XluvDRnJtvP2" +
                "sVzJ/+/zEg098B3Pgcb/L/uOWbaW5sM6e9IckPDGuO9ghHenzk" +
                "N4c++xgP+bVOrlB3iozpa3bVs3jtcHo9Ts/1Op1j/dp8I82Nbe" +
                "a0NyT5gWHN0R7hSI+PPNd99jEe8mudXKHuFBnT1+yqZ/Ha4fR6" +
                "XMX7THVYHaY52fYT0aHtFQHX2LhVxxSAmp5qhU+Ph5qDEeV4Bt" +
                "eAHnJdeKZ16jtS9XgyxA/fClUXDZZwzMC8L7/zq1wGH5WvwfM9" +
                "lmMl+/heNfpNFyqNilf6HfBCd3mV6qL4T1+P791G1unn4yrer8" +
                "t5OU9zsu170Nz2QJiDAQ/j3TsbaSiqWubr3yODvkdi3apqkeBq" +
                "Fz4a2TVr7K90f2/W+fbL+tXXzMm2nn3bA2EOBjyMd/qkoahqma" +
                "+vLeh7xOfMqVokuNqFj0Z2zRr7K93z2vl2yp3a7phtPTu2B8Ic" +
                "DHgY7/RJQ1HVMl9fW9D3iM+ZU7VIcLULH43smjX2hzpJ9bg8ru" +
                "2x2dZzbHsgzMGAh/FOnzQU9VqZT8NB3yM+Z07VIsHVLny0cREX" +
                "9bRD5pd75V5t98y2nj3bA2EOBjyMd/qkoahqma+vLeh7xOfMqV" +
                "okuNqFj0Z2zRr7Q52kul1u13bbbOvZtj0Q5mDAw3inTxqKqpb5" +
                "+tqCvkd8zpyqRYKrXfhoZNessT/USapH5VFtj8y2niPbA2EOBj" +
                "yMd/qkoajXyjzXQd8jPmdO1SLB1S58tHERF/W0Q+YvPotdPPu2" +
                "/8brqyU+RX1+3U+Dz679Pe/i07X5nuth9TDNyRqW9oqAa2zcqm" +
                "MKQE1PtXI1qRLX5DmewTWgh1wXnmmd+o5UPZ5Mz51Vs9rOzLae" +
                "me0VAdfYuFXHFICanmqFc5xpDkaU4xlcA3rIdeGZ1qnvSNXjyf" +
                "Tcs+qstmdmW8+Z7YEwh9kcxbvEwwprRtjjTlJyYDAj1gR1X1mu" +
                "Cx/NufnWyFixcaffr1f1PZde6/93pN9/Oer9Yj2qnM5xNdfzr4" +
                "vpWsU5Tn+3t/znx5PqJM3VCWO2x2wYvHzrzpShZ2tGfF5fFefA" +
                "yNft/b46VOIjfDTn5tvPWgXzM6/Hren19f94n1nD6x8kzaeB");
            
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
            final int rows = 50;
            final int cols = 82;
            final int compressedBytes = 1582;
            final int uncompressedBytes = 16401;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtWU1u5GQQ9RLWLFgAEhJZARISF0g0Hh9hThElmizQTH4lGs" +
                "0myiq5ARwif7fIDTgFLLFdfn6vXnlayigLNHJ/8leuV69eVbm7" +
                "3d1J0zTNq3+acR/ssIcP5HIncGA8J5If1HBm5WadyNzGC9Yff6" +
                "saOtd+sueI6m/+XO4959VaPuFmZ/PD5qvNz5P3bdP8/lvzjMfm" +
                "m0X0l4J837zYY/P11uiPz9L6riA/PbObX9fr+FLXsfvQfYg97I" +
                "CHh3NFyAWbh+tAgSj0XMv7ytXUJzdj9Vy11VZd9q4d1i69i8x/" +
                "/eb1m6YZ9rBDJDycYyeHixHFQz+f5fOsVZ/hqp+RXHNJFZnaiU" +
                "9a+WTlHmoN7TWO9X293h//P9exfde+iz3sgIeHc+zkcDGieKir" +
                "hqOuhRg6q/oZqX27KjLJ9SlyNqt71Tof+xTV/Xa/t/uwY2QfPh" +
                "HlcDGi+KQvGo66FmJzb0U/I7nmkioyyfUpcjare9U6H/sU1bP2" +
                "rLdnsGPkDD4R5XAxovikLxqOuhZic29FPyO55pIqMsn1KXI2q3" +
                "vVOh/7FNWb9qa3N7Bj5AY+EeVwMaL4pC8ajmatesep+hnJNZdU" +
                "kUmuT5GzwWVe1fMJM3/9nHmRz5nj9jj2sOMVPoZPRDlcjCg+PU" +
                "+i4ahrITY/x0U/I7VvV0UmuT5FzmZ1r1rnY5+iet1e9/Yadoxc" +
                "wyeiHC5GFJ/0RcPRrLXwvi76Gck1l1SRSa5PkbPBZV7V8wkzf3" +
                "1fv8jv691uN/aw4y/GXfiOkAs2D9eBAlHouVb5fb3rNRRxTmZo" +
                "D5xhaYrMxKR5IlevV2bmPnVPvX2CHSNP8IkoR9mapV7weMZzRT" +
                "SSrqTV4FJG7YnqubOlKXK21tbDM2vHOnN+XF026+MFHs+7P66P" +
                "jz2urtZr8Amffevn9fp3s/U6fnbXsf82/kXsgx328IFc7gROLs" +
                "41OyPQcGblZp3I3MYL1vB/V+9TO3TPEdXP/3et1V0P+XXCV//G" +
                "PthhDx9IxInxXLMzopkZ//h1ZPVtPNb3PrVD9xzZpu/VXQ/5GW" +
                "/ft+9jDwssfCLK4WJEcSqrnqKuhdj8Gij6Gal9uyoyyfUpcjar" +
                "e9U6H/tU/np/XD9n1uu4Xsf1Oi7+3Wyv24s97PgXjD34jpALNg" +
                "/XgQJR6LlW+WvPntdQxDmZoT1whqUpMhOT5olcvV4Z8hd+X/+1" +
                "/lr+lEf7ZeyDHfbwgUScGM81OyOamfHtXUTmNh7re5/aoXuObN" +
                "P36q6HfJ9wvT+unzPrdfzcrmP7tn0be9jxPf8WPhHlcDGi+HQn" +
                "EQ1HXQux+Z5U9DNS+3ZVZJLrU+RsVveqdT72qfz19fgir8eD9i" +
                "D2sOMVPoBPRDlcjCg+PU+i4ahrITY/x0U/I7VvV0UmuT5FzmZ1" +
                "r1rnY5/kd/fdff898j7s9M3ynj53YIzq4V7wVA/niuS68k3can" +
                "Apo/ZE9dzZ0hQ5W2vrkXfvQlRvu9ve3oadIrf0uQNjVA/3gqd6" +
                "OFck15XerAaXMmpPVM+dLU2Rs7W2Hnn3Lshvz9vz/nV5Dju+Us" +
                "/hE1EOFyOKT6930XDUtRCb3ytFPyO55pIqMsn1KXI2q3vVOh/7" +
                "FNWj9qi3R7Bj5Ag+EeVwMaL4pC8ajroWYnNvRT8jueaSKjLJ9S" +
                "lyNqt71Tof+xTVk/aktyewY+QEPhHlcDGi+KQvGo66FmJzb0U/" +
                "I7nmkioyyfUpcjare9U6H/sU1cP2sLeHsGPkED4R5XAxovikLx" +
                "qOuhZic29FPyO55pIqMsn1KXI2q3vVOh/7JL977B77++Rj2OnO" +
                "+UifOzBG9XAveKqHc0VyXbl3Ww0uZdSeqJ47W5oiZ2ttPfLuXY" +
                "jqXXfX27uwU+SOPndgjOrhXvBUD+eK5LrSm9XgUkbtieq5s6Up" +
                "crbW1iPv3oWoPnQPvX0IO0Ue6HMHxqge7gVP9XCuSK4rvVkNLm" +
                "XUnqieO1uaImdrbT3y7l2Q3160F/37+wJ2fMdfwCeiHC5GFJ/u" +
                "G6LhqGshNt9zin5Gcs0lVWSS61PkbFb3qnU+9imqp+1pb09hx8" +
                "gpfCLK4WJE8UlfNBx1LcTm3op+RnLNJVVkkutT5GxW96p1PvYp" +
                "/P8AJ1CrPw==");
            
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

    protected static int lookupValue(int row, int col)
    {
        if (row <= 58)
            return value[row][col];
        else if (row >= 59 && row <= 117)
            return value1[row-59][col];
        else if (row >= 118 && row <= 176)
            return value2[row-118][col];
        else if (row >= 177 && row <= 235)
            return value3[row-177][col];
        else if (row >= 236 && row <= 294)
            return value4[row-236][col];
        else if (row >= 295 && row <= 353)
            return value5[row-295][col];
        else if (row >= 354 && row <= 412)
            return value6[row-354][col];
        else if (row >= 413 && row <= 471)
            return value7[row-413][col];
        else if (row >= 472 && row <= 530)
            return value8[row-472][col];
        else if (row >= 531 && row <= 589)
            return value9[row-531][col];
        else if (row >= 590 && row <= 648)
            return value10[row-590][col];
        else if (row >= 649 && row <= 707)
            return value11[row-649][col];
        else if (row >= 708 && row <= 766)
            return value12[row-708][col];
        else if (row >= 767 && row <= 825)
            return value13[row-767][col];
        else if (row >= 826 && row <= 884)
            return value14[row-826][col];
        else if (row >= 885 && row <= 943)
            return value15[row-885][col];
        else if (row >= 944 && row <= 1002)
            return value16[row-944][col];
        else if (row >= 1003 && row <= 1061)
            return value17[row-1003][col];
        else if (row >= 1062 && row <= 1120)
            return value18[row-1062][col];
        else if (row >= 1121 && row <= 1179)
            return value19[row-1121][col];
        else if (row >= 1180 && row <= 1238)
            return value20[row-1180][col];
        else if (row >= 1239 && row <= 1297)
            return value21[row-1239][col];
        else if (row >= 1298 && row <= 1356)
            return value22[row-1298][col];
        else if (row >= 1357 && row <= 1415)
            return value23[row-1357][col];
        else if (row >= 1416 && row <= 1474)
            return value24[row-1416][col];
        else if (row >= 1475)
            return value25[row-1475][col];
        else
            throw new IllegalArgumentException("Unexpected location requested in value25 lookup");
    }

    static
    {
        sigmapInit();
        sigmap1Init();
        sigmap2Init();
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

        protected static final int[] rowmap = { 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 3, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 6, 0, 0, 7, 0, 0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 9, 0, 0, 0, 0, 0, 10, 0, 11, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 12, 13, 0, 14, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 17, 0, 0, 18, 0, 0, 19, 0, 0, 0, 0, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 21, 0, 22, 0, 23, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 24, 0, 0, 2, 25, 0, 0, 0, 3, 0, 26, 0, 27, 0, 28, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 29, 0, 0, 4, 30, 31, 0, 0, 32, 5, 0, 33, 0, 0, 6, 34, 0, 0, 0, 0, 0, 0, 35, 0, 4, 0, 36, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 37, 0, 0, 0, 6, 0, 0, 38, 7, 0, 0, 39, 40, 8, 0, 0, 0, 41, 42, 0, 0, 9, 0, 43, 0, 44, 0, 45, 0, 10, 46, 11, 0, 47, 0, 0, 0, 48, 49, 0, 0, 9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 50, 11, 0, 0, 0, 0, 0, 0, 0, 51, 1, 0, 0, 12, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 12, 0, 0, 0, 0, 1, 0, 13, 0, 0, 0, 0, 0, 0, 0, 0, 2, 1, 0, 0, 0, 2, 0, 14, 0, 15, 0, 0, 52, 0, 2, 0, 0, 16, 17, 0, 3, 0, 3, 3, 0, 0, 1, 18, 13, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 19, 0, 0, 53, 0, 0, 0, 20, 54, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 55, 1, 0, 0, 0, 0, 3, 0, 0, 0, 0, 56, 21, 0, 0, 0, 0, 4, 0, 5, 0, 0, 0, 0, 0, 6, 57, 0, 58, 22, 0, 0, 0, 0, 7, 0, 0, 0, 8, 0, 0, 0, 0, 59, 0, 23, 0, 9, 0, 0, 10, 1, 0, 0, 0, 60, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 11, 0, 2, 0, 12, 0, 0, 0, 0, 0, 13, 0, 0, 61, 14, 0, 0, 0, 0, 0, 0, 0, 0, 1, 62, 0, 0, 0, 63, 0, 0, 0, 64, 65, 0, 0, 14, 0, 0, 66, 15, 0, 0, 16, 0, 0, 67, 17, 0, 0, 0, 0, 0, 24, 25, 1, 0, 26, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 27, 0, 0, 28, 1, 0, 0, 0, 0, 3, 4, 0, 0, 0, 29, 30, 0, 0, 0, 0, 0, 31, 0, 0, 0, 0, 0, 0, 32, 2, 0, 0, 0, 0, 0, 0, 0, 33, 0, 0, 0, 34, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 0, 35, 0, 36, 37, 0, 0, 0, 0, 0, 0, 0, 38, 3, 0, 0, 0, 0, 18, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15, 0, 39, 16, 40, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 41, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 42, 0, 0, 0, 1, 6, 0, 5, 0, 43, 7, 0, 1, 0, 0, 44, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 68, 45, 0, 46, 47, 0, 48, 49, 52, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 53, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 4, 0, 54, 0, 1, 55, 0, 0, 0, 8, 56, 0, 57, 0, 58, 0, 0, 0, 6, 7, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 59, 60, 9, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 3, 0, 8, 61, 62, 0, 0, 9, 1, 0, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 63, 0, 0, 0, 0, 0, 69, 0, 0, 0, 64, 0, 65, 0, 0, 0, 0, 0, 0, 0, 0, 0, 66, 67, 17, 18, 0, 0, 0, 19, 0, 0, 0, 0, 0, 20, 0, 0, 68, 0, 21, 0, 0, 0, 0, 0, 0, 22, 0, 0, 0, 0, 0, 23, 24, 0, 0, 0, 0, 0, 0, 69, 25, 26, 0, 0, 0, 70, 71, 0, 0, 0, 4, 0, 72, 0, 70, 5, 0, 0, 73, 1, 0, 0, 0, 27, 74, 0, 0, 0, 28, 0, 0, 29, 0, 0, 0, 1, 0, 6, 0, 11, 0, 0, 0, 0, 0, 19, 0, 0, 0, 71, 0, 0, 0, 0, 0, 0, 72, 0, 30, 0, 0, 0, 0, 0, 31, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 11, 0, 75, 76, 12, 0, 73, 77, 0, 0, 1, 0, 0, 0, 2, 0, 3, 0, 0, 78, 0, 13, 79, 80, 81, 82, 0, 83, 74, 84, 1, 85, 0, 75, 86, 87, 88, 76, 14, 2, 15, 0, 0, 0, 89, 90, 0, 0, 0, 91, 0, 0, 92, 0, 93, 94, 0, 95, 96, 9, 0, 0, 2, 0, 97, 0, 0, 98, 1, 99, 0, 3, 0, 0, 0, 0, 0, 100, 2, 0, 0, 0, 0, 0, 0, 0, 101, 102, 0, 0, 0, 0, 0, 0, 0, 103, 104, 0, 3, 4, 0, 0, 0, 105, 1, 106, 0, 0, 0, 107, 108, 5, 0, 0, 0, 0, 0, 0, 0, 10, 0, 1, 0, 0, 0, 4, 109, 5, 0, 1, 110, 111, 0, 0, 4, 112, 0, 6, 113, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 2, 0, 0, 114, 0, 0, 0, 0, 1, 2, 0, 2, 0, 3, 0, 0, 0, 0, 0, 20, 0, 0, 6, 0, 16, 0, 115, 17, 1, 1, 0, 2, 0, 0, 0, 3, 0, 0, 0, 0, 4, 0, 0, 18, 0, 0, 19, 0, 0, 0, 116, 7, 0, 117, 118, 0, 11, 0, 0, 0, 12, 119, 0, 0, 0, 0, 0, 20, 0, 2, 0, 0, 7, 0, 0, 0, 4, 0, 120, 121, 0, 5, 0, 0, 0, 0, 0, 122, 0, 0, 0, 123, 124, 125, 0, 8, 0, 126, 0, 13, 9, 0, 0, 2, 0, 127, 0, 2, 3, 128, 0, 0, 14, 129, 0, 0, 0, 15, 10, 0, 0, 0, 0, 77, 0, 0, 1, 0, 2, 0, 21, 0, 0, 0, 22, 0, 130, 131, 0, 132, 133, 134, 135, 0, 0, 0, 1, 0, 0, 0, 136, 0, 0, 23, 24, 25, 26, 27, 28, 29, 137, 30, 78, 31, 32, 33, 34, 35, 36, 37, 38, 39, 0, 40, 0, 41, 42, 43, 0, 44, 45, 138, 46, 47, 48, 49, 139, 50, 51, 52, 55, 56, 57, 0, 5, 58, 1, 0, 2, 0, 6, 0, 0, 0, 0, 0, 0, 140, 141, 142, 0, 143, 0, 59, 4, 79, 0, 144, 7, 0, 0, 145, 146, 0, 0, 11, 60, 147, 148, 149, 150, 151, 80, 152, 0, 153, 154, 155, 156, 157, 158, 159, 160, 61, 0, 161, 162, 163, 164, 0, 0, 7, 0, 0, 0, 0, 62, 0, 0, 0, 0, 165, 0, 166, 0, 0, 0, 0, 1, 0, 2, 167, 168, 0, 0, 169, 0, 170, 12, 0, 0, 0, 171, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 172, 173, 0, 174, 175, 0, 8, 12, 0, 2, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9, 16, 0, 0, 17, 0, 18, 0, 0, 0, 0, 0, 0, 0, 0, 176, 177, 2, 0, 1, 0, 1, 0, 3, 0, 0, 0, 0, 81, 0, 0, 0, 0, 0, 82, 0, 13, 0, 0, 0, 178, 2, 0, 3, 0, 0, 0, 14, 0, 179, 0, 0, 0, 0, 0, 0, 0, 32, 0, 0, 0, 180, 0, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 33, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 181, 0, 182, 19, 0, 0, 0, 4, 0, 0, 5, 6, 0, 0, 1, 7, 0, 0, 0, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 13, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 183, 0, 184, 185, 186, 0, 2, 0, 3, 0, 0, 0, 14, 0, 0, 0, 0, 0, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 34, 0, 0, 187, 0, 188, 189, 0, 0, 20, 0, 21, 0, 6, 0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 190, 0, 0, 0, 0, 0, 0, 17, 9, 10, 0, 11, 0, 12, 0, 0, 0, 0, 0, 13, 0, 14, 0, 0, 0, 0, 0, 191, 0, 0, 192, 0, 0, 0, 193, 22, 0, 0, 0, 0, 23, 194, 24, 18, 0, 0, 0, 0, 0, 0, 195, 0, 0, 1, 0, 0, 19, 196, 0, 3, 0, 0, 7, 15, 1, 0, 0, 0, 1, 0, 197, 25, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 63, 0, 0, 198, 0, 0, 199, 200, 0, 201, 20, 0, 0, 202, 0, 0, 21, 0, 0, 0, 83, 0, 26, 0, 203, 0, 0, 0, 0, 0, 204, 22, 0, 0, 0, 0, 18, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 205, 0, 0, 0, 0, 0, 0, 0, 0, 0, 84, 0, 0, 0, 0, 0, 23, 0, 0, 0, 0, 0, 0, 5, 0, 6, 0, 7, 3, 0, 0, 0, 0, 0, 0, 1, 206, 207, 0, 0, 0, 0, 0, 0, 208, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 209, 0, 0, 0, 210, 64, 0, 211, 0, 2, 3, 3, 0, 0, 0, 65, 86, 0, 0, 24, 0, 0, 0, 27, 212, 0, 213, 25, 28, 0, 214, 215, 0, 26, 216, 0, 217, 218, 219, 0, 220, 29, 221, 27, 222, 223, 224, 28, 225, 0, 226, 227, 6, 228, 229, 30, 0, 230, 231, 0, 0, 0, 0, 0, 66, 0, 2, 0, 0, 232, 0, 233, 0, 234, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 29, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 17, 235, 31, 0, 0, 0, 0, 18, 19, 20, 21, 22, 0, 23, 236, 0, 24, 25, 30, 26, 27, 0, 28, 29, 0, 30, 31, 32, 33, 0, 67, 68, 0, 0, 0, 237, 4, 0, 0, 0, 0, 0, 0, 31, 0, 0, 0, 238, 239, 1, 0, 1, 32, 0, 0, 0, 0, 4, 0, 0, 19, 0, 0, 0, 0, 0, 0, 0, 0, 240, 69, 0, 0, 241, 0, 0, 242, 243, 0, 0, 0, 0, 33, 34, 0, 0, 3, 0, 0, 244, 0, 245, 0, 87, 246, 0, 247, 0, 0, 35, 0, 0, 0, 248, 0, 249, 36, 0, 0, 0, 0, 0, 0, 37, 0, 0, 0, 0, 0, 0, 0, 0, 0, 35, 0, 0, 0, 32, 33, 34, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 35, 0, 0, 0, 250, 20, 0, 251, 0, 0, 1, 38, 0, 0, 0, 0, 0, 0, 0, 21, 0, 0, 4, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 39, 0, 0, 0, 0, 7, 0, 0, 0, 0, 40, 0, 0, 0, 0, 0, 36, 0, 0, 0, 0, 0, 0, 0, 252, 37, 253, 254, 38, 255, 0, 256, 39, 257, 0, 41, 0, 258, 0, 40, 259, 41, 0, 0, 0, 0, 0, 260, 0, 261, 42, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 262, 263, 0, 0, 264, 0, 8, 0, 0, 43, 0, 265, 266, 0, 0, 0, 0, 0, 0, 0, 0, 2, 3, 0, 23, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 267, 268, 269, 270, 271, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 272, 0, 0, 273, 44, 10, 0, 0, 12, 0, 13, 5, 0, 0, 0, 42, 0, 0, 0, 0, 0, 0, 0, 0, 70, 0, 0, 274, 0, 0, 0, 275, 0, 0, 0, 0, 43, 45, 0, 0, 276, 277, 278, 0, 46, 279, 0, 280, 47, 48, 0, 0, 8, 281, 0, 2, 282, 283, 0, 0, 0, 0, 8, 49, 284, 285, 50, 286, 0, 0, 51, 0, 3, 287, 288, 0, 289, 0, 0, 0, 0, 0, 0, 0, 290, 291, 52, 0, 0, 0, 53, 0, 0, 292, 0, 0, 0, 293, 0, 0, 0, 294, 1, 0, 0, 0, 5, 2, 0, 295, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 44, 296, 45, 0, 0, 0, 0, 0, 71, 0, 0, 54, 0, 0, 0, 0, 0, 0, 0, 0, 297, 0, 0, 0, 0, 2, 0, 298, 14, 3, 0, 0, 0, 0, 0, 11, 0, 0, 1, 0, 0, 2, 0, 299, 46, 0, 0, 0, 300, 0, 0, 0, 0, 0, 0, 301, 0, 0, 0, 0, 0, 55, 0, 0, 56, 0, 302, 0, 0, 0, 0, 0, 0, 57, 0, 0, 36, 0, 0, 0, 37, 5, 303, 6, 304, 0, 0, 0, 0, 0, 0, 4, 0, 0, 2, 0, 305, 3, 306, 0, 0, 0, 0, 0, 0, 0, 0, 24, 0, 0, 0, 0, 0, 0, 25, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 307, 0, 308, 0, 309, 0, 0, 310, 0, 0, 0, 311, 0, 0, 58, 312, 0, 0, 0, 0, 0, 313, 0, 0, 7, 314, 0, 0, 0, 315, 316, 0, 47, 317, 0, 0, 0, 59, 88, 0, 0, 0, 318, 319, 60, 0, 61, 0, 2, 26, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0, 89, 0, 0, 0, 3, 48, 62, 0, 0, 0, 63, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 320, 0, 49, 321, 50, 0, 72, 0, 51, 0, 0, 0, 0, 322, 323, 64, 0, 0, 324, 65, 66, 0, 52, 0, 325, 67, 326, 0, 68, 53, 327, 328, 69, 70, 0, 54, 0, 329, 330, 0, 71, 55, 331, 0, 56, 0, 0, 72, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9, 0, 10, 332, 0, 9, 333, 0, 0, 334, 335, 336, 73, 0, 0, 0, 337, 0, 0, 0, 338, 339, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 57, 0, 0, 58, 59, 340, 74, 0, 0, 0, 0, 75, 0, 0, 38, 0, 0, 0, 0, 0, 341, 60, 342, 61, 0, 0, 0, 0, 0, 0, 0, 0, 0, 343, 344, 0, 345, 0, 0, 27, 0, 0, 6, 0, 1, 0, 0, 0, 0, 0, 28, 0, 0, 0, 0, 0, 346, 0, 0, 0, 0, 0, 347, 0, 62, 348, 63, 0, 64, 349, 350, 0, 0, 65, 351, 0, 66, 0, 0, 76, 0, 0, 352, 353, 0, 0, 77, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 354, 355, 90, 0, 356, 0, 0, 0, 357, 0, 0, 0, 78, 0, 0, 0, 0, 0, 0, 67, 0, 79, 0, 358, 0, 80, 68, 359, 0, 360, 361, 362, 81, 82, 0, 363, 69, 83, 364, 0, 365, 366, 367, 84, 0, 0, 0, 368, 0, 0, 0, 0, 0, 3, 0, 7, 0, 0, 34, 1, 8, 0, 0, 0, 0, 0, 0, 0, 70, 369, 0, 71, 0, 0, 0, 85, 0, 4, 5, 0, 0, 6, 0, 0, 3, 0, 0, 0, 370, 0, 371, 86, 372, 0, 0, 0, 0, 0, 72, 73, 0, 373, 1, 0, 4, 0, 5, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 87, 74, 75, 374, 76, 0, 88, 89, 77, 0, 78, 375, 0, 376, 377, 0, 0, 378, 379, 0, 0, 0, 7, 0, 91, 90, 0, 0, 380, 0, 381, 0, 382, 383, 384, 0, 91, 385, 386, 387, 388, 92, 93, 0, 0, 0, 389, 0, 390, 391, 392, 0, 94, 95, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 79, 0, 80, 393, 0, 0, 0, 0, 0, 7, 0, 16, 0, 0, 0, 0, 394, 0, 395, 0, 0, 96, 0, 97, 0, 0, 6, 0, 0, 0, 8, 2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 396, 397, 0, 0, 398, 399, 0, 400, 0, 0, 0, 0, 98, 99, 0, 0, 0, 92, 93, 0, 100, 0, 101, 102, 401, 0, 103, 104, 0, 0, 0, 0, 81, 0, 0, 105, 0, 0, 0, 0, 82, 0, 0, 9, 0, 0, 0, 0, 0, 0, 0, 402, 0, 403, 0, 0, 0, 0, 0, 0, 404, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 405, 106, 0, 83, 107, 108, 0, 84, 406, 407, 0, 0, 0, 408, 0, 409, 0, 109, 0, 0, 85, 0, 410, 0, 0, 86, 0, 411, 0, 0, 0, 0, 0, 0, 0, 0, 87, 8, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 412, 0, 0, 0, 413, 0, 88, 414, 0, 415, 0, 89, 0, 110, 111, 112, 113, 0, 416, 0, 114, 417, 418, 0, 115, 419, 0, 0, 0, 90, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 116, 117, 118, 0, 420, 0, 421, 0, 0, 119, 422, 0, 120, 121, 423, 0, 122, 0, 35, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 123, 124, 0, 125, 0, 0, 126, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    protected static final int[] columnmap = { 0, 1, 2, 0, 3, 0, 4, 5, 2, 6, 3, 2, 0, 3, 3, 7, 8, 1, 9, 1, 2, 0, 4, 10, 1, 6, 9, 6, 1, 0, 11, 9, 12, 5, 11, 1, 13, 3, 1, 1, 3, 7, 0, 14, 15, 16, 17, 9, 11, 18, 3, 2, 16, 19, 3, 6, 11, 20, 4, 9, 7, 21, 22, 23, 24, 1, 0, 25, 26, 2, 27, 28, 1, 29, 30, 0, 3, 31, 16, 2, 32, 0, 11, 33, 34, 9, 1, 0, 8, 35, 36, 16, 1, 37, 38, 4, 1, 39, 1, 5, 6, 40, 41, 6, 42, 43, 13, 44, 45, 2, 46, 1, 47, 0, 1, 48, 49, 3, 3, 50, 9, 51, 52, 53, 54, 9, 1, 3, 1, 55, 56, 7, 4, 5, 0, 57, 0, 58, 59, 18, 7, 60, 61, 62, 63, 1, 18, 15, 64, 65, 66, 17, 67, 20, 68, 2, 69, 4, 70, 2, 71, 72, 73, 0, 0, 4, 20, 74, 4, 75, 76, 77, 18, 5, 78, 20, 79, 80, 81, 3, 82, 83, 10, 6, 11, 2, 84, 2, 85, 86, 5, 87, 1, 88, 1, 89, 90, 91, 92, 21, 93, 94, 95, 96, 3, 97, 98, 1, 7, 99, 10, 2, 100, 101, 102, 103, 22, 104, 105, 106, 0, 107, 108, 5, 109, 0, 110, 16, 8, 8, 3, 27, 111, 112, 9, 10, 113, 3, 3, 1, 114, 2, 12, 115, 116, 0, 117, 5, 118, 119, 120, 121, 122, 123, 124, 9, 21, 0, 125, 8, 1, 1, 126, 127, 2, 22, 0, 4, 0, 128, 11, 2, 14, 129, 29, 130, 131, 132, 1, 11, 29, 1, 133, 12, 1, 134, 5, 17, 5, 1, 135, 17, 18, 7, 136, 137, 138, 21, 25, 14, 5, 12, 139, 1, 8, 140, 141, 21, 142, 4, 143, 144, 5, 145, 146, 147, 148, 149, 150, 25, 30, 151, 152, 9, 9, 153, 31, 24, 8, 154, 155, 4, 156, 6, 157, 158, 159, 160, 10, 161, 2, 162, 163, 164, 33, 14, 165, 166, 167, 35, 168, 2, 7, 8, 169, 170, 9, 36, 171, 172, 2, 173, 174, 175, 39, 29, 40, 176, 177, 3, 178, 43, 9, 10, 179, 180, 13, 44, 181, 182, 183, 184, 185, 186, 15, 4, 187, 188, 21, 6, 1, 18, 189, 190, 191, 9, 192, 193, 12, 1, 194, 195, 196, 22, 0, 2, 20, 197, 25, 18, 198, 2, 11, 20, 14, 3, 10, 23, 0, 199, 10, 200, 201, 0, 8, 10, 202, 203, 204, 9, 205, 206, 13, 207, 22, 208, 209, 2, 0, 210, 211, 212, 30, 17, 11, 8, 2, 1, 213, 9, 31, 12, 214, 215, 11, 216, 217, 45, 218, 21, 219, 220, 221, 2, 222, 223, 224, 12, 22, 47, 3, 12, 225, 7, 14, 226, 227, 6, 228, 229, 16, 230, 56, 231, 232, 233, 1, 13, 234, 235, 236, 237, 238, 3 };

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
            final int compressedBytes = 1569;
            final int uncompressedBytes = 38977;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtXT2OGzcUfqTeyhNlC8rYwnbFxEKqFIsgB2CMLQIjhYoUKR" +
                "fYdGl8BHqRAEGSwkdY9ylyhAWSA+QIPoKLHCDkaDSa0XD+NH/k" +
                "6D3AsmeX0gzfz/d+Kd+xt/DrC31hXpev9PMf1BOQ36gfvxT/wP" +
                "ca3v33OW5xwT+9xNd/f/X0zcNzuWHyCtb6lz8//P7dX0A0B+KF" +
                "n6zMHyxdo+yLBpDEupmSKMg/Q2z/Dw/k/9LoYRQBGPxCrp8JWI" +
                "Jk6gv7ZDcA14rjv+wTWF2iWkguYA0bIW/MXxd/3OpLwq/OdGf9" +
                "xzOwr0tu/cfS+o/NJvEfHzhugXHLf+M/ttZ/QOI/bn8j/2Gg9C" +
                "0DLaxJRQZhmVI7eEXxCKhBP1gr1Nwa4+NToR4iuTPOd1rCShH+" +
                "eoM/7w3+rFL82RjJPBj8EQZ/toxhjD+3O/yBjU/4o9jCaFaqf9" +
                "8qo10qgst1rH+Psf5Z34881T+jrvCzlpr0zw/8lSX4e2Pw96PF" +
                "37Ubfyl+r/dfH3P+6w3xb0D8uTfIeYQ/ugx/9AD4EyVx7z4Exk" +
                "Pmo5NfoL2SyTIZzQn/sHZBRNpKRERERDSr+K+6/g0LG/+9Kq9/" +
                "ZxyjcHrUeAHbl1DlLpq4hj7ih7uLXPy6dcX/VfWXn9h7qMsfGO" +
                "UPROHRVe2KrP2x2HijjDHH9iuS60jkE4X4DdIT/Ersd3GC/XtB" +
                "PHlBTECTFTMSXiO/M6W0/yGy/Q9bf4RD/THuf7xM+h/gYf8jK2" +
                "rO9/I2jvLQayqk6jlfS0RUxJSm+sOdC0buf6vshWhSkenj/u7+" +
                "6R4/rmP8oP4p0Xj4P4F7D37+xeE/6/kX459i1H8+UgbecGEv8c" +
                "fU/eMs/q9c8eNmHz+m95dD3L+4f3m8f+lj/9wL/CIKlmz/U7ef" +
                "v9D+zl8c8rcm8be7//61s37xwlF/rOjf3+znz9Zhzp9hraOR3f" +
                "1/j/p7X9RfPXv9Pfqhq/6fnx+Amc4PBE+sYv7j2Op2y/qTH/rA" +
                "gHDmX3ys/GEQMp5eRjRFNIjhOrhe2n+GHvvPgRift1TTv294fm" +
                "K6+K/z/qvnDxrsX56uFDJ8/vUX/pT0vx2wXdb/Fifzf1Ye7iTw" +
                "xiLjLkrQfBj+1cl/oPmHmvmFkc6Pdamff1asn2O+fg5J/Xx7qJ" +
                "/r2vp5+/yzQ8q7ZwPCskzjZjp/0aT/IfbyeyzKr7p/GuL8xazE" +
                "SzS2SKUTK7hvuj3CPS5T7GaYOMx4JTt6p7CvUSoaCVymLtmAUX" +
                "zB7g3m4CozJGKcTGNh6umDVtVnDeBkLeYTbrq1/iBLPwLzK7Ek" +
                "LPAHBcLL31U9x8gfnmxYaqwHceLbY7BJO6tQR9GrRcmq7HQycf" +
                "aRWjtUVfUspCvnzdDBWC3GUkT0SWEDIx1sFM88tbpun6HaSU7X" +
                "cUMNYGvYK5N4ALY+sC5g6/2p7rYdIH7xHH9Efi86ixfo5FXX98" +
                "+qisFOEoAsU7LK+Gl0L8OGej8OJifWMtQnStkupwW5xh5MuRBF" +
                "VT14lzKOf6lLePjb0H+LDD6wKiHo4kV0HEN47ZhxoA/Fks9XXj" +
                "0oUT+601o4irh3VjQYBk48lNnt/PW48QPpH1GGas9PpvMfi4r5" +
                "D5q/OAv9Jfvxjv9Nzg+WnD8c+vwg1npp6Vp6pvPLZL+0/2mp4v" +
                "znsdXO8vzg/PXn5Oefb6LefP554fH8c0siz0kUFP6pM98/UQj8" +
                "H/n8IKG438TzsbMn4lLupNyhrOOe35wy62myf5jp/jPYSuenw/" +
                "M/zvO74D6/Kxqd3x33+yMHRV/t//dvtzm/e1byC4SK5x9v0/OP" +
                "i1jA8UXN+ce+6196ZEvLzX/pLFzqLM5jJiBSpDsn2SLxjYioCn" +
                "8gjz/8aP5UNrej0caKsHivzH9S7/IOXPj0/HOn1qDLzgu0Ma9q" +
                "MssEWaKPu/in1++PGGguTs5acln+8xr+Ryqq4j9l6qNH34PKLx" +
                "rLuXTHR0pmu+L3QRsc+mN12uIvQBj23xIC8vt3+S9r07n9w9D7" +
                "9yl4o6STqKnRsQbo7Js+YYW5SfeTi97u3Cn+HeX713oNmglLBt" +
                "HfEg+oXCublpupfnA68ylkJSIaIyvv9v0D/wP21L0M");
            
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
            final int compressedBytes = 1144;
            final int uncompressedBytes = 38977;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtXUty3CAQbQh2EZcXpMqLLHHFB8gRSJUXXuYIOQpJZZGFFz" +
                "lKjpAj5EiR7PGMmAEJxF/qtxjX2IOA7ub1a4Q1AAEgrj8wQCAQ" +
                "+8Ow8sX4ykEefiOBygldqJc35AeAZjegjg0VO7ZANOZRE/L4cg" +
                "BFI6UENWwuzHSrp8Znb2+GJmrTNiFpL6dKLJn5MetX5+oM4yHj" +
                "ALhrWCbLanEZZ9XtF8RH2bUmT5z/FOY/BALhAeEmOkt6OVJOvf" +
                "rbwn93vvr/PobTRbbxB9cvd3DFp1ne8Bgdri8syVSxi/411k/1" +
                "odAECASu0sT1u3xLcxx5BpFZOiLqW48H7iSsHvmEf7TH/mF+Bt" +
                "VrVLiDP8E1fkx3Tfi/gfs/F/lXeM5/K/cxqNuSfC6ePNZpbHus" +
                "vxCInUsohWu9cTAf0g/P/8Qr/yszGih1hwZJfWM4ueRHRJpQd6" +
                "nK/MvHXJxHQ4anZtf/9FN8Mbjbqz/zO1V1QQICLLU0Jl5EDZDN" +
                "zow7N6bwoGQjopZt1jNv+vup3v4bxj+iXvyLmfqT+cZ/oRsLpH" +
                "J71BCIjdm/q/8/wvjF+Edg/kSE8LdKxt8YP8ulsi8mm4sx5997" +
                "h7iwxgJUOf+V0DefBpfzwfvf4Rej+qOAa5BEPYxdPw5mUZR9JQ" +
                "Rubpl6J6mAD/AA8nH4cfX8Td/+acV+qfhLptOfeJzTm3/3zD+7" +
                "yl/70m8UjB3URghBZbmmNKe9BI2cWVX8bcja3GmEq8UZZhBYEu" +
                "MxVov7x3VpY+P+Q6BdKNq/ASAnNZGB2YnhVL4BhJ/fo5HtG5j/" +
                "klHYKgcScJy0ZD0tdrIvqS1LJKKF/mnF/tcuFe4KG2kNdyryrN" +
                "RQ/khz/zh2/SdozwOzt4DsDojkv1r8yxPNn536Z+f9kzKLsoj9" +
                "4vN/7fhJrD90d/qvTQFMXJ/04b+S+Wfv+be6/8sICFv+HFr+9n" +
                "/+nTL0w30b41+tf/zHH8tXsmoNknn/48z+YLM/HOwPdv1ZdeWG" +
                "xM/K8bv9H9Y/VzXs14Kftg1z/cDU/4c3oyYz4g/SxB9iN/Ezw1" +
                "8z/O3VHtGx8k6Qv/34C1bzV54MJoq1797/K/RP2FNY2o6fOvVj" +
                "ofpNjPXNy+XodP/nvc2Fep1/Ec1C+vhRtTXmnYWeTFe/rqof2+" +
                "JZlYY/LfaT3vZjTelfab5Ra+f/mgHO5v/FFT/X+efPA2ZhQbn9" +
                "16MbsCTaCfh5fKrAdVqCFs+6bjM4t/v8tYKgUdHRugD8W67mFp" +
                "X7R3grHtHVaJuqlNItZ2f9wV/5m/076cefN6cHrz+N+pkvGinL" +
                "+OkKLVkO+P3FsckOMc9BtHvv9yH+eOfj9wUJOL+rz4SUVhZb3Z" +
                "sftVC/HK/6OR/nxuS/u/Tnj2LFxYaLpQTzz//944gMmx25hBrf" +
                "5RLZTC4qrtaj7r9ke/5OJ/ZDtFBzVp15tv9fm4Xl/Lhe3X+//N" +
                "3H82dVs/YrXT1u1f8p5bTeXPznXj+5z59vot5A/V1YGohJrPX2" +
                "/U/1FF0D5ateqiYa8t9/USSgUg==");
            
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
            final int compressedBytes = 959;
            final int uncompressedBytes = 38977;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtXe1t2zAQpQiiIIqgUIEMQAMdoCNc/uVnRugobJEBOkpH6U" +
                "i1HFuWXNKWxOOX+F4AI1Z4Jo/Hu3tH0YoQvLDjy4DO04o4u9RC" +
                "KPdf5GPho2Q/vL4Kc75ihDST0dPpTffrqJX6PBk4qVEi5/hHLX" +
                "yf4e3zIkkn/fVcfxpanPR/O13/Oej/ftH/aF+a6b+6/91DNqVt" +
                "Gtt3sSZYLW3Vu+MX5PPKez6g87VUzPZ/uKoU1h/kI67/zPGv9f" +
                "hbsf4X/qtD+G/b/idnH9DP84/9mNL/+yHEr734r5m/YQ4O3PWz" +
                "zTwsG3VmOpSeddeP/R1bqgTyYot86FZBYP4ojP9Z9lGpvOvPbB" +
                "mGnuyy7XD/ph8lf9/wx2c/f6QZfzxUMjsmuv8E7l80Lx8aP6L0" +
                "H+x/KeaPpf7La3+G/Jl5/ebef0D8KAaoX1gztL5XqFpMXCJ+vb" +
                "F/A8sEeIMptv9uavmw8wde/nKWV3+v/GUq/zrI6yI8z8+/OhH9" +
                "/AkP/wusn+qOnFntBwAAUG/85Dp/+JD0p4//C/Jnbv5iyTFnhz" +
                "vl0Ud7M4zqu7OBlInrW1p8MU51Ta4uE/YP5ActbCeLG6nhVC9f" +
                "sVn2+AkeAqTlD2z1uy4ydWlOomdL0sxWMf91uJa7PhCtfT2n8f" +
                "k3+55ktvi/+i4Q8S6TPP2XD1PJ+gF2jh1yEKz/tvMngPgBAAXn" +
                "H4qff7af/wdRbzChmhb0l7A/1AWA8kFTF8PWNgCk4+8c59d4+b" +
                "teJxn1/NpU/qT/+Ut5ic7fy40FC1ffOfvfH28GFrizhIEAIAa6" +
                "Ih3+Jv/+SHr/iuH+GcPzj0vf3KD99u+ArdvLKx/+xkKgXcLkjF" +
                "/GH7/6MX7hkedVwOTyH924/2usJtgfSJLFHBevzz+anrE1w0tX" +
                "3Pi1399c2sl+9/a7D5y/SAiq2n4GBqyu/hCi9vPH+znsaRbbTz" +
                "6wn05ov3rJl801Bc3eGcnND8B/AACIxTv6YuPByB/syP+el/K/" +
                "Q0la8PHfDPrf33+4XTlmZf4pv/7a942MRutfVY59Ceu31f2LF1" +
                "/8/oTnB7NBrilicVfGv8yw1IDUqPv8G1A1LjWUPf5Iq3txTMsd" +
                "fRtTuxRvx1+/PAnqhnuuXy8RUr2TffqD+UP9AoSwtVS1kMmcVp" +
                "b3j1qupWh2Uz/KbfVj7eefsH+A568BlRdxVRO2vONvN+ez3P9S" +
                "1+ZXkKO3/ny2hsoaf9FrSwW5EuAC+Sa2XzXxlcRZ8BQK4X/kjB" +
                "8vyb6/rEX2/39J0SwTzL//ARC7pWU=");
            
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
            final int compressedBytes = 898;
            final int uncompressedBytes = 38977;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtXctx2zAQXWIQD0cnHFQA0kFK2Nx8TEmIxwWktJSUkJZkyg" +
                "IpkPgD7x08kqwVFov94kdJpIgkjaTpA1qJy8th+sPzm+GNyMgT" +
                "MV3Bkr5TGuiN/8lH/smVfx2AN9/2LfTnPfJf0psM/Z9wJjnaPp" +
                "/bF//5U3cf87X9DQxkIdkahXHZm2X/L29m2ZzuaXQA/QvTvhu9" +
                "tNCn0P+S7e/GRTj7PYfxf2Mu/xHCf++0v6zj98d9/DhL/OoVal" +
                "WhOG3j+80yGSrIX7jk/AkAAAAAgMYwQgQAABD1nOyjzgEAIBKm" +
                "aRYDZxMCBiJoRv7jjvrjwPqF2vNlxtDmqB97p08jf3FnP8oSmL" +
                "TFzHgmbFt/fENyH/oD/quXn97JAbtGe1/64hFw/VTtbhxpSRVo" +
                "uaSTxDb958nyZ/3/NX/+e9L/99On4TM/2T93oZd/P+1nSf860Y" +
                "9+5lNcvOlQfwKAbd7Q7hqHCkomgREFfPKPc/jzA6rwxEOuOoax" +
                "llH0wJm+XbtpHrzdsf3/QMD4q+vsb0Vsr+iySNJyqftnpYt/We" +
                "PfpON/zDl+qP88lUzVff4P9RfiNwAkwdBrx9lTUsnpAaAgT9Gu" +
                "/j6b/1/mb/P8/2VTwt38P+crGnzXP4pCpCUU5F89QbhmOfU4NR" +
                "PdRGBrAIC6DIBPbUxYMC7oX+1giKBpSMtGEb5Zn211UgQ6PxTy" +
                "/skLvQp//xo/l18mO0qz/h5v/MqNH+wpfy7o/rDk44f1h+z++4" +
                "n2q8DtI/8Dera/APFbR4vfUXueff+fbtxZ6CLH78Xz/u8X+HXg" +
                "q5rrgk1lA2LltRXt3CqI+xGBhvM37UZ5KP5Zz/8OjvSGtp6/sE" +
                "q/6/yxS/uw/0OBCSdcm/IcS/sRD/bzs+z8d83+9br9q1v9Jvfr" +
                "/lcYtqQSWxPDH9/X02/+KEB+R/mnQPynad8+gqpds8b8NRARvJ" +
                "50FpT/9he/NeH5P9H0PUjuXHj+Rz0+f9Nn/Gp/fmoK/a3Nz5jC" +
                "5O+f/8XmP/b9zT0i3ORUTP9Vhv9pO//L4D878x+4Swv1Uwj/F6" +
                "f+6r1OK7z/zd9/FcN+uKn5hzTxYyhYt4QnoXAsx1R2hjf1v7r1" +
                "MwAAYoP3xE+cX2kHTTw/BXEKiFU/A8+9RZbxc3x+y8b+HbPh+u" +
                "z7n7Afs+ZUpgaIyqUqonmBo+dvdftK4yQ/APnLEfwDk+rNjg==");
            
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
            final int compressedBytes = 763;
            final int uncompressedBytes = 38977;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtnVFu3CAQhscWitw+8dADkBvkCByhR6JVD155k2w2rR2DzT" +
                "Az8H9SVptILDD8MDNA1kTgiID27+KI/Pq63GsJNL+9ndaXePtl" +
                "+k2U3HeK94LRmbcr9G+MafCBnLgs5crb4lXXD+SZ337aD5g/qR" +
                "YBTZ31v3HL/3re+v+815926geA5ty1P8JW6nCflmwARpG9XM2l" +
                "/t/f828nUj/VrJ+h/T9y2/88sn4N5U8B469ZeT3aH/sXLfzHdn" +
                "mvTz/x2ApL3uoVOxv/q+t33qRy3dpmf/7EA/0/7c2fovKM4yet" +
                "n4YL8rzzHjSWSYAZNCYlbqBuLwyfES9PjNEXBgAAMAL//kNTIt" +
                "v+QZ38XcQeToP9EaQe6m+hsv2rovLW45cW8/dq/wXsN7Wafzrs" +
                "r3v/+/r48/kvzB8N/uu8fi3cn9esf8Q/2L8AAAAATpIEneV0pX" +
                "6l96eGuf/kKG7Fr3E9grsZ4eft77/2/v/ly/h36iP/089n/S00" +
                "Dlf1K22/FDfqfP5nTf8/lQtr21/g98AA+f/19seS+AP+Rxnd2G" +
                "/CWLaIf+rH31F5/QDI5A914m/kPx/M/S0KqeK2RxIuDw4m4lxN" +
                "4IAx/gG1LN/B+X938Wv2/TML95fkM1Vp/XDrXzeKdfKh/3TX/4" +
                "nzu779VyCQaSaYarD4J6N8LPCfft8pTSwTs+r3VyL/wPoPDMa/" +
                "dvWD+/uy/Wfwn0Xx92P5xNT/oFj/bfxXZOs/9Ktfv3bi90B4/o" +
                "mRoKTWKcp8+HFpL8FSgl/XV//QidcGf9s6CH7oy0JVzDmXlcdj" +
                "O5A/VIk/sH/eX/6+CJeXxm77cydIp3cfXNf1a86/QAv/FTmVW1" +
                "8/UkGmdHBrO7gOhKMCAEB3+Q+f/2X6/oCuzv9OlFfVf4Hxbxy/" +
                "2b0/gfuz3cZ/1Z5fEAzqN5CB5w+2mb8L5/y1fv9Euv+4/9EDpp" +
                "6/ak5/BuNvAACwCvYPQDkC5//4/j3b8auO+nXHn9brz2bsy5Wh" +
                "yVAl4fIA1OQvS+7TnA==");
            
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
            final int rows = 217;
            final int cols = 16;
            final int compressedBytes = 304;
            final int uncompressedBytes = 13889;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtmgEKwjAMRdNSZMqQCh6gggfJETxSEQ/kEd3mNhhuMJXRNP" +
                "1PqJsQbdN0ST4SgX9wRL4dKwr9J4Fsf2nagbsbcyeK7kA8GrLr" +
                "LGbsz2vtLzrX/5X9BvPPyv85EBLbS2aIodi8bKw87SgYvo6hae" +
                "nWXB5rYkPW02lwhntwrJ8fX+enoS3DsUFx/KV+fuH8icRMb7no" +
                "5zf2X/D+28nbHHEpwQCV/UuC+ndaqlTYlvyiCPsHAM6/yvqTk8" +
                "4/wfpX16+5638C66/U+qc6/UJz/6f0/MB/IGd+i9/cGyeIQ6j/" +
                "y1t5BvmDxc5fqP9E6fehcHvN/UsJ/XPIfP809z8ASAf5q2z9r/" +
                "T8E7D+jVULfksX/Z9gYjfu5/SYqEesAcifIs6f+N8vff4ALPIC" +
                "VXJMcQ==");
            
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
            final int cols = 120;
            final int compressedBytes = 4281;
            final int uncompressedBytes = 19201;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrdWwm41cQZPTNJJnd5LvgURX1SBFn6hAcqrT7AhSpFtFQt1m" +
                "ItpSKIoCKKgKDIJptUoC6IbVVErFoFF9CKWlRA/Wq1X6FiXVpF" +
                "wIUWlUoRRbj9ZzKTm9yb3JfHu0+0+b6bTCbJZJKTc/5l5lodwG" +
                "BZzZBBlu/CfqgULVCFY3A4WqE9OlidUWMLVuu+g1o+CN1wvO3y" +
                "5pkB6MH78ivxJ/R2XPSxIJ7Gz3EBBlrVGIJL+AqRci613k/3xC" +
                "iMxDW4ViyDjRT2EU35LPtZtgsH4iAcZjXld9gDWcvUeWhtT2bV" +
                "1lZU823ohKNTb2QWsy/5jTgWx/G7cQJOxvfR09rCv8RZ1iH8Cv" +
                "44zsVP7bZsGfqxs9Jv2fvymXy7vTeG8c8wAqOtKbiaDWIuP9Va" +
                "gwrsz58RLdEUB+BgNOMjcYg1Gd9CC9YKbdAO37YO4JZ9Ljpam/" +
                "AdfJcfhy58vrgUJ+F7OIUP4D/NLMJpOJ3l+In2+TgT56S74jz0" +
                "tw7EAJxvTcVLuBCDcSmG42Vczs/DWIxzLwCHA8Hfh4s072Fx9g" +
                "X2wr5WG3cdmuBQ3i07wHLQkj2BI/hKuzp1MdriSByFzvYA3p09" +
                "zE/gOb6Vj0JXdhJORHd7NU4Vy9ELP8AP8SOcjR+7A/ET9MXP8A" +
                "t2LgbhIgzFxWIYLsMVuNJqjzG4SjyTyzmjne/b17rbnDGYzHfx" +
                "IXxM+jmMF7WiS04tTk8PXz6WDxZ/UjUVvHl2aS7H++ZybEkulx" +
                "6Qy1lIPZQLLen70wMJ315mXyzT9U/yeU5X0dXUOzPFeudqdcbp" +
                "tNc0l3NXyb3Um9nV1O7CfIuYQPuEryy7vdx++vrN1Iuzcjl7X7" +
                "qmjb039eszOpfwpXrCl655MxexWJPlmrUy+/Y4VbtJrvmFVOov" +
                "RnhH+IBc5IL+zs3qmqlsabBePCfXmf766g/1/bg5nvmlqr84O9" +
                "Ca4dW4s8WC1Ev+9e/wM60h4rfyAk69YieZI+7W4H0I34leiQ31" +
                "rx2m30oPTKI9whdT+AOYSi3dx+/nu1RPlhQ/C6vNzBJ/ttRxt6" +
                "PueV/dX/UE6Z3eXspS58zJPJSZEHrqZabEn6f2dvm9PF3yV5X2" +
                "Y9USX74tF7tkTnBfL+iZbtft4Lev8FXHBqnn2RmPr9+LT1PrxY" +
                "YgvsTfMbrnL4kVukRv2j4/0MaB6om7hlvm54Xu06kQX3edh29F" +
                "O+sRem8tqLwyeAXx91Z95vN8UYl34aPKxhbiS/y9js6ge6Zs/i" +
                "4svs7TZ/EyKp3xmMbfwXTS5xvQgb+NObiR1WZf4IPxS8zG8frr" +
                "7aG3vel3vfsGrX+uWlb6TG0/mpmijpM+K/x3wdbP/w+Jr9Rneo" +
                "LPMlvFeokvWqc2smqX3kZ6stRn1dvuqoXj6Ef6rJ7pNI+/xYvk" +
                "rzp7mIdvqpvB16Y2sb8+eoDqwQYPX9C7xUxPn1MbDH9JnyV/3x" +
                "LjpT77bFX6rEozcE4QX4vYgVlSnwPcHpdVegZhHZvHF3t5+KKJ" +
                "wvfb1iOQT36Ef9WR9OssNnJfcfhG0NeDE/0zlD7TVukzbT19Du" +
                "Dr6/OvcJVi8jppf+2qvP11NqJKLDL2N32OfSi9qQfFw/Zh1F63" +
                "SHz7pIdmPpb2V9UofLNrss8afH37exP2oecjjoqHPHz196fwpS" +
                "3xF9VU1vjqO/j4kv19V9pfXU/2V21vlvrs7GXwpd8tGJ35PUj1" +
                "pT47e0v7q44p+5vnb97++mxT+Kb/gC7W22JCIb7qDb4n7a9fq+" +
                "wvbbX9pdLl9CP7S2uu8D1e2l+DL9RXKO1v+gmqK8LXs7+pFv5X" +
                "29zYXzp6qsHXs7/qioD9pT3f/pI+z9X4fkj43poZaI/Cr8Vs7G" +
                "ePNviK6Whvj7CvxDwxh/yrG+jZpoFYIa4nHlFrmQv9fvUhPEkP" +
                "PHxVv0ZmX80+J2bp4wpfVZL4HizxDWlsS72tFjMlvnELemZOoH" +
                "UBvvrLVVZN3KCPkT5L/0rVTUWFrqUvC7cVtNkmpKabdG0Xu1KM" +
                "j+nFmYEy4RvY8/FVe0qRre4evvoMozKHZoZIfIvaPooUdWXKR9" +
                "y+StdrfPXejwps1Ng8vv45v9Hb251ao8+YjztQmT4Zd6ZPwW+J" +
                "v3d5+kwtLMcCdfbd0fpM+L7m6bPhb8UR2ZVBfZb81VblV6TBPf" +
                "L8TT0f5K+6Joa/Up+j3rjxrwx/jf31+Butz2pP8ndmMX+zv6Cr" +
                "JFYR/E2tyutzoI9F+qy3wuoZqDUaQ/qcpe/C+SJKn4m/rYP6LP" +
                "kbpc9B/gbuofhL24Uef8XHxN/fZQbifiwS20if76NjVelVOJy2" +
                "7fGA0wU1eITVii1YLL6Q/MVDFB/R15J9WGwXn/v8/TzIX8J3Ss" +
                "W3/Htq/uIeio92uE/RXbtG81fGR/orYEXv8F7i75tx/DX6XMxf" +
                "qc8B/v4+j29J/l4Tzd7Ui4a/6J+Iv2fE81f8J4q/hG/bPL6l+I" +
                "sHi/jr+5J4WOuaxNfKPib5q/CtxBLCd4uxv84kLJX+s9jiTHTX" +
                "evZX4iv5K/H17C/h2TZsf6nmuiL7K+PfHequMfbX7WTsbx7foP" +
                "3N7CjGF/3C/lUhvnansP3Fo37L0r+KsL8SX6dZNH8zBxbaX7WN" +
                "t78/zuMbtL8evtH2N+A1Nvf8qzrsr8IXj0l8ff4+DqPthC/x1/" +
                "evJH+zR/v4kvY6F7Fafa72ryS+FX3y/pVzrvSfw/jqKzS+VFL4" +
                "6ii4RxBfZ6iHr0MKX9q/yh4b4V/1CzFA+lcjSuLbLAm+Ymg0vt" +
                "L+JsUXfyB8+0b7VxJfd30UviE92T8RvqtNfiOgz3Pz+izmidtI" +
                "n59Q/F2WPYb4ewrh+6TUZ6tJXp8lvs4NUp8197dnDjH6rLYXxH" +
                "gkCl+jzx6+0frs4Ruvz2obqc8F5xbgG9bnJP6VmBvbdv30uV+0" +
                "Pqv3F6nPYXx3W5+f0gp/EFbhadcVDCt4e2cnVuIFPMfb4Rnh4E" +
                "XqQw2d05F35vSVcc87acOb8/1o24K35cfQliy1OIorxvH9ufKQ" +
                "REfejJNfww8O9H25OqN18RsTdnEd75BLvLCzovAN+lexV7YqeJ" +
                "+b/D7NzZVlsYaXyFDMKOj1H4veTCfpX9X5/EMDbTyrt88H7a+b" +
                "kfqcaS71mV8WzD+LGtS4r/E+xN9aPszYX89/FtuNPovOnj5joK" +
                "fPomOcPnv8pT2Vf47XZ8NfrDb6LPPPUfyN12dtwyL0GaQ7Mv9c" +
                "rM8y/2z4W1qfg/yN0WeH9HlktP31+BvUZ5N/LsQ3Xp9N/pkPN/" +
                "qcqfL1eYzRZz6DT+PXEX/fyLRwdvIJfCJeo+N/x1pCcrzkL22n" +
                "iy3cz+h5+synSnx1P04u+O46Rujaq+rIjmT8Ldbn+iyGvyZ/Fb" +
                "fwj+L467ap5x3XhPbe8lu8Ov6aQn3G36L4W8d9fxbkL+Gr87d4" +
                "M8hf/CPvP3Pyn/ljef6q97BUbKGrJxXy1/jP4uywfxXNX5nfCP" +
                "LXjzlO9Pir6kv6z8n4a+xvDH//mcS/cjs33L9S/vPcpPyN8q9K" +
                "89f4V9a1efvr8/ftQPxr4Z1AfLQOVXQ8jO/TEt/sybH4npME30" +
                "J9Lo6PGgNfa2cBvu8mwrdrefC1j298fHV8tD6E7waDr1qrrJ1Q" +
                "IzepXoTgWlXSHhv/C3+N9Hl1hcqL8lfIv/ozbV82+Y0Y7VsTqU" +
                "kR+pw6rWH6XOhfYWNp/0qs+er8K7t7cn2OeIejguODdftXAf/5" +
                "Pb39hH5b3PfxAd6n0sf4EJuxib+OfwXuQvjSerXee4V+Hr7/LN" +
                "m3InzxUUz/AvHR7i7F+Sv/yN5FPRsZ6+2WHV9naUOulvgm+L7H" +
                "Rtjlf+vv6xipz6mFKv/8qbK//0JV6h6jz/o+m8UHwfxG4fiRKt" +
                "WZ3/DGFyLs733F9jc6v1EW+/ufRPq8qTz67Dwer89UqkOf+bgk" +
                "+mx3NvY3kN/YqvW5CbbjC3yWvgQ7QGor1BdsZQNv7EvJXz62mL" +
                "91fHtrEn/jV+fKvCT1n+P5W7f/jP8merYnG8TfcYlsQOeI3ulZ" +
                "EiDFpvjXlfx1900vVPlJNbsCZJskf+X4kaUsr/grcnHjR2Z8P4" +
                "q/uhSKgoL8lfERAnmP+PGjBMgOi8I3PH4U4u/MKP56+hzD33qN" +
                "HznLo8ePovkrx49CXkiT+PH9PH/58OLxIwaTf87l0kvcjNub30" +
                "8llXtnl4T6XiPHF3KNuKQT2ShMqC9/8/M36s/fuu0v+ifi78oG" +
                "8XdRyR5MLHFskrbNw9wz2HBv/lUJC96o+JbDvyo3vvXNb8Ti+2" +
                "Lj4VvKv2KXBd6G9q9UuVIxKifnX0l9FjWYw65gKpPNLo+dfzUo" +
                "rM/h+LcufQ76V18XfbYry6TPLzdEn038W1qfQz0x/pWef8UPsh" +
                "ZknrDuJPTG8PaePmM8b+ffoQY1dY8vuBcWji9Y8wvHF/QX2TqZ" +
                "PhePL9RfnxvZv0qmz39pCH9Fp4bqsxz/1Yy+hsqEW2a5l7+S8z" +
                "ckvoy8W+tufW63yLb6uIPD44Ph/LMZHwzGR3H5jdLzr+o3PpiP" +
                "jwLjgwnnX8XjW7/5V87f4scHI9o+Khrf0vOvqMaPj/waf/6V9Q" +
                "CszCZv/hWbqPznO63FZv6V1GcZH2GBjI/k/CsvPkIPLz7S+nxR" +
                "sT578VEyfbbu/b/V57V7SJ/1/Cs2yXoBFu0Tvmxytp9nf62/yv" +
                "iIXZfHl+peKYHvkIbgK/X564Yvn18mfD9Jhq/1bHnxZVM8fIm/" +
                "a2FZryt8p1mvojLVi/j7BpuKVtnBHr5yfJBNl+ODbEbev+JTA/" +
                "gObZB/ddrXD1+pz2XBd+ue5a+0v2wWrVV0JO2vnH9l7C+bLf0r" +
                "PMLlaNliz/7K+TmefxWwvxd79tfaELa/nn8VnJ8T5V+F+lcd61" +
                "/di57pVIPtb8L5OWmnLvubbH6Os63x7K+ZnxNpf/X8SX6QzD+z" +
                "m9X8uvYy/8xuRRVvFxwfzPvPMv/s+c/EX+U/ozc/nPC9lB8t88" +
                "/Sf/b4a230/OfC8UGDb5C/ar8g/5zHN5h/TjdreP6Z3ZQk/5xu" +
                "Wp78szisIfnnZOODHr7slmD+mc018ZH6f+gneXytzWF82TwPX+" +
                "vjEvheVoTvRxH43tRQfCPHF26uH74F+hw3vtCmTPi2+qrwLRhf" +
                "eNzgq55LeQG8vSpvplIg/mW/1vhuKxH/Di+Kfz9KHv9GZm7qMb" +
                "9uz8S/yZbUXo0X/5bMad0Wjn+N/WW3S/sr+Ru2v/wILLazYfvL" +
                "1Tx7xd/LJb4ef439zeP7jbS/vymP/U013UP29ykf6bv0dr6YTf" +
                "Ur2QJ2t+RvdomZHyvmcKUaYppaXx/GV33tV9j7hPmr1rOSzY8N" +
                "PM/M0vxNN4u9suj/Zf6RqbvD3/TtZeJvn3qoTuT82HoqV8H8WH" +
                "YvVrGFdkv7cHaPxDe/VCzx9FnhO53e+GSDr0Yg8N7cEQX90viG" +
                "7rw8gR7NrGOkKfWV4euUaWxs3p7Bl/3O959X4Wl1ZIW0v2Z+O1" +
                "tE5cTz290rC+2v3Sr5/PbGtb/1WfL42pXlwTdzx57lL8/PYnzK" +
                "869UbTv2JNYa/ibAd2QRvq2L8H31m4RvueY/Z+6qRxu7Of85tF" +
                "8w/9nuqOLfZ1R81FvNv+oVMT+2iRlf8PJXXITnX7mjks5/jspf" +
                "RcVHUfmr3YuPqLQb8VF6TXnio+wZeyg+ejv4/0Ev/yz9Z1SyFa" +
                "iyTwrlN/z5kyr+Lc4/93FHF+Obzz8H499vCr7lin/pt2fw1fOf" +
                "8T+i2Iu+");
            
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
            final int cols = 120;
            final int compressedBytes = 2032;
            final int uncompressedBytes = 19201;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtXGlsFVUUPvfOnffa9yDShSWmQKhgRSw7KiK4AYFWNlv3GF" +
                "wIIBFwAwQUEClLiyFAkQAii0UgcQEl8YeJBqM/FMREgkABEQUV" +
                "KotG/7jkee6dO9t7M+/1LX2dqZ3kzr1z5868mfnmO985Z24biU" +
                "Qv5HPrVqAPX7PhkbhLcK59O9A30qwLvJDaccoF445KIh5YAv1S" +
                "PZJ8ptW0Ez6N30TPF7QULpMvyUG4QHtCg4kv7UsH0R5YtxdHlN" +
                "CuNB/rbvQ6OhDrYnwa8+gAsa+QdtDwZRUUr41ebXvql7Dn2sZd" +
                "H+3t1Jv7Y7bwzT3jc3wPaDWbbvR8Ze5lM6z8pV/To7j+Rj75Q1" +
                "gOYrGMD86P5S89nPn7DfXIFr6ha7KLJHsqw/gekvUJWk3qaRU5" +
                "Qo6S4/g78+licsxg0SLNPtMVWJZEMWyZBd8XW5p9ZgU+5++3un" +
                "1mS7l9JqfISWGfv4OLKdjnhTH2uaop7HOr/iZtn18DAgo+kRCE" +
                "yWnIB3xvoTOWYugOpdBb+lfr5XMbCsNka6Ssx2KpDC7C9USYJH" +
                "qmwXSTvzAHZsNLsBBbDMtVRn9H6GRDROIOvbD0gwGWPYOx3Abo" +
                "4cGo0Bxc3yP7H4KHRb3Odp6ZOr5g2BRoA4Wi7gDtwfLGQTcsJd" +
                "ATrrfiCzdhGRKahes74C5jbDncLVsT4H6j9wl4XNRTYCrMgGew" +
                "9SyW+bAA1xRLAEsQcuXottBO1HlQJHtQBcDQHLgB+sMgO75wK/" +
                "bfDnfiejSWMhgD46AC7oX7xBEPwiPwGNaT4Ulco42Hp+E54Nf+" +
                "B8zV+AuEbQpsZJvJT8jffHKW/ACdkb/FbAuURvMXhjrxF/FdzP" +
                "kLkyz8fd3kr4avaF2ViL8cXzf+wiixjsLXjb9WfGWN10aiPDQo" +
                "ceIvDAmsdz33BEtb4iu3DHzFFpW9Br7YbifrIpdz93fmr46v3K" +
                "qIOc7A1+DvOVnzN/Yvch7XfztaiD7wj6a/bGuc+OiVlqa/oYT+" +
                "M/zpafv8i66/QMgl2eb8beD2mfMXa8FfcrER/F0Sw99tmeMvrc" +
                "s+f+k2n/P3V8NTrAOF7eb6y95kO6GA7SWX2TtsB3Rnu7j+wmq2" +
                "h70vj7qi6S/bbtPfmmAV11/ZI/WXvYvlba6/sp/ZrshFf0XbRX" +
                "8bxdyZTvzV9Bfr9rZerr8rnfQX+fuvq/5Wm/pr7F3F9deyvUDW" +
                "AduotrLOM3qi9BeLo/4aI4T+Yh2lv5bfkPpLQOpviObSMA1SfL" +
                "NoGyf7LEblRXm3gSj7vMxb9pm2Tds+e8F9TsM+G09iDC0X9Xgs" +
                "Y0WrLAV8l7c0fJVHWwi+ndg+Ue/n+mv09rTlrxLHvyti4t8PYu" +
                "NfMSKt+Dccyha+4ZyWgS+7LO+sna33vHHHefb8pJLvlJ90fMaH" +
                "/czf+PkN1pCd+1AK0z5DsSpyDoot36ra/BANX7XAPf/shq+a51" +
                "d8w4Ve4K/SoynOSqbb4t8+Gr4G9h0dvi9UN73+wsveyk9CFhQ6" +
                "vn2GxXH2GRkJtUjPTyoY3UKBzl/ojkXERyZ/STUM0/gLIzV8ZX" +
                "xUExsfafz1a3wU7uLz+GipmZ/U8eX5DShQyrT8hpl/tuY3EF/h" +
                "XyG+wr+CsSK/sZIO4Pln7l9p+KpdNP/Kmn+GWjO/kSj/bPpX1v" +
                "yzU36jifLPZ/ydfybrNXzVEjXKBih4H+qNsfob7/uv1/yr9O1z" +
                "eLQX9De4KmXlLpfWeRzZLnsecFQAEf+qExJcx6bE+guf+Arf8m" +
                "Z4Kz+O1V/O36TOsV/6UDv1+FetNO2zrr88/rXor2GflSmO9rkm" +
                "uIPbZy3+lfa5wrTPVv1NZJ91/XWyz9nUX+5f+Vl/lanSPlepS9" +
                "Sl4t4WxOHvsgT8/dBb+avW7/vm/Bx1OakPomUgR5VXZd8xM/6l" +
                "p/0Y/8bHl571S/6KnktZf1ca+G4k9eoGjq+xb411ZA5yV0V9ZV" +
                "0ivllaCn9Z13TxRf19i8+/yjmjrOXzr5Ra6/xY9jyb1Zj8M65t" +
                "+Wc2W93RFPOvwrOdR+d8n3H/yhP8ZXNTts8HjPzGLizIX2Vdzs" +
                "9uo9VN3uBleF7W/GdP4BtMWd90/eX5DXWv+p5olwLyUtlgnb8h" +
                "vi9Y8hu5XZ3mb2j8Nedv4Buxx5vzr5SNUSNd5m+42+dszt/Qvy" +
                "+kPv8Ke0kkkiuP4Pjq8yc1fOlp5Q1Ea5/cP9TxqiplPcnlqi34" +
                "JmRerzj7RoVrM4Gv1b9yw9edv9nEV/evUp+fQy1RqNP3X3VIKv" +
                "qLx30arb9srTX+TU1/s+c/J+tfsTXe8p9ZreFfHVa3IB5bnfEV" +
                "WwLf3DnJ4Os0vz17+Db191/v+8+2t13mr0K99PyV6BX5Kyyrtf" +
                "mx6gm8/ymO89trRGui7JkGlu+Lmf1+FP4oW/krbp+bP38VzEs3" +
                "f0VOsDpSz3bz+Fc9yXYijqeUNXz+JL49uzT7bM6fNN6r7Y1498" +
                "T8Sb/mN7zB3/TzG5ZcWOcwuHrbt0R8tWj4ksl+t8+ZiI9U4WkF" +
                "bk6kv0n6Vw2t+uuNhdVh2R0oQ0x+5/aZ7cVi2Gcxwnf2uaXMn8" +
                "zcEhgf52mJ7wsB6fO0zp/0Jb6Vtq2Bsfh6Yf5G+GIrf5PEdbB8" +
                "X4c7//2CIhTe/PuF4AixHWieqw1f+X/hq2Rglm4AYzzleFDEjc" +
                "oRed76qOeV4P8zeG1ptc/Gk5BZhuDmzPrPgRFN4T+3KWq1z0lG" +
                "iv8BIShXYw==");
            
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
            final int cols = 120;
            final int compressedBytes = 863;
            final int uncompressedBytes = 19201;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtm19IFEEcx4dx73dbZw+WlvQSFmkGRVK9FhH0z7R/UFRQWB" +
                "QmBKEoHqSFWZGP1ksYGFER+BYVRQW99NYfqMw0egysh6DXgra5" +
                "ub29nb073bnb82bnfgO7M7/ZOXHmM9/fb2ZuD7ZZPEXHLIu2Jk" +
                "p0l5WRaJXHBkvpRBfk97mKn6lStMHSIsF21pe/lhW5HQa+lS1z" +
                "xTdmWtqk6D/YkeA7w3hdY9egp+5qCfjuR76S+t2ZzM2b6J+15N" +
                "tsWaSC8X1OYn74kk0iX9LqbUs6yFnB7rFzQ6hdQmoFu95VbiLr" +
                "05b5jNVstvXb6a9X5gvP/1SdrRWp4/dGsjo7X7KFbHXaNpPdTv" +
                "lQlr/VLlj9di7ogNizjqRHc4UcLcLYkD28dJDfD7OrTWhxjnSR" +
                "bjYCL0mck2oBHtPo3lDE3zjqV1K/+1yjUlS+5nQAfPt8zvPecu" +
                "dr/hDteUMCdWEdQ9/TCXb/YFvv2PWGXW9n0dDHIuh3EPUrqd8D" +
                "yXz+d1xf6bo/4nx/p/jCEYXj7zDylVbwUdavSbt/43Y+Ffz+17" +
                "geAN87qsZfY1gtqsYNh++xBN/Yn2LzDZd/1imJ+184ru7+1/fq" +
                "seD9L3+iwf7Xpx6YfqGtwBX79BzO2N5y129qf0Rr4ZQxAifgJC" +
                "uvcXg2Mg8+6ljr4DRdyfIabjXQZXQhy+voKrqB5ct5LVccraaL" +
                "bf9/izYxe2nGTKn3OaPWlmJU9PTPPvXbrn8/9eNLXVFQ1C90pP" +
                "VLN8rqF86gfpVjLfAVnrj4stzhC525+LIy8lVav64dcpe8fpGv" +
                "qnyh28s3L//cg3wVpp7hnwXL4evSeNyrX24j39DzhfPon8PPF/" +
                "py8cX4q3KC/kLWz7i+Cin1C7Lx136C8TeE+yNRvzCA+g29fi+h" +
                "fssp8fPny/r3U0f/bH9/dAW/PyrX+JvX+dUo8g1RPL5bHv3U0j" +
                "8/gPuz6RfGpPV7D/WrCN+H8AQeB+6fHyFf5Vgn3697muVJyN5v" +
                "R76u2Po6sy5SI9Dkv0+JLLItid+nRKqQb8n98zcYhwn4nPDP8B" +
                "U+wWQg/vkL+ufAlTgl2f4Vv/9io5nj7IJeTJ9vhOn9dtSvXIpG" +
                "cH8U3vMNY4QRhJnOr+T9M55fKUN4yO2fo5V0AP2zPon8B9AjfT" +
                "o=");
            
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
            final int cols = 120;
            final int compressedBytes = 503;
            final int uncompressedBytes = 19201;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtmstKw1AQhmVsctaKl6qLWqFqF/VCLfgMLgTvF1yIoDsfwo" +
                "3gA6hohVLBK4KXtm+hFnSh4hvoQxxPY6lJKFqhoTmTfyDnnAnp" +
                "IvPln+mchMKyYpSorOLSZjRCKYqpud3yBihCrWqO0iCNqbnPOp" +
                "u0xjbq+P6N6KRR5XdLl1G/rMloSDbAmj8kM6Ow6PKCr/EJvvqY" +
                "6AnGfXLUr0f5OQL9avQU7ChivdCvlvU36oV+Qznol3V+joEva7" +
                "7DOvJlWFm94psEXx9Tj7v8lMOr8LV1UONOvuUrwbfxve2UlMbB" +
                "79cYacRJ2//P017kZ+ME+dkn+p2BfgOl6LxiPus6V0BctM3Py2" +
                "JRLIh5sUQJ813MmW/1yM/mK/KzT/P1SpVoQ7+8ma8iBpqS27Sp" +
                "dNIaJ6rot8Xlm4icLvXXtq7n/tUW6q+PVb3tiHWRXtT4VPYe1X" +
                "Gvjoc/CD0jjprQ3i3xdbCrgS9MG7574Mu8SlfhK/YRF20Ve4j6" +
                "GyD1Fu36FRnkZ3aES9/XZREHHftfcz2UNtfEsbv/DWV++l9z49" +
                "/f1x2h//VFpT21xRT7V/z4noEva77n4Mua7wX4suZ7Cb6s+V6B" +
                "L2u+1+DLmu8N+PLe37Ct6/l+/xb7G77Q7x30y5pvDnwDSD3voI" +
                "n3g/qSLEC/nK3pCwWi8Nw=");
            
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
            final int cols = 120;
            final int compressedBytes = 523;
            final int uncompressedBytes = 19201;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtm0tLQkEUx2PUmWkRVFREUFHQa2EUaa+PEBRFlNm2iFoU0S" +
                "4IalFEVLQOF9GXiB6L3PWgTWUP8zMY1Bew8SqW1yhFhZnD/yzu" +
                "zFxGr5yf/zPnHFGcxFLGRqzrUCzDWIVtzWMwI0ycgi9pvmfgS5" +
                "rvOfiS5nsBvjBxCR+QYRn8Rb9R+MVMY7XiSkwJn5gUfubmETHB" +
                "w+puR9qeLuZlLWqstlZtrJFVqrGJtTOPGputuz3WtYrVJF7DX1" +
                "m3WtdlPK81y8/VCTYF4nvNn/gLf1ZzxZeHwFdX42857g9afG9S" +
                "PnWnZnnzdY2Drx76/TEvIF9xC76axOc76Jc033vkV4SroYdsdr" +
                "nG4Cmcv2nn7yP0q0l8DqE+olwfFUm/H+BLOj5/gi9lvpKBL2m+" +
                "peBLmm85+OrBV9YXg69zC3wN+hbsKEU2wA9G6teD/iRdk95sdq" +
                "E/ifzKll/1Qr+axOd+RWMgwZdHZB/is7axdjC3/Yn+pPQVQ7+O" +
                "Y/AlHZ/94KuJ6qdV/nT4T34VgJ+MPX/n5Kyc/1u/ciFn/c5Av1" +
                "rwHZZL1jhK6f8prMy4KLpYzHd3hJNPWU6ubb8kx/tXbNN2bxvq" +
                "IKPyeH9yBX4gzXcVfkB9lMqv1pBf6cNXrn/zdUQL0t94B1/D4v" +
                "MG/GBk/bvvDCh6e/b47DzKKz7vQr8GVWcH8IGZVvIFgCJtfQ==");
            
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
            final int cols = 120;
            final int compressedBytes = 595;
            final int uncompressedBytes = 19201;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtm81KW0EUx9PDzdwZ2oCNSSmtIGprdRGp1PQhLC1utLo0oE" +
                "9iKdm1pZuSgiKKLmoXrXXtA/iF/bA10oVPYKsrEcfx5nJpbkJM" +
                "Si7MTP4H5uMMcwM5v/nPmTskdFtKpyAlfyUlZaRv1K9GZwPvIW" +
                "XpvmrTnveAOimp2i7qoyHVdnujj7w6RbdKzzjvaVD5d2TIqFfW" +
                "ZTQgYU0w/saj0cNf14x2XpWZ0NhLRM+aVfAWMbCabwExMNNI5V" +
                "8+x754/bL8+8+c/8i/fBb5VxNtKrbxd7XnxKFfY/XLN9ha8/XL" +
                "VqFfLfg+5TteO6LKM683XGXWzZDPNP9WCeN20e3o8m/Qb2b+3Y" +
                "V+DVpdB4iBuedn7/7qN+6vrOZ7CL6Wnp+P+J+r8i//2zDfF+Cr" +
                "Cd+TSM5Xx+Cry/4cCd9T8NWFLz+rxhf5F/qtod9z8LWarwRfnd" +
                "6PRAz7s518xbUo9Ot8Bl89zFlQ6iW/v+S3K86iqpdLnnCcT6Fn" +
                "5uv43I+qfEB8dTQhQmrKlnmBfitUF+jX96FfPfleB1+rs3J/o3" +
                "zFDfBtpfcjkcD5ymq+beBrUD5OIgaG32+0434D+3MD+3MKfHXh" +
                "K9LV+EK/LZR/7yIGpuqXTTkFlhMdtfTLpqFf5N+y/HsPfPXgK3" +
                "qj4Ivf11mu3z7wNWgV5BWxJ4iDkWfjkcqx+OMyutu0p+pd39tS" +
                "ZUOVzSvWxFfE1uL9eQz7sybnq0l33H3ujrkTlGFFd5T9asr/f3" +
                "+CryZ8c+w722M/LvXLiuwb+OpqbL/B+etSxi4ARB7hYw==");
            
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
            final int cols = 120;
            final int compressedBytes = 600;
            final int uncompressedBytes = 19201;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtmstLG0Ecx+24md0ZFV+pxFzEgtqCBosJwf4fWj0rod568S" +
                "Cop/ooFTy0l5KCIA229IHY6tn/QBR8RBEv7anqzcdBGMc1lCRS" +
                "N7ETmR2+P9idzOxuCL9Pvr/H7pKQEFZSCDYsBOkQGSNP5Orc31" +
                "kniZEWOT50Z22kidTJsZk8JlE5PnJXu9x9kDRcX2N9IE/lPCzy" +
                "jLSKgoxEBEyBBSbYSAFnTcFTJhsbhQ/8atZHuX12KY5ZC3I/Lu" +
                "ffrJTcf8qcsWgt5V0zX8D3fpfbV/hXC31OesTnJHzkTyOhrM85" +
                "9VXW+h3qKzaN+koPvmymJHzfgK8msXlWxt/3iM/mxmf2Vr1+A4" +
                "vQr9H59x34apJ/ZX/kdF/zpXtOnKZV8KU74KvanGfFnU9XXb4L" +
                "dp/93O61+6/42j3ga56VpzP/kJeZ+W6et1/L7VXemkb3LNkaGP" +
                "47/7rPF9bxfMFUvs4F27hZX2XzpQnw9XUPvOlxfAs+Qn+U0x9t" +
                "Q79a5d808i/0WzhfZ/Q++bJ9kPTQ7wH0a3B99cvD25r3v7D/5P" +
                "8bPjCa7yF84Nf8y4686yv6pej7zynkX6P732Pw1YcvO1XO9wR8" +
                "NeJ7rpzvGfj6x3iFH3/1gwTI3cy/vEqFfnkl9KsHX16bzZdXq4" +
                "nPvAZ89dMvD/J6vJ9jcnzmDdCvMVUTceNzI12hy6rrZ/oTfDXJ" +
                "v2Fn0HlxO19nqOjngwPg6yOlt8MHvtVvJLc/UhOfA3+gXz37X0" +
                "X1VRf46sL36v0NHsX7G4bG51gp9Gv9AF+j43McfHWwskt3qNK9");
            
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
            final int cols = 120;
            final int compressedBytes = 490;
            final int uncompressedBytes = 19201;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtmr1Lw0AYxuUlPdJDF6kiLqLi16AoKroICuqkOIiLg4uLgq" +
                "OjkwjSUQdBKnQS/wF1dhZF8Kt+gYuo4K4iQrxGCCaU1NJrubs8" +
                "L7TXhGR5fnmeey9XqnO8ok7vV4fzp6ib+qlFjDXuURs1ULUYG6" +
                "md+sTY5J7tdb8TVPt7Dx+kHnFc7wSKWp1/FXU5KAlFpeI7BL5G" +
                "8x0GX42egqQgNgId4F/Pv6Pwr9F8x8BXn+Lj0AD+9fl3Av7VrL" +
                "+ahA7G5vMUNEA++/J5GvmsBl8+Uwq+sTfwVSR7ZwWN7fBrYino" +
                "ZEo+8yop+VwJ/2rk8TlooO38u8QO2YHs+Zftw7+q5LMlZle+HO" +
                "RrpYvha+2Ar9HroxXwVSSfV9kVy7DrLF92zy7ZrZR8vgFf2cXu" +
                "Crz+yOufNvKonRSftcC5dShuTP+8CQ0w//rm3y3ks9F8U+Cr0V" +
                "OQ3R/chQ5G892DDibkM94/m8uXJ+wBrH8juD46hgb6+pef2Avh" +
                "/bO9WKh/7Xn4V5185qey10fgqw5fd//oDPtHEV8fnUMHjf17Af" +
                "8azTcDvhHP5wfooPv7DYn98xf8qwpf+zsX3+LymT+CrzplNfOn" +
                "fPmM/2/om8/xd9n5zJ/L6V/+ApKhfD9l841/lJXvK0jmroofWN" +
                "1KMQ==");
            
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
            final int cols = 120;
            final int compressedBytes = 418;
            final int uncompressedBytes = 19201;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtnL1KxEAUhWXMJmFnbVZdRRBR8K9YUXQLURG0thBd/1/BQi" +
                "wUbLSw0BU7GxV8LcXHsBXjGCQYbaImMHf4brGTLIEs58u5Zy6E" +
                "jaIsVX6NKJGler8c15Oj8dQ1k6qhhs3aHZ+NqgFVNeugGlMzZh" +
                "2Kv52OP7tU7fOJeFNT5rzvx/1GMv6uCdjkw1erIviGL/C1hG87" +
                "fOnPv+WrA/g6zVfDV9BTcGGIVdAB/yb+reJfp/l2wldYf66hA/" +
                "5N/NuDf53m2w9fW/h6d4ZH4ztf7+E/fL17+ArL31l0kOhfPRds" +
                "BZvBRrCt6v5z0PSf8ujP/iP+tYTvfBH5W1qDr9P7qwX42lB6Mc" +
                "tVpVWUEtufl8hfh/27jH/J3z/k7wr+FTb/rqOD03yb6CAwf3fI" +
                "X8f3z7vsnx327x7+pfQ+GjAfpeajA/qzsP3zITrg38S/R/jXab" +
                "7H8BXWn0/QQeT8e/rx/lV4le/7V2EL/wqaj87QgPxN5e8l/rWk" +
                "P7eK4Oudw9dO/5Y7cvl/hgp8BeXvNRqQv6n8vcG/TvO9ha8N1f" +
                "YO51Ztfw==");
            
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
            final int cols = 120;
            final int compressedBytes = 393;
            final int uncompressedBytes = 19201;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtms0uBEEUhcdNDZ10IRFELIh/FoQghMdhwaN4hhlh4yextP" +
                "UGIuI5WCEkEkppyUTPQmZCJVV3vrvo6u6kN/frU+fcTsuIa5Qs" +
                "Ns4W3I+SZVmXGb8OFVdzMi4Dfp2QeVnz62Rxd7U4Dsrw9zN5TV" +
                "b89ahrKpl1LZUsOeofSjxfU/M86s18zdFf+Jo6fOPgmx+G0K+5" +
                "hG86lR/Tg3T35yD+e4J+VfM9hW9U+eqMfKU0X52Tr1TzvQjBt3" +
                "oP34TeggO/g1/RB7Xz0TU9UM33hh4wH5Xmo1v8VzXfO/hGNf8+" +
                "MP+q5vsIX/bn1vlmb/BNKD8/0wP0W8pXL+hXNd9X+MbBN3/Pdr" +
                "O93/lm+2377w58Vev3A76R6DdMfn6Cr2b92gp8VfPtgq9qvga+" +
                "qvlW4RsL36/vz7ab7886+dqeEPrl/7qUylp6gP+W/LcX/Ublv3" +
                "34r1L/7cd/O0C/Y+i3g/PVFD1QzXeaHpCfS/l5g/05Kv/dxH/R" +
                "bxv63YKvar7b8I2hKp86CHu/");
            
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
            final int cols = 120;
            final int compressedBytes = 251;
            final int uncompressedBytes = 11521;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrt2bsKwkAQBVAZYreVqAQbUfBVKIra2VtbimBv7aOw8Acs1E" +
                "6IkB9QBH9wXKJIYhXEIrncW2SzS7aZw5ANEVfV8VTNSFXa+o60" +
                "7Kr/mXVlKDU7FoJZQ8qSs2NFmjKwYzVY7QfXvBRfe5yL9Oy8pF" +
                "+RusaKdJT5Q8QN3Ud8Q+s/+JoxfZPiG/TvlP2LGDOL81R2wkql" +
                "1HdOX8YsWAOeryLnqyXfv9C+K/pC+67pC+27oS+075a+0L47+q" +
                "bo+2jPGrB/I/17YP9C+x7pC+17oi+075m+0L4efaF9+f8X29en" +
                "L7Tvlb7Qvjf6Qvve6Qvt+6BvEpJ5Aj36ofM=");
            
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

        protected static final int[] rowmap = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 0, 5, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10, 0, 0, 0, 11, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 12, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 13, 0, 14, 0, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 17, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 18, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 19, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 20, 0, 0, 21, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 22, 0, 0, 23, 0, 0, 0, 0, 0, 24, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 25, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 26, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 27, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 28, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 29, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 30, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 32, 0, 33, 34, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 35, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 36, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 37, 0, 0, 0, 38, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 39, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    protected static final int[] columnmap = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248 };

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
            final int rows = 1218;
            final int cols = 8;
            final int compressedBytes = 129;
            final int uncompressedBytes = 38977;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrt3ckNwCAMBEBKp/OlA3hZFvJMAfn4jiIludoLBov6QP6qL4" +
                "DP+t/r+fqv/AIAsF8CuO/cl4g/ANPmk/1K/ni/If7ii/wA9as/" +
                "IL/MT1AfyB8AMF/NVwDA/gEAAPZfAP0TAAAAcN8DAAAAAAAAAA" +
                "AAAECt6v9Ptn/ffwBBk0qA");
            
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
            final int rows = 1218;
            final int cols = 8;
            final int compressedBytes = 71;
            final int uncompressedBytes = 38977;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrt1DERAAAIxLCXjnNwwMgxJBI6NAEAAAAAAAAAAAAAAAAAAA" +
                "AAAIA/elcKAfgvAAAAAAAAAAAAAAAAAAAAAACcGce8PsM=");
            
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
            final int rows = 826;
            final int cols = 8;
            final int compressedBytes = 60;
            final int uncompressedBytes = 26433;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrt1LERAAAEADGj25wNFHQuWeC7jwAAAAC4qVl+7wP+BAAAAA" +
                "AAAAAAAAAAAAAAAAAAAADAUgNOaV4k");
            
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

    protected static int lookupSigmap(int row, int col)
    {
        if (row <= 1217)
            return sigmap[row][col];
        else if (row >= 1218 && row <= 2435)
            return sigmap1[row-1218][col];
        else if (row >= 2436)
            return sigmap2[row-2436][col];
        else
            throw new IllegalArgumentException("Unexpected location requested in sigmap2 lookup");
    }

    protected static int[][] value = null;

    protected static void valueInit()
    {
        try
        {
            final int rows = 39;
            final int cols = 125;
            final int compressedBytes = 178;
            final int uncompressedBytes = 19501;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrt2ksOgjAARdFO/f9QBF0P6+rSXYKSOGnfyZ0SBu8EEhpqqV" +
                "9bNrWol8pPVzFnLuZiLuZiLuZiLuZiLuZirj+YD+vuupwt27z5" +
                "bKk4852l4syPloozP1gqzvxhqTjzraXizCdLec7VvfnNUnHmF0" +
                "s5b5fvczFX8+YvS8WZ7y0VZ/60VJz521Jx5ldLpZmvzb9RHTzn" +
                "J0vFmd8t5UxG3ZuPloozHyzl3a6WzcsHlGXxyA==");
            
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
            final int rows = 1;
            final int cols = 125;
            final int compressedBytes = 22;
            final int uncompressedBytes = 501;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNprYGggCN14GxhG4XCBQAAAwdF8VA==");
            
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
        if (row <= 38)
            return value[row][col];
        else if (row >= 39)
            return value1[row-39][col];
        else
            throw new IllegalArgumentException("Unexpected location requested in value1 lookup");
    }

    static
    {
        sigmapInit();
        sigmap1Init();
        sigmap2Init();
        valueInit();
        value1Init();
    }
    }

}
