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
final class BodyParsingTables extends ParsingTables
{
    private static BodyParsingTables instance = null;

    public static BodyParsingTables getInstance()
    {
        if (instance == null)
            instance = new BodyParsingTables();
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

        protected static final int[] rowmap = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 13, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 0, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 0, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 29, 120, 0, 121, 122, 123, 124, 18, 125, 126, 102, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 13, 137, 138, 139, 140, 141, 142, 143, 144, 145, 96, 146, 147, 0, 148, 149, 89, 1, 50, 34, 108, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161, 162, 163, 164, 153, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, 50, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 1, 2, 3, 0, 13, 4, 109, 50, 122, 123, 5, 124, 125, 13, 6, 28, 126, 171, 187, 7, 8, 169, 127, 128, 0, 164, 188, 165, 189, 166, 9, 10, 100, 190, 191, 192, 11, 167, 193, 50, 12, 168, 13, 170, 172, 177, 194, 178, 180, 181, 50, 77, 195, 14, 196, 197, 15, 0, 16, 198, 199, 200, 201, 202, 203, 17, 204, 18, 19, 205, 206, 0, 20, 21, 1, 207, 208, 1, 209, 210, 99, 2, 22, 211, 212, 213, 214, 215, 23, 24, 25, 26, 216, 217, 187, 189, 218, 219, 220, 221, 27, 77, 222, 196, 188, 3, 28, 193, 0, 194, 186, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 77, 233, 29, 234, 235, 236, 237, 238, 239, 240, 241, 242, 89, 108, 243, 30, 244, 245, 31, 246, 4, 247, 32, 248, 249, 250, 0, 1, 2, 251, 50, 33, 252, 253, 254, 255, 89, 256, 195, 190, 197, 199, 13, 200, 201, 202, 203, 204, 257, 206, 207, 258, 191, 5, 6, 108, 7, 259, 260, 34, 261, 96, 262, 208, 263, 264, 265, 210, 109, 266, 267, 110, 111, 115, 118, 268, 130, 132, 134, 269, 192, 212, 270, 271, 272, 213, 216, 273, 274, 108, 275, 276, 277, 278, 8, 279, 9, 280, 281, 10, 11, 282, 0, 12, 35, 36, 37, 1, 13, 14, 0, 15, 16, 17, 18, 2, 283, 19, 20, 284, 3, 13, 4, 21, 285, 286, 23, 287, 1, 38, 24, 39, 25, 221, 40, 41, 27, 29, 30, 288, 289, 0, 290, 31, 32, 33, 34, 42, 291, 292, 293, 294, 295, 296, 297, 36, 298, 299, 300, 301, 302, 303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313, 314, 315, 316, 317, 318, 319, 320, 321, 322, 323, 324, 325, 326, 38, 39, 327, 328, 329, 330, 43, 44, 45, 46, 331, 40, 41, 47, 5, 48, 49, 50, 51, 52, 53, 332, 54, 55, 7, 333, 334, 56, 335, 0, 57, 336, 58, 59, 60, 61, 62, 337, 63, 64, 338, 65, 66, 67, 68, 339, 69, 70, 71, 340, 72, 341, 73, 74, 75, 76, 8, 342, 343, 344, 345, 77, 9, 346, 347, 348, 349, 350, 351, 352, 353, 78, 79, 10, 80, 81, 82, 354, 83, 11, 84, 85, 86, 355, 356, 87, 88, 90, 0, 357, 91, 92, 12, 93, 94, 95, 47, 13, 358, 13, 359, 96, 97, 360, 14, 98, 99, 101, 15, 102, 103, 361, 362, 363, 364, 104, 105, 106, 22, 107, 108, 16, 365, 18, 110, 111, 366, 17, 367, 368, 369, 112, 3, 370, 4, 48, 113, 5, 114, 371, 372, 373, 6, 115, 374, 375, 376, 377, 116, 117, 18, 378, 227, 118, 379, 380, 381, 49, 382, 383, 119, 120, 50, 0, 121, 122, 123, 127, 128, 384, 129, 19, 51, 52, 385, 386, 387, 388, 130, 20, 389, 125, 131, 132, 390, 133, 53, 134, 162, 135, 391, 392, 393, 1, 394, 395, 396, 397, 398, 399, 109, 400, 401, 136, 137, 138, 139, 21, 13, 140, 402, 403, 404, 405, 406, 141, 13, 175, 142, 211, 198, 232, 233, 407, 143, 54, 408, 409, 144, 410, 411, 412, 145, 413, 414, 415, 416, 146, 417, 2, 418, 419, 122, 147, 420, 421, 422, 423, 424, 425, 148, 426, 427, 428, 429, 149, 150, 430, 431, 432, 123, 151, 433, 434, 435, 436, 18, 220, 23, 437, 152, 438, 242, 439, 243, 440, 240, 245, 441, 248, 29, 153, 154, 155, 156, 24, 157, 442, 443, 444, 158, 445, 251, 446, 0, 159, 447, 55, 56, 136, 448, 137, 449, 160, 161, 450, 451, 18, 255, 452, 162, 453, 7, 8, 57, 25, 26, 454, 27, 455, 456, 457, 35, 259, 458, 36, 260, 58, 0, 3, 459, 460, 2, 7, 461, 462, 463, 464, 465, 466, 467, 468, 469, 470, 471, 472, 473, 474, 475, 476, 477, 478, 479, 480, 481, 482, 262, 483, 484, 485, 486, 487, 488, 489, 490, 491, 37, 492, 43, 493, 494, 495, 44, 496, 497, 498, 499, 500, 501, 502, 503, 504, 505, 506, 507, 508, 509, 510, 511, 512, 26, 27, 28, 513, 514, 515, 516, 517, 9, 518, 266, 3, 519, 267, 520, 521, 522, 523, 524, 525, 526, 45, 527, 265, 528, 529, 530, 46, 268, 531, 532, 533, 269, 534, 56, 535, 67, 58, 536, 537, 538, 163, 164, 539, 165, 540, 167, 541, 168, 542, 543, 544, 545, 546, 547, 548, 549, 69, 550, 551, 552, 553, 554, 555, 70, 556, 71, 72, 85, 557, 558, 559, 560, 86, 561, 562, 563, 564, 565, 566, 567, 568, 569, 570, 571, 572, 573, 574, 575, 576, 577, 578, 579, 580, 581, 582, 583, 584, 585, 586, 587, 588, 589, 590, 591, 592, 87, 89, 91, 100, 59, 593, 109, 111, 594, 595, 4, 596, 170, 597, 598, 169, 599, 600, 601, 602, 603, 604, 605, 606, 5, 607, 608, 609, 610, 6, 611, 9, 10, 11, 12, 13, 14, 612, 613, 614, 615, 616, 112, 617, 116, 618, 119, 272, 110, 619, 173, 620, 174, 621, 622, 120, 623, 624, 625, 626, 15, 29, 627, 628, 629, 171, 630, 631, 175, 632, 633, 634, 635, 636, 637, 276, 638, 124, 639, 640, 641, 642, 643, 644, 645, 646, 647, 126, 648, 649, 650, 651, 131, 652, 653, 132, 654, 655, 656, 8, 657, 658, 659, 660, 661, 662, 663, 664, 665, 666, 667, 176, 177, 668, 179, 669, 138, 670, 182, 17, 671, 672, 673, 674, 675, 676, 677, 678, 679, 133, 134, 140, 141, 142, 143, 680, 183, 681, 144, 682, 683, 145, 60, 4, 146, 147, 684, 685, 10, 686, 687, 688, 689, 690, 691, 692, 693, 694, 695, 696, 148, 18, 149, 150, 697, 151, 152, 184, 1, 157, 61, 158, 159, 160, 161, 162, 62, 166, 170, 171, 175, 176, 179, 180, 183, 698, 699, 185, 700, 701, 0, 702, 34, 31, 703, 704, 705, 177, 178, 186, 187, 188, 63, 190, 191, 64, 279, 706, 19, 707, 192, 196, 197, 199, 198, 200, 205, 708, 709, 710, 206, 711, 712, 207, 208, 210, 212, 213, 65, 713, 714, 715, 716, 717, 718, 719, 214, 11, 215, 20, 21, 12, 720, 721, 722, 280, 216, 282, 13, 186, 188, 723, 189, 724, 725, 726, 727, 728, 32, 217, 66, 729, 730, 731, 732, 218, 219, 5, 733, 734, 735, 736, 737, 738, 285, 739, 221, 222, 67, 740, 286, 741, 742, 743, 744, 223, 7, 224, 225, 226, 745, 746, 747, 227, 228, 229, 748, 230, 69, 190, 231, 232, 233, 234, 749, 235, 236, 237, 750, 238, 239, 240, 751, 8, 241, 242, 243, 191, 193, 70, 194, 195, 752, 71, 72, 139, 75, 76, 77, 244, 753, 754, 287, 755, 196, 756, 245, 246, 247, 757, 758, 197, 198, 759, 760, 199, 289, 0, 200, 199, 290, 761, 762, 201, 763, 764, 22, 765, 23, 202, 766, 203, 767, 768, 769, 770, 78, 248, 249, 771, 34, 79, 24, 80, 81, 30, 31, 82, 32, 83, 34, 772, 250, 251, 252, 773, 774, 204, 775, 253, 776, 205, 777, 50, 77, 34, 254, 256, 35, 291, 96, 207, 778, 36, 779, 210, 780, 37, 255, 257, 2, 38, 258, 84, 260, 261, 39, 262, 781, 263, 782, 783, 784, 1, 785, 326, 786, 787, 264, 38, 211, 788, 789, 327, 790, 212, 332, 791, 792, 793, 265, 266, 267, 215, 794, 795, 217, 343, 346, 796, 218, 797, 798, 219, 799, 800, 222, 85, 268, 269, 270, 39, 271, 272, 0, 223, 273, 274, 801, 802, 803, 275, 276, 277, 278, 279, 281, 282, 283, 40, 0, 284, 288, 1, 295, 296, 2, 299, 301, 40, 302, 307, 315, 317, 320, 321, 41, 326, 327, 329, 330, 331, 332, 334, 336, 337, 338, 339, 340, 341, 343, 344, 345, 346, 347, 1, 224, 348, 349, 350, 351, 354, 355, 356, 357, 359, 360, 362, 363, 364, 365, 322, 335, 41, 358, 86, 2, 42, 366, 367, 225, 47, 50, 54, 55, 70, 71, 72, 73, 74, 75, 76, 77, 78, 84, 368, 369, 371, 374, 375, 376, 377, 378, 379, 380, 381, 2, 804, 382, 384, 385, 386, 387, 389, 805, 390, 806, 807, 392, 393, 808, 809, 391, 394, 810, 395, 396, 397, 811, 399, 400, 812, 813, 43, 85, 814, 815, 816, 817, 818, 819, 820, 821, 822, 823, 398, 401, 824, 825, 826, 827, 828, 829, 830, 831, 832, 833, 834, 835, 836, 837, 838, 402, 839, 226, 0, 840, 403, 841, 405, 842, 404, 843, 86, 844, 845, 846, 227, 228, 406, 407, 229, 408, 292, 409, 410, 847, 848, 411, 412, 413, 414, 417, 418, 419, 849, 230, 420, 421, 422, 423, 424, 425, 99, 426, 427, 44, 428, 850, 851, 88, 429, 430, 433, 3, 231, 415, 416, 435, 431, 4, 437, 852, 439, 432, 234, 235, 434, 440, 441, 442, 443, 853, 444, 854, 445, 446, 447, 448, 449, 450, 451, 452, 855, 236, 453, 454, 455, 456, 457, 458, 459, 460, 461, 462, 463, 464, 465, 466, 856, 237, 857, 858, 45, 467, 90, 468, 469, 92, 470, 471, 472, 473, 859, 860, 861, 474, 862, 475, 476, 477, 478, 479, 863, 480, 864, 238, 865, 481, 866, 482, 867, 868, 869, 239, 483, 484, 485, 486, 487, 3, 93, 94, 488, 870, 489, 871, 872, 873, 1, 4, 490, 491, 95, 87, 492, 493, 494, 88, 495, 874, 496, 497, 498, 499, 500, 501, 502, 89, 504, 505, 293, 503, 506, 294, 507, 508, 875, 509, 510, 5, 876, 877, 108, 46, 878, 879, 511, 512, 513, 880, 240, 881, 882, 241, 514, 883, 244, 3, 884, 885, 246, 515, 516, 886, 887, 517, 518, 888, 889, 519, 890, 891, 892, 520, 521, 14, 893, 894, 895, 896, 897, 522, 347, 898, 899, 96, 524, 523, 525, 900, 526, 527, 97, 98, 101, 901, 247, 902, 528, 529, 297, 903, 530, 90, 904, 905, 906, 907, 249, 252, 92, 250, 908, 909, 910, 531, 911, 4, 912, 913, 914, 915, 916, 93, 917, 102, 918, 919, 920, 532, 921, 5, 922, 923, 533, 924, 925, 94, 7, 926, 927, 928, 104, 929, 930, 931, 932, 253, 933, 934, 257, 95, 96, 935, 258, 936, 259, 534, 535, 536, 937, 938, 939, 940, 537, 941, 942, 943, 261, 348, 1, 264, 105, 944, 0, 945, 946, 947, 106, 97, 101, 102, 107, 108, 109, 948, 115, 117, 118, 121, 949, 950, 104, 951, 47, 952, 953, 298, 954, 539, 538, 540, 541, 542, 545, 548, 300, 955, 122, 956, 957, 109, 48, 958, 49, 959, 5, 543, 553, 50, 546, 123, 551, 107, 550, 552, 110, 554, 960, 268, 349, 350, 555, 556, 560, 269, 274, 566, 961, 351, 962, 270, 963, 352, 271, 353, 964, 568, 965, 575, 577, 966, 578, 967, 580, 581, 585, 587, 591, 593, 595, 968, 272, 969, 273, 278, 970, 971, 972, 51, 558, 973, 974, 596, 975, 976, 977, 978, 979, 0, 980, 981, 982, 983, 984, 559, 561, 562, 563, 985, 597, 599, 600, 986, 564, 987, 601, 988, 989, 602, 990, 991, 992, 993, 994, 995, 125, 996, 997, 565, 998, 999, 1000, 603, 1001, 1002, 1003, 604, 567, 6, 7, 606, 605, 607, 608, 1004, 279, 1005, 1006, 1007, 281, 609, 1008, 284, 1009, 285, 1010, 610, 569, 1011, 1012, 1013, 1014, 108, 570, 571, 572, 573, 574, 576, 2, 1015, 1016, 1017, 111, 52, 579, 583, 584, 588, 53, 612, 1018, 613, 618, 1019, 620, 1020, 1021, 54, 589, 1022, 286, 614, 1023, 1024, 615, 1025, 1026, 1027, 1028, 1029, 1030, 1031, 1032, 1033, 288, 592, 1034, 1035, 1036, 1037, 616, 617, 1038, 1039, 619, 621, 127, 623, 624, 1040, 1041, 1042, 625, 626, 1043, 0, 1044, 1045, 1046, 8, 128, 129, 622, 598, 1047, 1048, 627, 130, 1049, 628, 1050, 629, 135, 1051, 1, 1052, 1053, 630, 631, 632, 1054, 633, 295, 1055, 1056, 634, 635, 636, 1057, 136, 137, 1058, 296, 359, 1059, 637, 1060, 646, 1061, 638, 1062, 1063, 652, 639, 640, 1064, 1065, 1066, 653, 641, 109, 9, 642, 643, 15, 1067, 644, 10, 1068, 1069, 1070, 645, 1071, 299, 1072, 647, 138, 1073, 301, 1074, 315, 648, 1075, 649, 1076, 317, 650, 326, 320, 1077, 321, 139, 151, 152, 651, 55, 654, 1078, 1079, 1080, 1081, 1082, 1083, 655, 1084, 656, 1085, 657, 327, 658, 328, 659, 1086, 660, 112, 1087, 1088, 11, 661, 662, 663, 664, 1089, 665, 1090, 666, 1091, 667, 668, 331, 669, 114, 1092, 1093, 12, 1094, 670, 671, 329, 1095, 330, 1096, 672, 153, 1097, 1098, 1099, 154, 1100, 155, 1101, 303, 673, 674, 360, 332, 304, 1, 1102, 333, 1103, 1104, 115, 1105, 116, 1106, 334, 1107, 336, 1108, 56, 3, 4, 675, 676, 1109, 112, 57, 337, 1110, 338, 677, 1111, 9, 1112, 156, 678, 680, 1113, 1114, 684, 157, 366, 685, 686, 687, 688, 689, 696, 113, 364, 1115, 339, 117, 1116, 118, 1117, 119, 341, 690, 1118, 367, 340, 1119, 158, 1120, 1121, 691, 1122, 1123, 699, 694, 159, 58, 697, 160, 698, 342, 700, 59, 701, 161, 702, 703, 120, 704, 705, 706, 1124, 707, 708, 709, 711, 13, 1125, 710, 712, 713, 1126, 715, 14, 717, 1127, 718, 1128, 15, 16, 17, 1129, 719, 1130, 1131, 1132, 1133, 162, 720, 1134, 1135, 721, 722, 1136, 723, 346, 724, 727, 305, 725, 726, 1137, 1138, 1139, 728, 729, 730, 731, 2, 114, 60, 121, 732, 733, 734, 1140, 1141, 1142, 1143, 1144, 1145, 735, 736, 1146, 737, 738, 1147, 344, 61, 62, 739, 740, 63, 1148, 306, 122, 124, 0, 125, 126, 741, 345, 1149, 1150, 1151, 163, 742, 743, 744, 1152, 745, 164, 746, 1153, 1154, 747, 1155, 749, 751, 753, 754, 755, 756, 757, 758, 759, 760, 1156, 1157, 351, 354, 761, 762, 763, 368, 764, 8, 165, 765, 9, 10, 766, 1158, 767, 769, 1159, 770, 1160, 768, 1161, 128, 771, 772, 1162, 166, 142, 1163, 1164, 1165, 355, 1166, 1167, 1168, 1169, 357, 359, 773, 360, 1170, 774, 775, 1171, 129, 1172, 1173, 776, 1174, 18, 361, 130, 1175, 1176, 777, 778, 779, 11, 1177, 1178, 1179, 19, 362, 131, 1180, 780, 785, 1181, 363, 2, 167, 172, 170, 364, 365, 308, 1182, 369, 1183, 1184, 371, 1185, 1186, 132, 1187, 133, 1188, 1189, 1190, 1191, 1192, 309, 171, 782, 1193, 310, 115, 374, 64, 311, 1194, 781, 783, 784, 786, 787, 788, 789, 1195, 1196, 791, 1197, 792, 1198, 117, 65, 790, 794, 134, 1199, 136, 1200, 1201, 1202, 1203, 137, 1204, 795, 796, 375, 1205, 1206, 1207, 1208, 1209, 1210, 5, 16, 1211, 1212, 1213, 1214, 797, 800, 1215, 809, 798, 1216, 1217, 1218, 799, 1219, 1220, 1221, 1222, 1223, 376, 10, 793, 11, 12, 1224, 1225, 801, 807, 808, 20, 21, 175, 810, 1226, 176, 1227, 66, 812, 819, 824, 837, 839, 1228, 841, 843, 1229, 840, 842, 845, 1230, 1231, 1232, 312, 12, 179, 180, 1233, 846, 847, 848, 13, 849, 851, 852, 1234, 370, 1235, 377, 379, 13, 1236, 14, 1237, 1238, 853, 1239, 854, 855, 856, 857, 181, 1240, 380, 67, 858, 1241, 1242, 139, 1243, 859, 15, 1244, 22, 860, 140, 1245, 1246, 1247, 1248, 1249, 381, 861, 16, 1250, 382, 141, 1251, 1252, 1253, 1254, 1255, 384, 862, 1256, 385, 386, 1257, 387, 1258, 1259, 388, 1260, 1261, 1262, 142, 143, 7, 8, 863, 864, 866, 867, 389, 868, 313, 1263, 1264, 390, 371, 1265, 1266, 372, 869, 14, 871, 182, 874, 1267, 68, 1268, 1269, 183, 184, 185, 1270, 1271, 187, 69, 872, 873, 1272, 0, 188, 875, 877, 878, 879, 882, 1273, 1274, 1275, 883, 884, 1276, 1277, 1278, 1279, 1280, 1281, 1282, 15, 886, 1283, 1284, 889, 881, 885, 1285, 1286, 1287, 892, 893, 894, 1288, 314, 203, 190, 888, 1289, 1290, 890, 897, 899, 900, 1291, 901, 1292, 896, 374, 1293, 1294, 898, 1295, 905, 1296, 1297, 1298, 396, 144, 1299, 1300, 1301, 23, 398, 1302, 1303, 1304, 1305, 402, 407, 902, 397, 1306, 1307, 907, 1308, 1309, 1310, 1311, 408, 412, 904, 399, 1312, 1313, 1314, 191, 145, 1315, 1316, 1317, 1318, 316, 318, 319, 1319, 70, 400, 401, 323, 915, 906, 909, 911, 912, 914, 913, 1320, 204, 207, 411, 410, 413, 208, 1321, 1322, 1323, 138, 916, 1324, 1325, 917, 1326, 1327, 1328, 1329, 1330, 918, 16, 922, 920, 926, 928, 1331, 921, 929, 1332, 923, 414, 1333, 1334, 1335, 924, 925, 930, 432, 931, 933, 937, 934, 939, 436, 1336, 1337, 438, 440, 935, 445, 1338, 1339, 144, 1340, 941, 446, 936, 448, 1341, 1342, 145, 1343, 449, 927, 938, 942, 450, 1344, 324, 325, 1345, 943, 1346, 375, 944, 454, 29, 1347, 146, 147, 1348, 1349, 455, 946, 1350, 1, 1, 945, 947, 950, 948, 949, 951, 1351, 1352, 1353, 1354, 952, 953, 1355, 954, 955, 956, 377, 1356, 957, 1357, 1358, 456, 1359, 1360, 148, 1361, 1362, 24, 1363, 149, 1364, 1365, 26, 147, 328, 333, 335, 457, 458, 958, 378, 1366, 150, 151, 155, 1367, 1368, 1369, 1370, 209, 156, 1371, 959, 960, 1372, 961, 1373, 962, 1374, 1375, 963, 964, 965, 966, 967, 210, 968, 1376, 1377, 27, 459, 1378, 1379, 28, 460, 1380, 342, 1381, 361, 969, 1382, 1383, 1384, 212, 213, 970, 18, 216, 217, 229, 1385, 1386, 971, 1387, 972, 973, 974, 461, 1388, 1389, 462, 463, 1390, 1391, 465, 464, 254, 255, 256, 467, 468, 257, 975, 976, 977, 258, 259, 1392, 469, 1393, 1394, 479, 1395, 373, 480, 481, 482, 1396, 1397, 978, 979, 980, 1398, 1399, 1400, 1401, 1402 };
    protected static final int[] columnmap = { 0, 1, 2, 3, 4, 5, 2, 6, 0, 7, 2, 8, 9, 10, 2, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 1, 21, 2, 22, 6, 23, 24, 25, 3, 26, 3, 2, 27, 0, 28, 29, 30, 31, 6, 19, 7, 32, 33, 0, 34, 8, 0, 35, 17, 36, 0, 3, 13, 20, 37, 26, 38, 39, 40, 41, 42, 43, 0, 44, 45, 30, 46, 47, 41, 38, 1, 48, 49, 3, 50, 42, 51, 52, 43, 47, 24, 53, 54, 55, 56, 3, 57, 58, 0, 59, 60, 61, 8, 2, 62, 3, 63, 64, 13, 48, 7, 44, 65, 53, 54, 66, 67, 68, 69, 70, 71, 72, 73, 66, 74, 67, 75, 68, 47, 76, 69, 70, 77, 0, 78, 79, 0, 80, 64, 81, 82, 83, 84, 84, 4, 85, 0, 86, 87, 3, 88, 1, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 8, 85, 90, 100, 101, 102, 5, 94, 103, 104, 100, 105, 101, 4, 20, 2, 17, 0, 106, 26, 107, 104, 0, 108, 86, 3, 107, 109, 110, 111, 112, 113, 0, 3, 114, 115, 116, 110, 117, 118, 119, 16, 120, 6, 121, 10, 122, 123, 124, 125, 126, 127, 128, 129, 0, 1, 130, 131, 26, 132, 133, 128, 134, 0, 135, 136, 0, 137, 138, 118, 139, 140, 141, 121, 2, 142, 67, 143, 144, 145, 146, 1, 147, 124, 3, 148, 125, 0, 42, 129, 149, 150, 130, 6, 4, 151, 7, 0, 152 };

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
            final int compressedBytes = 2885;
            final int uncompressedBytes = 38989;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtXb9vJbcRHi72BJ6sA/YE4aAYiEGdI+A5lZE+xuqiwtbZiG" +
                "KccTAcIA9BDKiIERcpr2AEJVC6g5MqlWHAKVKk8T+gwoULF5c6" +
                "rgKkcZr8Cd63P97bt0suP5Kz+95dsoBlP3necIYcznwzQ66+vv" +
                "PJD3//2VlyS8wf3su+/9ZD/fjJfn716Nvrm9d+9crTVBPR1+8U" +
                "NH872y9oPujS/LqhWfD59uze8YJmv6C5fvzk3bH4cI21bfIw8S" +
                "me+l+rJ+t83jaZb/Y+uS/mhR0SvXQv2ZMP9c7sXdLp/WsiRdl2" +
                "2tidcqzhvXPHLg+7rQI03/z0i79//q+ze/++Pf/nez/68fu/uf" +
                "7Fk18++MPP/1PQfPTGP0qatP6nMpwsL36cF/az+A9FuzfT2w9A" +
                "U9qPOts5XtgPFfZzvTPb33b7eVH9Kuf+mkhmkpQUP5Olg5SFwT" +
                "efVO0zJ9Q9bTvrWo58+Qs9uTwvbFzeMpreI/q/mjJe/C/bWMcn" +
                "ZM0P1cVRrvi+wtjDNFPtHc51R3SfKKYcFGt1tcIthQ/NHmXF8p" +
                "W4JdE4bkFpPitlfrgNuqeFo0ibGJG9Xvw4PH+9+qxE8jtW3Wus" +
                "VWF1qrB6F2t9886fGpz5wXv7S5xZ8mlwpsc8lxj7oJ6fZJP7Ys" +
                "QczfBcuvI4hAbJrRCaLyuaCmMnFcZe0TxlpeHKB7nG8uVzUPN5" +
                "ECAz8iC+t/AHSYMjc0se16Y5iaPp5Yw00ljdRxlsnmusKXPPhs" +
                "+QX92EPEN82jQHNU3SoUFyKy6ajl7vl3o9qPV6w+jDjTRcYwXp" +
                "RS0asutOY8nT/7CWfVLjD5XNZ3rkO1KBHg+jS9H/Z8mpy3l1PU" +
                "BeMCYNBfAx2kZvLTarl0NmFz6E8q8YDInkF40l6ZUdpd3YhPCZ" +
                "kqbre1JDPA0diwLkYcutWnxs6753m3avLgv1xTyn7O4J0c9+m+" +
                "VX51/RzWtvv6Lr+ejnVqqTWxU0O678C6FB7JBrrL237Lqf1roj" +
                "uR5XztimOahpelgCqI/x0Sge3sh3qjho+j+54auplac9fuVr2M" +
                "FFg8scMpYcHGs1oKIuozSpIrVYOqtBPplRZtkK+WlfS9lwT5a/" +
                "NfDJi99r2dDcLXZY5eCyrj+NtcNUCpGm5YTpci+L0v/I4vO82c" +
                "sgbvF7lDef9roDc+jmg+muePagl09IBmys+qbs65750QBjQfuU" +
                "aZ7XkbQM93Vc/gdYSfN+b/yP8JgfWpsfstmh0be0xgJsFZEZCE" +
                "6dmgxVNRm5hrUgvbjWlGvdQ7wbIrPdR4lYvZB5jvDzMnDv9CNf" +
                "7FqYx0L8IXXjBQviisOHItDicgDX+eHMXWXCNlK417S/v6wrrD" +
                "1qLWps3JIDOJO8YiWy39nsRy6dsB2vKv99apgfsN90ydFv4urL" +
                "HB9+JNJbJN68Elp/779EPzl8dkRKnqf08d3kjyoO1+XetcH2HD" +
                "7uzeGzXj8F6Snklj4IWssE7DBRjj4I6GmcfNjOBDKf9wPqCTtD" +
                "9YRNyDzUTyltHna/AzicKSeaslcSNJahVxLKh4L4KDReJMAKJF" +
                "iMw0CJLe4E6RVoP0ifCDF4rrNziF7wuQKnXkDg6YOvoahvpwlZ" +
                "L2NMcOMWrl4SV39QA3UJECMF9UEorOdSyT7QJzL1kmw+Q6+Wzc" +
                "Rnq2gYe23O9Qpd96AcjcL3oHf+hdQBxsEADt+7tgdTXx8V27dq" +
                "enZIX89EQ/3eH9Jrc/YHuerzYC9yMppAmR9tWJ4djn4u4luQHq" +
                "uJhjo0bP1lpl5tezYTuzdy98iwfSEWX5D5rql+eLPUy3EOBPSr" +
                "ItDPd/DYrPGmWrfrq6lvbALGguSJCky511hIXQuqbwTw4ZLZZ9" +
                "ZSXz4KwPwjy9yenxmJ4sPF7vwvi/76X+mYxJ8/zT5NX93Pkpxu" +
                "OOUB8ZhXn0i5u0hRtgH2boA+mledX7fIpYiFnEP7K/Wwn8BzMs" +
                "j9FL7aOzIpc549OKt7W3mxcKd0WXxKMpmpQtenRYi7MfYmjF1n" +
                "wHghPnMmv8E0P3SxzscQB5e9AC00WXoBx4cfVjSXNc2DmoYMNN" +
                "pOk8pjKmgo14t9+nExlYfPmn16d4Uh80reHm7xwjbQWIs5dNg5" +
                "ohciMzKHiF5Yf+dD85p6rhdIU87zm4t5jtCr4ZP3+cz57fDIaR" +
                "te8wPJ456fke2HIubn3LhPBbZPwb2s3TJTlG9BfB2G4LjzAnvr" +
                "ATv7BOR61wANzYD4PvPHAKE00JkBLqzOhOeh2M0V3734xNFAZ4" +
                "Smm2dkX2C1UwA/yyVM16uzfGToKZiq1yrxwcZcMi+mYbFKkkR3" +
                "TbMwGoqjWZ5Ds9eRMHmWbnporJ6tDviNoX0KyzP8XKzzWeFwz/" +
                "nh6oNMaIdIbDLHAqSuJbfZ/2Bn54DzmbOyBjuT+cuLTz/QQtFT" +
                "XeS5at9lG/7xC6oJK3eFYFIaCuUjB2k2bGNTYhu+evioeo3jx6" +
                "C7HsHyhNlG6vQb/n41WObtstWgWqXF1yXltxO97nvTnLTCB9pI" +
                "zhj0hK6XtX64IfuxygPmy+688ppOpHl/ndOqfyp1Un6/YC9EOW" +
                "pO+TXdyNM9L71KfNg599WdZ4DGap356HvQtKZAfV6uAXCx9ikf" +
                "Xvf4egKF+w1o1mHsF4v5CcU2jv4FwAdyibP1xbTbqgDsUGB1ia" +
                "F1d+sO3YnwkznOh2M0PGc8psQJ0Dk0930HrliJvO+i2Z5esCP0" +
                "LN+uWS8xAjb2xVF6dNw7YR2ADbMh2CYArwbaD5bvuIePOFPhPT" +
                "98Nc8tq31B6x7AB8lPxRAGGOCDxZ0Td+7ZucvgF7+WGJvLnsFY" +
                "mdfs1vPBtJTHwUcs+ehqOcq7iPut89jKuy7Bn1dqJhpWf2g0SO" +
                "7aTpBttHI9hM9Icdm25Ttcw2pW0D3WHo0Bs3ndzxUR91i5fC9y" +
                "j5VrTcH97nv+x3x/EOvLIO+9BHxmwL4QLHNo0V268SoaL9qkpl" +
                "jQLRoH2gZ0n1G2S8LdHjTOB3nPGxqbXPJw7QtwfjadC7ttNTQ2" +
                "BdqhGsVHAXywv+8A1CrrfRx/B9M9FvR+JIVxNePw3Edmrjuz6J" +
                "l/TfHvTUVsA7xbeljX1XvR4nQ5w0qs5Rf2mrmLZrp6OPHsHeg8" +
                "0oS4heveKBZSlRvkTfhOwhHvaYbdYx3r7GVoviMg3Z32g63pEV" +
                "OOZoSQ0qKbleYoe1sUNOW5L327yG9OSAtSMksXu7YniAZmX8fV" +
                "M3E+8e/PJL55hmtWCWJz1rE870lpz7H8aYBzTR53+uLf1ozIw1" +
                "evO+G4C8PYa+PCvSfuWhPbWJPp/nzTJHFx0MtH/X8tgmk2EAef" +
                "K72mPDMwLQ0P3nhBadhiAZTDMtH46q6pW+qAdeean6PstMHzRA" +
                "s8n5d4fmGECzx/PybXG7qPH1PHhsYC4uDEfViPuOxBE+17ofwi" +
                "Lk/x8IfD95smzK143zEej6NWpF3dJ8lPDfbjvOPMZvNc90HC+7" +
                "Dj3BuF+Fxw4Sjbei1ztN5dcvN9/Bh/2BrLcmffcW+9u3egd1kA" +
                "dS2HPScefj5nyvGDMG3m2u3JoO72sZo5HOLTfYZoBvjA7yep3s" +
                "PQXa+eXiANWWnMoCXEH24rDW0Zn+eJBonvrL4FfD/JkM0HYZLe" +
                "O/zRdx/hukfND9d7abh8ywjx1Bq7PXHdAI009QI6/WWExl2Fj3" +
                "h3n7de5dnvWwzvnDFjCf+8EnnnDNhTSFx7pzuWrd5ikblVTxCA" +
                "PBuo8w/mFzx4bFXm0dugu+ffL/CV2fZ3u7RnXrmFNMprDv1qRM" +
                "jfpoykwWo77toFc99h+F06HnmKZ77c5+N+bw+2dy7c78mZMHZP" +
                "+XeFYN8i4D0YVTPn6wUoHr/KdceZvZ+7JTRcNdjvAGVOuJU=");
            
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
            final int compressedBytes = 2691;
            final int uncompressedBytes = 38989;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtXb1uHDcQHi5WAiMrAKUYhhIgxsqJADmVnyBYOSpsKQEUw4" +
                "BhpLkiBlwkgIuULhhDAZTOcJXScJEiT3FFihQp/Byu8gjZvd/9" +
                "4/IjOcs7yd7C0p1H5HBmOH/kzBItHr38lSRR16dk+kNQ+8Fg8u" +
                "J7LecwO0R75c/rpBZ/yDeXmP5Ywqjpb8pvXT0PMk5cOvPAADj/" +
                "+92rr37762R3Q4x+uKE+v3+qHz3fzc8fvrsY3/7p5stUozSE6J" +
                "NZMcXwsY9DCFUg2QDmgmDoyLB3ztp7JwxnnKeywrG0RTe2cT4u" +
                "xnl3cuOgHGe3GOfi0fMHs3F+nI9DGbB4OwzXXMja0+pqVamWVM" +
                "nLQkkWEyQa50U3PtIgzdqE83j71S2RnWweEF27Qdvy9GLzcJd0" +
                "euuinEHBe5lNJ0SeK+nUh2kTpvifGXOmNqX4Ns07KN3UY5cbZr" +
                "n2hj0t1q4zeH/10lBnPviYx+Gay80qmDcYYuNwPyHcDuI2TqzR" +
                "PkXsoBUmlfuUpiTylLRWd4oV7p0VEiELpo9E8utUsqUoYUpB0F" +
                "TCUBuGay7IN+7W4dJfDIPl0M4Ls8TkgPbPLbYb8yxTyNJ47/ew" +
                "da0Q5qqua0gYHz9cVINOB3nG4mXfPVgXgKmt1FJVxin3jRKqEj" +
                "NLnUz+rKCHEBOy5JRf0Fgeb1tlKPe1F49a9uItb1zJD3N592nH" +
                "XNcL0TivxCkpqYeqkJZqnBLACw+NLuvjiNmX6RA85ZqLZxwoPg" +
                "XzP/b9fmjyjV13QB7gJ5g/9tPQO4sXiE/d34B4kXXBtPNsBr7P" +
                "/eeDvZ9FukHi3rnQ+tP/iL7Ze7tPmTxL6dlO8nvGGjvg+10Ae1" +
                "B46A3n/IaDbwzTx2SbuGjYyBHlZY6o2Jxqonu3xrAvgY2D8GLN" +
                "8myYPkT8KIezCZa4u99H8tFRYtC4m4eGA/haemA9hshYTJiuR3" +
                "ntr4H8XuG7353X9Rig4eN+H6lX5pOK3Kr5P9kAshqQ2/HJ27Do" +
                "OvM4wrzfE8B2J0CckvSuazrOJxN8pMHP7MFHLoymNvl1pRTYvZ" +
                "YSQkgSTV6cOfJ0Ng71jWOfC9PPyFzzL7nw0RV8mvwCaCjrMMu4" +
                "wAsfatNnNXl+aF2IHCI+G+IB8/h+tJV16XkpvD1yC0zuqI3Dbw" +
                "YgMMPrZ6IXNnuKjcPlz3vHcSZbEETDyXn36CTZKM+7k215qjcP" +
                "HzTPuyPGO1DOE5jLIdazbBhormQ+l5jNRe25kqxBraYczu8ebM" +
                "zvHujOuweA/WLiBSrPtpxD3YecsiTxyUs4r2vq996d4fy1wz0Z" +
                "BGeAFxA+bvneHn+Mx+b+PZVDg07oi99Fk7iiI9/rExYg43DNxY" +
                "VPtHEC9ukgsdW4Kj8GPQbFp7773cPmRs3FLc8QW1w/Bhie22U3" +
                "t+hMCehVNLvdQkcv/zJVHSGrbk6gaHaM2sAnDTypAHAOuFsYO+" +
                "fJAYPcAwnHJ/6dCnecoTsDwL5A7jmE5Y2d7zDYHq6z7ACZ97iv" +
                "Fc+nTUlspnNhmd15uzP9nC3vqvHERNhcdvps36et8xflTc9RTm" +
                "rniOj7X1R+fvYPjW8f31zwVHjSWUaEcZexiLyIKRts53pYvMOU" +
                "b8lAtd+hvlZ0n8SQy+1MFXH4z5NB884/HXvkKnvmypEcEXB313" +
                "4HGLonHFW38PHLDhPfx6451Liv5ZtjxOdKgukccy5tnusyyqE3" +
                "DA0zV34Z1v4BZl1hsokEPU1GfxQGJvmz+CzOX6vX6Re7lOTcth" +
                "KEER/4dXlgYtplj3FEcJ4mQt7Pe399gLnyMIz+j1WvIr4El7+x" +
                "buNgtnI6pczoeHLnISGpsun1rWRcz0uY79hfWfrIEc/aERoezr" +
                "7Nt6a8EJSoJi+49DNQW+F77zQMn77nKWvepg9mUQ+ihSZTPQgw" +
                "zr76VhR4Tu6h6Y8KFh6RFsU4KqUzNRcKYO0wPgLOA2jzXE/qc9" +
                "2dzUXLudYNBslDIuOk8oAKGMp1mY96Vvzl3tt5PmpnkbPab8Kc" +
                "NWEgnMPyLdVPLGtfwlAovyb06RsnlU+sdObiF+O6AHz2WfBh1Q" +
                "n2XC7g0iH+vERAXGqudZi9APL80Pmphz3tj92MxEby4W51rH3P" +
                "oTuMgNfl4Y8h+HjkhKn7QCbAdqu57ebjxVXNgYyQvcxzFtl9n3" +
                "9162LMI+mlHyfm7lz90MVhrjRCzkoaFXIOzOzkr5aqkbrrbhQ/" +
                "TC/yK8gtj0y1OZ5n0H3rAuiDxXGm2hzFoKM8/JbLp1e5akb4ak" +
                "/ySwjDtK5DKos2D2X+WfnpSy0yeqlVlma7yk0nMK7rvc2LrhsM" +
                "V+4LkB+unrqQD+CtD2PqXnLmRUSYuH0P7PjkZf3FnXtJIRzXUt" +
                "qmU70pd4vvy/oL6q/pk6uj80Lb6T4/09qfFh9nbfq4In2N0HUB" +
                "OEe857luMpa5a9sE1nV+MBb7jt2rBPxDrJcgS19Zw54s6Z+40X" +
                "lm4+o9KKR7rsCJX/35sfb/yAFhwvFx6HdR+zTsutYNxkbnOn26" +
                "x0Fg6s9OpaHO0OsKm8s3/8Ovn+cg/fccumRe1mQeOKOXNYVRN7" +
                "25i4/N1XkxYn7D6z6/2/tuGr2CtY/8mIjYzEfxx2jYGRnmj1VB" +
                "575os7+Nn9Vy1xsu55V9PTYhOQR6mcLyY9PhXHNldYdFBOUBBK" +
                "APfXjqp9ExGNFvC9qyythvx72fJ1CLx9bnpJsTw9SMxLS5wNoX" +
                "Z6yp0LQ8Y6XqGSvkh/Pl0HzPIll6noM14NK9LyjQlw+K8TNfnI" +
                "fLFQhQSdl6EuKyMaVzV49EwBbAdtDW1xGSVckj85g9tfa0BPsa" +
                "Ab4N0NcI7Ulo70NlHwfpx+iPz1B8D5GNjj6TPT0t2XpZIP2s2N" +
                "euTfgE9BFq+xJvJnm202a/glqe/81J0tXTwOksYKvbfgl3Wzmk" +
                "PfXNs/nGlfx9D9bszhLY09shlnHeO9L1nVxIn8nI/SEXPRuPwP" +
                "6QmbdvY7dNcAyibPxyjndMtpupZ6OHnrfAmMdxyvOHwUgnXafX" +
                "AsZXNlpyaOdXQM1RR+60/924YO50Ir16GUKkfnkJ6s4jvR/vhu" +
                "OmYVAeAOw/BvVaBOQw4B1zkM9vHqevP0laI2LI2agZZ7cciKuO" +
                "8jiP4/GjkjoiompfcwePmat3KFv/MX9+Bd6DFQauZS4Of1DOE6" +
                "FPzJ6fGL94ZCP6XWKGWqHLiHNMGKR2CaIPk8wPfDfM3b6b7TLy" +
                "fljlZLuHj82dzgJc4+XAd5+ZLHzq6I/ZTQB8/9khLtD+MuZk31" +
                "PAJ4kAs6J3nwXqZ8CvQ94livSXCDoHqfrqT7vX7njnBKoBh/oD" +
                "BI1TOdta1sjram1739nWoLnlBpU7Th8d167Na+esx6deE8Nady" +
                "xaMaP7++Og2BOBifl+ajsMdp/NXiPPxq/CJ0msMs9UR+8zV/eZ" +
                "C3Baw+TXufnqYXaQ6b2uXv3w478rYd1gyPX9BZzvJtBLH99Qj8" +
                "8TF6y0hsUzp8fnQwJ14pivZR0HwRl7513EeJCptp0tHuSSDaRG" +
                "dRmbV/0xyXdnqXXeLUL5xR4zuj/tes+sXu9ZLFhW6z0NMLWaUK" +
                "61V5S6ttgCtE5zsd+b9ZVstZM4TH+88z9j6dTd");
            
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
            final int rows = 977;
            final int cols = 9;
            final int compressedBytes = 2389;
            final int uncompressedBytes = 35173;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrlXb1uHDcQJhdrgbYVYK0YhlLEWBkQIKfyEwQr24X/AlwMA0" +
                "aQRkUCqHCAFCldMIECKF3gx8hTqEiRwoWfw1UeIXt3uv8l5xvy" +
                "2/UJuULWWXPDIZfz982QZ0z7suMfxpvFy5nuV5Dm/Tfvvvr9r2" +
                "d71+zJ96+rL58+96/f7jVnrz6eX9x/c/dD6ddo7mzQ/AnTGFOL" +
                "8gRpysWb0h2YsjS2KY331YN2HfZH7adcY8yJLX4VF2PBx475mG" +
                "Y81piP6eBz3v7ZGu9MMX1/q6Ua/3vbVO3PAuaz/iqionkjvyI0" +
                "9XRnlCtEoXU2wWfhzHFg7qP5DNzkF++qJZrxs6rseH2qJXnshj" +
                "ylcl6IPAu9iM0LkVleH0xmYCxgUyB83CafqpubBeYl0Mjrg9gW" +
                "xEYFxlp5vf+sHevjszuHY/uz1451/vrty8uxfpjZHyfrMiaPim" +
                "Z9fRrVJ1g2ARpLXufb7TqdzTdX1apw9apql64Zf7jwufNy3TIX" +
                "wpIu8bGbtgXaG4ivxPYYuA3iMrP2ISZzvbrOdpPP4f5Ptrxm7J" +
                "Mz6/0X/xrzaP/DgandqDQ/3yr+uGRwNPt05rzcKtHMEpXpikHQ" +
                "ZZvEdVO/LPpJj9m67ybP9OHlM/1asQ+nhrBYlmTmXhamQGfngd" +
                "gvJg9gnzl+eQtkNr3oMpAX0OaO+m6b73d6Xmc1zUH1wrbyWGda" +
                "e3j9wrSRqbft+FU59oquNvFn0QBjN+y9AdoNeZ3DNCOZxs75dM" +
                "oj7eeiM47Sx3VFZ7yB8bECH2jPc+LepQ+u8xl1zMsvzcvp414n" +
                "e0Rs/7AC30leGZ37nMYCNFE+si5j+0ceK2UfRuPn6M5ZXZ9FzD" +
                "bi2/mM2O9T7bGh15CR59L4gLGxjS/hxe67e7Z+tnNozM07Ztc9" +
                "P9852jO+vHduJh6T4HckOxaLe6vZj1o5L3ANf1v/XJWkF7K+t+" +
                "lRMUuRjquqtUvVqB2rmuTmNy40PqWo13Zlp8yVpBbIWFDsJ+cp" +
                "0FiQ/1Lx8Vl8ZJn/nupOcW2sO0WrO37n6OVcd3B9x/goYtESmF" +
                "6pnnsKRi1v5iWlcct7t4EC8Ua5V2Xcb8YHWUMefhiG0mg4JK3u" +
                "QPIFAXmUdQcI8+y21Uiut9U0xerC2GW738zV3O6Us/eX9a8H0/" +
                "f1vP6ltocWw906/rp73dw4+62V3J40prp1bMy3v1TN2egfc3H/" +
                "xV2P83ka5vNYwScwL7U9RHAb6FlgdgyrtcmxlshnSHyehaEl4f" +
                "OFys42vY9VpGIOKgxW60/1NVZWLKHW5ZyxOmMAFk7r+pA5gHm6" +
                "DcxTzqmbRNuSoIOnMg2rXomCyzJuQ1mfw/0fpzU7b70Z1+weXt" +
                "bszHLNjgSI656773te83rljOZRx9yB565cQw8kBusYUdhXfj7H" +
                "aZdx9WOO7rhumrKLpMj0BUeAXeVgIEqYIsu2TOxzIJZ4c5dST1" +
                "mtbVmSnafwCSWwddGxeM30yTVwEhIl6dUvc/JuyHcPyIfnc0+7" +
                "se5lNYP86UmoLlOpdJCEh8P70PYQqyfGzxCungDbQnUcmwz8AX" +
                "ycWcFtwv0/1kRr66Q8F6NJrdnpcT8WjdtczSov1/PKp53Ub0yR" +
                "ud8e4IR+WgVNQaIx20EDnUGg2flab+vydTC4N5TYMhLPI6Y4L0" +
                "+BxkJoAv2i6ty8DvkCbb4D2HBN3gTJbBJwgC55Ir6SE/NP4o2T" +
                "UA2xiuG04S1QrAdQnorlwnZDsAO6PupIrMXMB5E9NlQfGq13Tq" +
                "aB+hgBvYD6D9HeOUovFmKgkWcKxM/MfhsEkxFpkB4q2afQzgFd" +
                "xeeFraFIQ82pK4BGyInmdYeylTnWay3GNgnxYfdL7o8i9T6BvX" +
                "Op5zjWtyHQZ4WGykJMQuKD9DVBvU9uaTy/EaVYFVII7J/JMAu1" +
                "L6sU00LCKhF5sB6qWg3/BDA0eX3S++v0eO8SH5+LvZPlgeMN2x" +
                "mLArUk0lg99PKZQD8SjOUS8ncEz2T1I9HsD0DD6n2i9plz8pRj" +
                "KY5Sn/P13dEoVrupRb+D9YHI0PtB9XjWv2EmcVRzGUdNkCd3z4" +
                "B+cICalPJc0ikJkzkFsC9ZnmDvQbnSe9DneQdev0TJ75eAxkqp" +
                "2RWATS4ALLdINJop9YtkTAakUfRLeKVAnzBeFcdS2cxonMCK61" +
                "SxX8zWhfL3Hurv8ljD+ncgmmHZVQdBGV4Nd+Tc+yTgJNg9VPUq" +
                "1hTbq7nnOKC519OZRcfqjC7jNFbEjTv3Ku9+GxbW5CgkND6keh" +
                "x2n4zT+LiIlAifGttjUv8G/54KJIf1IX9KuxeCVU9ZX8Mi+rxY" +
                "uUNePzYJnyfWJij3b8D1QXR9GHZjoFrJtt2FAs5drN1cLGPdZo" +
                "p1b/YEknwTy9YBdQes90AeC6mn8GouSG+G4q6zbTlzzcaxc89B" +
                "s+oFw96RSKlBY/1jSDwPx+oonyFxgHCMxKvZkWGSvLs6k/Lu5P" +
                "OnHJugxmR8zjoT7ZiYXwD4M4ItA7EEgp3CZ9Zs/txZ8mRi5rqz" +
                "5Ng5OxL+7HKee2otQPa5uXpBOruE2QQA8yThmcpzUnHczzL8RS" +
                "1iFwjup8IG8+ND1j0wnDMapDOzSKw16B3RJD4QFofHq1a0P1aR" +
                "wgT6w4k4PwlHAvL3/mXuAXOoB+shp91rre9xvSL4z3Y9CxhLGe" +
                "jMCIjbDFgHgfo8OTSsfFmDDZa5c695dxoMFZOwYuxBMYc6mtBF" +
                "ZLZqTBjCN2rD6DFL6vGw8H5m9hXw7+0hxRKZY6Xe15SXL6NjEf" +
                "AN3h1cQH5KW2dZT5uJr3xStELdLM2uaf2y22v/f+wrTcRXin4n" +
                "7w4B9LtRsuauPOMc34fp9wQ2Oj6c/AKr9V+9WHTYHgbknN3/eX" +
                "0UMeQwsd92xfOss0uA7VXVB+E1VF0QtEJMOwsD0CBjYedcOPsZ" +
                "jldxuxHPDCh32CK1LUM6A47ExsOdrVDmF7E1JPVRL/IUQg4bjb" +
                "VoPQxIXDdg3LJ1vpKEP19FmSGcf0gMDcK1dPiYz9CvIX0lRDPg" +
                "2dIM25syL9L3TkLnEHHd8Su6kfZddQKfHvpJMmMAVq0f9INIjV" +
                "76Pk1WLQna0PVwdcYh5eHVOHA7n4vPb11voTwv6Ax4ul6oaWhY" +
                "N+t7hVhYLoRnXkFMb2h5BtOdZP3qSeYatyaxOAGlIejy9uVonL" +
                "EyfEovNBCuBeUOw2GwrGfKmju0Pv8BoFMAmA==");
            
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
        if (row <= 1082)
            return sigmap[row][col];
        else if (row >= 1083 && row <= 2165)
            return sigmap1[row-1083][col];
        else if (row >= 2166)
            return sigmap2[row-2166][col];
        else
            throw new IllegalArgumentException("Unexpected location requested in sigmap2 lookup");
    }

    protected static int[][] value = null;

    protected static void valueInit()
    {
        try
        {
            final int rows = 63;
            final int cols = 77;
            final int compressedBytes = 4696;
            final int uncompressedBytes = 19405;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNqVWwuwXVV5PlhUDAJJSFTwirZAH1RMGIdopW2W59ycSCa1NQ" +
                "3ENk4kQBNoSsPD0iQm5Oy99rnn3nPCjQTIg5AXNyEzqeAMVgtW" +
                "IPJWKhDt+ICJVUBLMkIobwqBrvX/69//v/61jx3vP3ut//H9r3" +
                "X3Pmfvte9tDjQHRu6w9aaf319cmM3znCe7J7upOTB8cjGxOVCr" +
                "ZX+I2vxPmgPZucjXauB1HklezkftWZ7Lj4IYn+5dmr+B9uzeWq" +
                "19L3k1B4pn/dgaRE/8odxE+U+I89b68a2dnJdnsrdKHY/5Qomh" +
                "0Z7TWYtScZz3HHrJzgZpAiN1PfbP7XS0mVlmlv2i/Xs/d08ys7" +
                "L5ngPtl/JNZlZ7QjERpH9Crfew/xwQLT/2DpUezje/xv6NXWAX" +
                "2h5ozu89356I1pFH7NL8MGFd5PGl35V2mV1pr7Kr2Rp8JpWYv7" +
                "WL6uOzbcAvDrqLGWmX2EtgvgLG5TB+2dXzpsB8AcbzzKyhZ+0F" +
                "9kIvFcfZv7MXdR6wc0CaINCX2stkLXaFXRVWYKfZaW+wW/3c/b" +
                "5db7d4zpM7V75qdrbnuzXzmOtJ6/idRhDGIGo/YK+z20lrN9dq" +
                "nbcJZzcOThPI8czbHf63anebnaZPbM91Xs62S22cOWS8UfBjZq" +
                "fMaDeUkb7t83kJz7ORTfbzIE3gWIQovW+yu4LtEnMJWGHGA8mt" +
                "2Y/NJcNfZZvGxBbk6j26zrym+FCt1v1jsrpr8z7CmkuG3s9+0k" +
                "eSjF2rDX8526FrkXZdE9ajo2EHjMcfa/tFJCpOKD4YMIUpwBpm" +
                "5FDKXzFF5/fJRlrmkexeKRXfxRiIgTX7KHm6z42LGDn0M+bLNS" +
                "tiktWYYrid7dS1aKwei4eqEJytXLNO2mUcH9YMbaNmFK5NNztP" +
                "f22OItk9dqIZHX41YK5HrZfctTnKhHaieheuzaD112b3TcLZjU" +
                "M5IzufYb68Nkdj4tieG/l4dofUxplDxhsFP+brEVk2kC2s2Shx" +
                "tVrxDh2ZEKW3vzbRdpm5DKww44HkejjezbvZpjGxBbn6cHnOUO" +
                "TdaPXfm5+eS1gfmf2kjyQZ233qtNEnzs92XRPWo6PBuFtWyFVW" +
                "RazIsMQsAQlmPJDcmk02S0aETWNiC3LNo8oqQuTeXLK6z7OvE9" +
                "Ys6XyO/aSPJBnb3Wu8nh3WtUi7rsmP7dt0NOxAVgjn2Xv6RUwz" +
                "DM4ZnOMlmpFDyQ4Ozul+iWykZR7J7pXS0N0YgyP2ziFPp3ubkZ" +
                "1/ZJ4ql5FkVuRGvp0ntWhsMr5dhcj3xj27NftY2qWOTx6mYzr2" +
                "kTD/Z/a6/annQNpTr5tOd4e9y0uIcNoDzCMNHSslexB+Hx37MH" +
                "qhBJYf2J8PdRk58pTw+rH9kf1lHFdmdYhHXS2fzE+MatmfYu3j" +
                "MD4B4z7T8T2UMZ4mnF8z+xjovue4n7g1my0j+5krJx3KblxoFo" +
                "IEMx5I7lqYYRZ2x9imMbEFuaEny+ssRGa0+zw7h7Bm4ch/s5/0" +
                "kSRju3peyz+oa5F2XZMffQ9xND/ieSZzF+f2i1iR4QZzA0hhRg" +
                "4le7a5oXsP2UjLPGuYOh/HGGlE6HoyI7vvkjHi3Glsz9Vfzc/Q" +
                "tWisHu1nqhBhzUTmYn7apY5f+i81S0GCGQ8k1+GgWdo7lm0aE1" +
                "uQ63yi7D9EZrS7P+sR1iztTmQ/6SNJxnafjB/JP6VrkXZdkx99" +
                "D3E07CCu0K3Zxf0iphlaj7Yese3MnU2tLeidfaz1s2ys5a6x+s" +
                "Wtp3unO8uv/NNq65CzzXBn33vhefgtwB4B47uzY7KJbm5mkzuf" +
                "suMcN9MdA9nvOfTRNfFTfx/4/rr1bOu57hdbz7deAvkmRmTjsw" +
                "nFSjef6I6T3HFq9get/2h933GDvVPzP0NUa3/rvwL3lDsO2GNa" +
                "2/0zeusVl7HIjqRo7Zn1i7MTIO4pgP5h60dufKKW/BT/kH2H+N" +
                "abpf+fBs2ODJ6xsg9lv+tWbrPZDCsYZuRQsnPN5t4UspGWedYw" +
                "DX8LY6QRYc0mCeRkGSPOncb2XO/8fJ6uRWP1aP+6CsHZhtaENb" +
                "Nplzp+6T/DzAAJZjyQ3JrNMzO6O9mmMTi7Z6cZLI/8suw/RGa0" +
                "uzZvJKyZ0RzPftJHkqzGXZur8vN0LdLOOh7tuToadhBX6NZsqF" +
                "/EigxjZgzuNfyM9xpjSHZPc5wZ6w26ew0nIcJpDzCP5O41hBTu" +
                "NcbcvQZ4oQQWd6/hIxJ1FwkvutcYi4k17l5jrP5Kfn5Uy/4U6+" +
                "41/PgEjPvMmMzo7jUCDp7UHgMd3WvcJiMTJo6PshubpgkSzHgg" +
                "uSvpE7FNY3B251lT2spzpkkSWd159ihhTbO7mP2kjyRZDexkLt" +
                "O1SDvrePQ9xNG4Spm7uLtfxDQD7EA+Uvx78W+tfXieeU37X/w+" +
                "bf1Mvytp76I9S9jhPNB6zX0m/i/veg4dK/dp7UH/HeC8HkYdfw" +
                "fAPe1B3v3sfgv3aTPvFc6z7GjYLb09ex/tlZa7pI/Cml3lcv+U" +
                "Mtv9rV+4CM846zFe9vu09nGoAj7l7b7sA9hD9mHAP817sNmZ9j" +
                "HA0Hn2ZOuF8Hl/WO4cZ+9yx7hsGu/bupVbZpbBCoYZOZRsLm2k" +
                "ZZ41UkKPNKLnOh9gZPd+7RVHimNjzHy1rkVj9WizKgRnK8+zl9" +
                "Iu086CrW7qIMGMB1KtNmNHbNMYnN21WZe2sv86SWSt1RoPEtbU" +
                "exPYT/pIktXAebZW1yLtrOPR9xBH4ypl7uKNfhErMmwwG0AKM3" +
                "Io2RFpIy3zrJESeqQR4TybxsjeRO0VR4pjY8z8K7oWjdWjHa5C" +
                "cLbyTuystMu0s2AbMSMghRk5lPJbpI20zCO580xIFCONCGt2PS" +
                "N7Z2mvOK7MSnnzW3UtGqvH4tYqBGcrz7OvpV3q+OTBn970ycdv" +
                "tmyXbHtr8v1Y/K6MJXrDxXFkRFiz9YzvXfv/v6uT1cC1eedv+6" +
                "6uvazqXZ2u0J1ny3VkXY/4DphkJsEKhhk5lJrHSRtpY54lspXn" +
                "TIjooxDf2U5YM2nNO9lP+kiSsWHN7tK1SLuuyY+UPdbGFcKabU" +
                "y7jOsRGWaamSDBjAeSO89Gzcz8HrZpTGwhW9l/iJzfT9ZabfAw" +
                "YV3ke9lP+kiSsSHWfboWadc1+VFmZG3Y1xC58wf6RazIcIW5Ai" +
                "SY8UBycR6MbRoTW8hW9n8FSWR1v81vEtZcseY97Cd9JMnY0NlD" +
                "uhZp1zX5sf0NHY2rlLlnLO4XsSLDArMAJJjxQHLn2TWxjeZ8H/" +
                "OxB+JDRQtIIqs7z75OWLNgzUfYT/pIkrFhzX6ga5F2XZMfZUbZ" +
                "ZVyhW7OL+kWsyPD4b0epR6zpb4ecQr/mlBj1m2tBa77yN+VqDZ" +
                "BOjlV4zzEGnup+R3voikjG/bN2u3egPUrfIX7/zOmfbExrTGs9" +
                "Dd9Hv4LxULvbetHNr8Fu2lsOOTU7ot3j/TPHTRZPHbB/Bldk+f" +
                "6uMY33z9wM+2fNgewM/t70+2e9g24+sX11vH8G59nT+I1G+2eu" +
                "jqfcccDhptD+meOP5HiNaX7/zMVT+2fy2xrWbBtpWof9/lm7Qx" +
                "Hw2zLaP1tr1sIKhhk5lOy1Zm3+HNlIyzxrpIQx0ojwPuB2Rrrv" +
                "AOUVR4pjY8z8kK5FY/Vo11UhwneAyJw/n3aZdhZsl5pLQYIZDy" +
                "S7Z/C7sU1jYgvZyv4vJYmsbs3uI2zsJ30kaUz+oq5F2nVNfvQ9" +
                "xNG4Spm7c3S/iBUZbjY3gxRm5FAafEjaSMs8a6SEHmlEWLM7qv" +
                "3KNbs5JlmNP/JXdS0aq0ffQ4rgbHxtpl2mnQXbSrMSpDAj56kx" +
                "pTHFrIT7M7Ch1s+MkRaSMAZHdPdnpcVHJHLXpvKKI8WxMaY9Qt" +
                "eisXqUGVkbrk2R2b4j7TLtDGd8Guhs5Xco/Oww+KB8ImoOFNvh" +
                "8/e1SLdRP0nJT87wLLe+vAJ2FVtjfGswfocCn7eTIPJN8bMTPM" +
                "2dVGzQz2XhW2E7PTsVu9BWbIO7G9dDMQbxrtXPTsWNxZaysj2d" +
                "LcUO3j8rrguYTcVm/w4lenbaZDbBCoYZOZTsX0gbaZlnjZTQI4" +
                "0I36B3V/uV59mmmGQ1UNFndS0aq8f2XVUIzlau2S1pl2lnwdYw" +
                "DZBgxgPJVfid2KYxONu97IH4UFGDJLK63/pthI0zSR9JGmP/Ut" +
                "ci7azjMc3IVcrcndv7RazIsNVsBSnMyKFk/0raSMs8a6SEHmlE" +
                "uNueUO1XrtnWmGQ1UNHndC0aq8c0I0aKe3bfTn+Udpl2Fmy5yU" +
                "EKM3Io1W+WNtIyzxopoUcaEX6b36v2K9csj0lW4w+7RteisXr0" +
                "PaQIzlaeZ3emXaadBduF5kKQYMYDyVX4YmzTmNhCtnAVPUyRGe" +
                "2+0a8jbOxXrtmFMWmMvVrXIu26Jj/aF3Q0rtJL/h0KnGen9YtY" +
                "kWG1WQ1SmJFDqXmMtJGWedZICT3SiPAe/bRqv3LNVsckqzGr66" +
                "fZtboWjdWj7yFFcLbyPHsh7TLtLNimm+kgwYwHkvuePSK2aUxs" +
                "IVvZ/3SSyOoqe5WwsZ/0kaQx9iu6FmnXNfmxqOloXKXM3XmjX8" +
                "SKDCvMCpDCjJynxtTGVLPCfoNsqPUzY6SFJIyRRoR72qnVfuWa" +
                "rYiJNRjTflPXorF6TDP6MdzTiszDFV2mnQXbRrMRpDAjh1LzBL" +
                "OxeQLZSMs8krvXEBLFSCPC9+ZEidRecdwYgzF9RXEtGqvHNCNG" +
                "int259m8tEsdv/S/3FwOEsx4ILnf6uvmcvsG2zQmtpCt7P9yks" +
                "jq7mlfIGzsJ30kaYx9U9ci7bomP7b/R0fjKmXu4Wa/iBUZ1pl1" +
                "IIUZOZSa75U20jLPGimhRxoR7mnvr/Yr12xdTLIaqiiuRWP1mG" +
                "bESHHPbs3OT7tMOwu2q8xVIIUZOZTsW9JGWuZZIyX0SCPCvcap" +
                "1X7lml0Vk6wGKnpb16Kxehw6pQrB2co125Z2mXYWbKvMKpDCjB" +
                "xKM26TNtIyzxopoUcaEbhHq/3KNVsVk6zGH8U5uhaN1aPvIUVw" +
                "Nso8cnLaZdpZsF1prgQpzMh5snsGD0gbav3MGGkhCT3SiHBtHq" +
                "j2K9fsyphYgzHtHboWjdVjmhEjxT27NZuSdpl2FmwXmAtAghkP" +
                "pMaixqLYpjGxhWxl/xeQRFZ3r7GIsLGf9JGkMfmduhZp1zX5Mc" +
                "3IVcrcI2f0i5hmyG7NvhbeHezjHamwd/ROkrPrXL1z2l3/f8L4" +
                "92fZLeH9SE++iUDfDPbOsy+U76j5f6B+mP71b8Z+67Lyr9XyT7" +
                "avjnGwq/Vr//dn2TXlXwv/wh3PhDijrYGKvy2G9ynZhy3nU3Ez" +
                "6GDkTP77M6i5IxDjsmnSw5xtzuYZD6TG6Y3TY5vGxBay6ciMdu" +
                "fZ6YSN/aSPJI0pXta1SLuuyY9pRq5S5h6Z3y9iRYYtBvYqaUYO" +
                "pcGD0kZa5lkjJfRII8Ln2cFqv3LNtsQkq/FH8YquRWP1mGbESH" +
                "HPbs22pF2mnQXbkBkCKczIoTT4jLSRlnnWSAk90oiwZs9U+5Vr" +
                "NhSTrMYf7aQWjdVjmhEjxT27NduRdpl2FmyLzWKQYMYDyf1Wj4" +
                "ptGhNbyFb2v5gksrq7oCMJG/tJH0ka0/6srkXadU1+LN6to3GV" +
                "MvfQtn4RKzLsMrtACjNyKNXnShtpmWeNlNAjjQjcI9V+5Zrtik" +
                "lW44/msboWjdWj7yFFcLbyPHs57TLtLNgyk4EUZuRQKsZJG2mZ" +
                "Z42U0MM+rCPCb/Oj1X7lmmUxyWr80R7TtWisHgfXVyE4G+05dk" +
                "9Lu0w7C7bljnaLeTdwIBXH+xE1AeFl5kuNlCD2cvIK0vIQSWDb" +
                "/xrF2F1WIUhoMG9SS4LdrcbdulKsruwUOLdmU2VkP4vKl1Mvwd" +
                "YyLZDCjBxKxSRpIy3zrJESeqQRYQf5/mq/8jxrxSSr8Uf7Hl2L" +
                "xupxaEoVgrNR5u7ctMu0s2CbbWaDBDMeSI3pjemxDbXMxxbkGm" +
                "Hn0+PQm9EYUaKJK9dsdkwak0cZJT6uk8c0I3ZJeKq3+/l+ESsy" +
                "LDKLQIIZD6TGvMa82IZa5mMLco15Yc3mUWRG25+jVnozipH9MU" +
                "P7pVbi4zp5TDNil4Snersr+0WsyLDewF8G0IycJ7unsVDagvYA" +
                "80ixFP5HbL33jSPCSi6s9ivXbH1MrPGcW7PnpLaqDj2mGTESZW" +
                "uE/67ujujIuh6xEsMGngZpRs5TY35jvrSRlnkku1dKjfno4XFx" +
                "RFiz+YyUUco1G46JNZ5za3ZIamO8rJPHNCNGomxYr7uvOllH1v" +
                "WIVfo/nzUn9A==");
            
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
            final int rows = 63;
            final int cols = 77;
            final int compressedBytes = 4140;
            final int uncompressedBytes = 19405;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNqdXGuwXEURXkCghAIlhEeSJRHlobx8AaKQMLt3s2LFPPkhoH" +
                "AxGoSI8U80VQSqztk9e3fvXiKJwAXKVBH4YSwQDD+EYAhEKAsV" +
                "VECeyjtEeYMCohRBZ6ZPTz9m9kLcqZnpnv766+7Zc3bPOXsTc7" +
                "m5vGJfppxBcm1o5tBMbsNVkqFJbWgmeDicZKxUgDHlV6nI2DHG" +
                "SZVKd1++mspDj3FEYMJokG+l0u9rZp0P7UTj1MapTsMZJNAa49" +
                "wWVk8lDLegBh7tezSjl8bTfrhnnEliIG53ss5FY6NxPIWgaO3f" +
                "l3u2Na4yrgxms86s8ztYziCB1rjCrBtZjDZcJZlWuAYcMaOPeU" +
                "XaLxxn62Tj2bjePVrnorF6jCO6sbVF1mz37NW4yriy0tY3fa+V" +
                "M0iuDc0amsVtuEoyNKkNzQIPh5OM9tycBauxX9izvmy04qRKpb" +
                "Wer6by0GMcEZgwGuRbqYztr5l1PmwnGqbhNT9Dh1apFAdLm8ZI" +
                "C9pC/Q3U0FqpjByDWOnHfXjTmOahOhdu1zm50R5nio2y5LHHjh" +
                "/EmIiwxCzxmp+hQxsyQ8YsKT5BNlglWVpAGjKQg8MBM6GBkaNR" +
                "Cnu2RDaNKQ7hqxzPa+BjHNGN5bm5hPIdO2cQYyLCHDPHa36GDs" +
                "1meJiZU9xDNo2Bub2FPICrrL9k7g6j1R5nX0asmdM9k/y4D288" +
                "G891ls6F22mNxpGmZnNjuWcs9tj3BjEmIpxvzvean6FDs3t2uL" +
                "RpjLSgLdRvV2pXoVQy3YdY6cd9eJOY2lXdH+hcuF3n5MbaVZqN" +
                "suSxx64bxJiIcLG52GvlDBJoxSe5DVdJhmaPM6YhB2BqVxKjlx" +
                "7gSO0leSXGSbUruyt1Lhqrx9qVKQRFY+9XVKXmD5iVZqXXyhkk" +
                "0IpPcRuukkwrXAOPmNFL96f9Qt4rZePZmJUj87oX6lw0Vo8jc1" +
                "MIisb2LKoyrqy0jZkxr5UzSKDVF3AbrpJMK1wDj5jRS4+k/ULe" +
                "Y7LxbFwvjtS5aKweXQ0xgqKxPYuqjCsrbW3T9lo5gwRaczduw1" +
                "WSaYVr4BEzeumhtF/Iuy0bzwYzkrlorB7BQyMoGtuzqMq4stK2" +
                "2Cz2mp+hQ7Pv6tHSpjHSgraQw2LU0GqlBxEr/bgPbxrTzXQu3K" +
                "5zcmNxlGajLOPYKcZEhLPN2V7zM3Ro9tycJ20aIy1oCzmcjRpa" +
                "rfQwYqUf9+FNY1qbdS7crnNyo6tBslGWcewUYxyhFe6Ps/vLa+" +
                "0qrhTsygltrS9m/7bYd8oantF2klsPSt29ertWolfO5T2D9wmV" +
                "xKvbsrEfIz2z8bPng1aNPYrdPe+MZrXyPq/sn+W8Pcpwj/x4rp" +
                "tJZhKfQQJt9s1mkr+m9RquSpk0tGlmQts92w2xZpK9pp1EFhmb" +
                "M3KMvaadpOOTXefkRleDZHNjeU07Kc5XViJzQUyzCu8AziCB1t" +
                "wHbbTe2tlbEFHlmjyuNKO/Kv0VsqNf1tA+vPFs/NG3i4zIke44" +
                "kzn5cR/NRlkSL2mE1PkQxqwxa/wOljNIoBWncRustnYmGXFSA4" +
                "+Y0e/ZzWm/8F6vkY1n43prF76aykOPxddSCIrGjjPFrPNhGPvy" +
                "mp+hw8t+B8yUNveyexZkaUH/kINBDa12zzYZw9Fk4cjBGLtnRs" +
                "cnu87Jja4GyUZZxrFTjIkIy8wyr/kZOjS7ZydKm2stJksL+occ" +
                "lqGGVrtntyFW+nEf3jSm9X0TxSe7zsmNrgbJRlnGsVOMiQhzzV" +
                "yv+Rk6NHtuniFtrtnjLMjSgv4hh7moodXu2S2IlX7chzeNscfZ" +
                "XB2f7DonNxanazbKMo6dYkxEWG6We83P0KHZ42yWtGmMtKAt5L" +
                "AcNbTa783dESv9uA9vGtO9XefC7TonN7oaJBtlGcdOMSYi7Gf2" +
                "81o5gwRac0+zn/89wGu4KmXS0BZysCu1cWL0e3YgYqUf9+FNYm" +
                "rj3SN0Ltyuc3JjbVyzubG81ohiyyplPgxzmbnMa+UMEmjFN7gN" +
                "V0mG1t7CNeQAzMgCYvR7dhRHai/JKzHA2X1P56KxehyZm0JQNL" +
                "ZnUZWaP2DOM+d5zc/Qodk9+6a0aYy0oC3kcB5qaLVn+0uIlX7c" +
                "hzeN6fV1Ltyuc3JjHJGyjGOnGBMRrjXXeq2cQQKtvpbbcJVkWu" +
                "EaeMSMfs8OSPuFvK+VjWfjem9M56KxenQ1xAiKxvYsqjKuDOb2" +
                "rnj9jtfE+enhPuuM1P0mx+pX64WJ7+lGzooZ5bX4RPebzWrvEn" +
                "2vwXNP3W+OnKnvTwa9BmNkjnbn1pq1fAYJtOJbZm3rTrThKsm0" +
                "wjXgIMbWb8hSn0LI1l3aSzJJbuBs76Rz0Vg9FotTiPI7gEVOVR" +
                "lXVtpWmBVeK2eQQCvOMSv896bXcJVkWuEacMSMfs9eSfuFvFfI" +
                "xrNxfeTbOheN1WMc0Y0jZ8uaSePR4spgnugevTi3WXXPNfj9Kp" +
                "4L8T26PmeRsfck3eP2lhC+9/iO36P3ntjRe/TiO6l7dDjO/u97" +
                "9KvN1X4Hyxkk0OrLzNW9V9CGqyTTCteAI2b0NV9IyN7L2ksySW" +
                "7gbN+qc9FYPboaYkR5brLIqSrjykrbUrPUa36GDs0eZ981S3vv" +
                "kU1jpAVtIYelqKHV7tlFiLXM28mP+/DGud3L7tlSHZ/sOic3Fh" +
                "GbG8s9WxrnGzMmIlxqLvVaOYMEWv0r3IarJNMK18AjZvR7Npr2" +
                "C3lfKhvPxvXRPXQuGqtHV0OMoGhsz6Iq48rIo3tTd0P3xma1+w" +
                "t/hXFMeC7sHzvnp7LrkIXwbJt/DrS3sKfA+038rd27M/0dny/Q" +
                "ny/xtUa+yO7ZnhIlnyAmn21n7Rnx9UnqGgPn+Nl2dK3RMR0+gw" +
                "Ra/QzT8c+2vYarJEOz905MQ46Y0e/Z3YTsPa69JC+PinF7T+hc" +
                "NFaProYYUZ6bLHKqSs0fMNfYtp7N673ktfppboSVEuF0ksMK1z" +
                "z3Nehl6/xtwK3nWMHibYpXYiBulEuEXS9HX4PIFLILlXqpzDkw" +
                "uxnXiB8x+rjjz8zrF8jfA6C7cxOk1nXyKmTQ7wH0ql/QmyWvFL" +
                "JG+jyw1yL1+FrD+p+sr4zYWZXIgmroDVFt9Qsm+P1pe5x363p+" +
                "rdE8qHkQjHFr2V5/U67FWLmCGq2i1JpuK3hzsN+gLDhaZqM9Wl" +
                "VcozEd0a0Sptx9xagzIoxZbVb7o66cQQKt6JjVo1W04SrJtMI1" +
                "4IgZ/Xn6u7Rf+ExZLRvPxnV7raFy0Vg9FkUKUX6escipKuPKYM" +
                "7+lP3RH5H34W/C2V+yJ7PHs2ftUbE8e27UHh3Z3/z6a9k/sjfg" +
                "3Mzeyd7L/mu/y3bKd2lvyXfP98on0fdm+T1XzT9eMj6YPZI96v" +
                "fs9ewlq7+cvZK92tuevZ69qX8Tzj+a75NPzvfPp+bT8unNan5o" +
                "fnh2b/YHvD7LHnK/CWdPZE9lT7vfhLOt2fPZC9mLcG5m/7Ljf/" +
                "IPWaZ98wPseGB9eT4l/1g+Iz8ke8Da/pw9bMe/wp5l27K/89+E" +
                "s7fs+Ha2PXvXeu7s89k13y3/cL5Hvnf+Ea8flB9sd+5cc67fQT" +
                "9Dh2b37IfSpjHSgrbwvp2LGlqBkaPJwpGDMUVP58LtOic3xhHd" +
                "WHRlhrpKzpiIMGyGveZn6NAqldqzZtjfOw3TOsdIC9pCDsOood" +
                "VeX+2CWOnHfXjTGHtuDuv4ZNc5udHVINncWJ6bw3G+MWMiwinm" +
                "FK/5GTo0+4k3WdpcgzVEuNFen51CenNyyKFkdh4oj+6PWGAiC/" +
                "nwxrMhLhmf7LRGI0WRqzxDnm/MmIiwyqzyWjmDBFrtOW6D1eJn" +
                "JCOOa8UN4EGMxc8RZ/dsKkNexzlk7JgbOIvr+WoqDz26GmJEsV" +
                "7WrKuEaJo/YGqm5jU/Q4dmM7xJ2jRGWtAWcqihhlb7LtyBWOnH" +
                "fXjTmNETdC7crnNyY7FBs1GWcewUYxyh/GzdVNwSvrvCOV57Wd" +
                "+V4d9SDXq1Xpz4+XFti15pH5e4R9w46G+pRscn/GuoxP0mr+GD" +
                "veL7TZvlZK51Po3S7KfKPftp2D17BNe3gK02ZrF9N7o9Q2znc5" +
                "0xwXZsfh1aO58Pq71Qwa/Zu/DRzvHW9lnkcn4kdz7TWRXn7rLh" +
                "KCehBjlGvwec0znG2TrHcZ86e+/AP78eeWDPOl2OoIh4NzB6on" +
                "0Hv0R/sxeexk7Fo6t9lLx3yt6hp57xvVN+yaB7p+ZU+cTX3Tvl" +
                "9NTkx/mexDR6Unzv5Pyzx9pHhiezz1iG5wGX/yh17zQy7p+bzK" +
                "BM81Uyr/YR+m/2dN7ub/b4vVNjemN6uZvbyn+h51dcL24DW2O6" +
                "s+GqP862gYZ2koiJmNlxcr2730Q8zMAFfk5Gpl6dMIgoNvdO5i" +
                "gnYS7cl0d0mrO5+00ZT9aNc7Zd5w0RqSaz0CyE52d29s/P3Iob" +
                "bYZ3m4X9nUGG0XX4PAMNLegDurSKM2WNxiNr+CReKKMBBrX2rR" +
                "LFcdI3RFzN1+J4qc+z2M4rteMiswjOTbOotC9ykuv1jWaRf362" +
                "CNahl3u2CJEwo17fqK3iONuo8cgasgucoycRBlHFvRJFWcl8eE" +
                "S+hkytLRPuWZQ3r9SOC8yCcj3MTnK9vsks6O+NFlwt92wB+boZ" +
                "9fombRV7tsmdm4Dv70V+LLvA2asjC6Hat8KzIJk1dcqUIga+IV" +
                "7bhHsW2Xml9gytNqpwbs7eCuemW3GjfVcfbVT7hzh59lZYaZTf" +
                "AbO3gtaoogdKxWPOSjzqm38zx8MMXP5zY6uTkQk7scO5yVFOwl" +
                "ykb7huecRpGEPGw6hytp9nKm+IyPZmSmOK18oZJNCKpxtT+oeh" +
                "DVdJphWuAUfM6KPvQcj+XtpLMklu4GzfqnPRWD3yiLRK0Whn4i" +
                "rjymA288388vNsfnkczneS68UzsEaj6+W5OR+RMHNdWsVxdpfG" +
                "I2s4CwKn/TwLGET150gUZSXz4f8+QNag4yXPzcjOK7U7N6Mxoz" +
                "wCny3326+4XmwDW2OGs+EqYEFDO0nERMxsz+7keJiBC/ycTEyE" +
                "QUT/qxLlJMxF+oY92+A0jCHjybpxlv6UF9Vk5pl55V6G2UmuFy" +
                "/BGo2IQQ0t6EMc7XtIY9/8l2m8ZOWcFINQ/bkSxXHSN9Q/LmvQ" +
                "8fD/ihBH1bx4hfs3pjWmee5yBgm0xpncFlanEYZbUAOPmNHv2U" +
                "/SfqHCabKJbGzvf13norF6HFmdQlA0trtRlXFlMJt9zb5+B8sZ" +
                "JNDqZ3EbrkqZNLSF90Yx+nPzdsRKP+7Dm8b0L9K5cLvOyY2uBs" +
                "lGWcaxZZUyn9iDnlyw39FfG/R3bYN+Ke8cO/Ezg9GjJv5buffj" +
                "71+c/pu9Qc81mtXi1TRfdE+5IR2T31cnzuK3YMx/iXp9cRpD8w" +
                "7v2RcGxf1g/C4jjSc99sIazFud4wZH9Xu2MR0TIgzKx7wBY74J" +
                "9fy2NIbmHX2N3Dgo7gfj76+K8aTHXuaNkRsGWdSebU7HhAiD8j" +
                "Fvw4jnpnm7eDeNoVn/jcv7vUZXDIo7mF8809wc40mPvWw1tw+y" +
                "qD27Ix0TIijb/wCn4oHV");
            
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
            final int rows = 63;
            final int cols = 77;
            final int compressedBytes = 3548;
            final int uncompressedBytes = 19405;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNq9XH2sHUUVf0BpLcgr5Us+nlYIRhNAExsBX4q8vXvvo5QQQ/" +
                "yPmKjIHwKJHwn+IYrs3Pv2Nr23iQZEJSZgSLWJiR8JoQKh5EX5" +
                "kkKp2JbSltIHDwT6waOUUkgBZ+bs7Dln5szee99DdrMzc+b8zu" +
                "+cmTu7O7O776kNQ8U2dhBS9ZiT1RNDQ81/DJENMJi7TT05FNnU" +
                "o81HUKrfgeXmP0OuGH9p8zDo1OOIQ2xoNXYQPPqa5qSElTyCh1" +
                "g8Y4chbc5zcu3bMgbzQbdVf4/57Y9/YibEoxxauTZImt416CEW" +
                "z9ghSJvznVy7Vv8i+0MM5gP32XphnB3qn7/5RohHObSCNoQaPc" +
                "4Oye2XascOcV16ID1AcyiBNPFWemDVbqdztVjGGioBR8ho++wh" +
                "RHbW+VaciXMDZ+t+PxYf66cTIqI5yduMEvUWtgzy8ZHxEbBpvA" +
                "I51Jhj4r3xkYkNpmx0rhawUHKpswEZtE7qPETG2WOI7zxo0qzu" +
                "/Bo7Y4nMjgUj6qznKFMyR+tLppyN0KiKs3mBkTBewEOf8XZjHN" +
                "Qe48K+Sfel+2wPFjmUQJp4P91n+gwkV4tlrKEScCBj51+oqf8O" +
                "kZ3HfCvOxLmBs/O4H4uP9VPqEWuLcUY8S60MWwY5GWfTwTj7oB" +
                "xn0944m64cZ9PIw7f2Q8I4my5/z2nDGxtntv/Xc5QpuVga09I4" +
                "a6+342yajLNpb5xN81wYZ9PUXvfc/tRe5V0OJZAmPkz323FmJV" +
                "eLZazBvbECOEJG630FIvU4Ixzcd8gNnHqcebH4WD+lHrG2GGfE" +
                "M+B4K31+Z0HG2ZQ/zvKjXO8aHRtnU7Fx1hh1WmmcGS3H63E2VW" +
                "qnjGXl9WyGo0zJxdKYksZZY9SOsykyzqZ4XOC/MYpxBONsitrr" +
                "nptJZ2wPFjmUQMqPpTpXi2WsoRJYhIz2HvCEbFeOsxm+02hsRP" +
                "P9WHysn+bzJAR6I9ezoJVhywrd4bSYrzV2F7a2xhzJraBLDxud" +
                "qwWs0zgL0IIMWmQm980tPh64HauxRGbEOKnzJkeZkouF27oN2o" +
                "DxIh698pzbY1zYN+E9F0fm2D1O9nV4X+abmwFE57RbyWzxHuRy" +
                "dnHL4tx8m6NMCecK2YgwJ73Hj8j3Ec41gitKNK7s30UvP1r+Rr" +
                "/gMpaz92QOihX77HUs5xcXK1Jip46P2xpM90Ht+zkSs74GZa9W" +
                "eYc2qCW91yjZgeia+Th1YbVt+nDZKn3FXrUbZaoz28qfx20j3D" +
                "ux3L24fzuHad3fX+Tkl/mquAabHGzNt/IW5ufe9F6aQwmk/JL0" +
                "3okNTsIcMVQD+8RPgAPx3a+5kp4tbUNkdxlnob5DbuDsXuLH4m" +
                "P9tP2shCjmGqVfPa+6KWylz1/aP5A+YKUihxJI+Xj6gO6zQsIc" +
                "MVQDe+Ny4EB893JX0leGyxHZvYyzUN8hN3B2l/ux+Fg/pR6xtu" +
                "iz0q+LjLfS5+cWdl1zWRF3+bwrX677f0NKnn/Rsnh2PNKoVemp" +
                "trsMPffidb67l8S0JnqJRY4nPDer43Z9U0SyLl1HcyiBlF+Zrt" +
                "N9VkiYI4ZqYG8sBw7E01JjOSL1OGMs1HfIDZzd5X4sPtZPqUes" +
                "LcZZ6ddFxlvp83ML25fjRdzlb5BfZe8B5DdJe1w708nGFZXjjG" +
                "i729Bz2sc12WDi9wATvcQixyOMs8q4Xd8E9Q1/fpZ/w/QZXUmM" +
                "j1S3S69M6pV9RrTd533PvbeKPtMc0vxMjkfos8q4eYTpfel9NI" +
                "cSSPnVJnUS5oihGicBB+Jpqb27lx3faTTm6O7xY/Gxftp+QULw" +
                "tlOJegsj5Ba6L+vBOPuWP7Z6j7NqfS0Tf8V6LzvH3d0bHQl1eZ" +
                "zJHgfd6mwU1mv1mivRGnPkN9Rr5tk2aFwtRZu0NWksUA6ZSQsW" +
                "OE29tvpoZydZuXrAOIQ7N3nUFOtHUVtA65y/6jltGDcyW/tL65" +
                "daqcihBFL+g/qlts+s5GqxjDVUAo6Q0bZgISJXz/etOBPnBs7W" +
                "/X4sPtZPqUeshT6jnqVWhi3jFuS8frE8N38Y10XubS9Wy7VPzO" +
                "0cWT08iHfqsVfkc9vS8hlvfmNc18tWlle9Ncc+WzSId7tGf7a/" +
                "yB2u7146AfP0hHTKSfnN+mzY67S2bopK3JrrIU+nKM7spP8Oxn" +
                "WxGFv7Yp6dt8JvmSYv+Do310AMxc1uU18s+z4NdN/Nb/XvcS1y" +
                "PVWn8nunumZ8JL8d5c6nyBOH2waPLf9V1f1aum+6NqhzGc8vP+" +
                "Jz8+UykoWhLqyTbVGmFp2zSDsXDh7bYN6pBdfNxjfxszndTHMo" +
                "gZT/hupcLZZhb01SyXEgnpY6IxTpW3FejgHO1Sf6sfhYP81/LS" +
                "F426lEvfn8voU0L01GB52z+npf7iyb4z3glMonrdK5Odpf5HM8" +
                "N8tn3/kdcV0vW1nu1D6eyEmfbekvcoebpeeXyj67M67rZSvLya" +
                "P/1z4Tokue7i9yh5ul5/LdUH5XXNfLVpZr580lsl7WUnTJ870i" +
                "z39PcX2tPhfWF9IcSiCNn051rhbLWEMlsAgZbavPl+1oNDK3Kd" +
                "XONxHxWHysnybbJARvu+2zbWErw5YV/TJC36q7Ekj5GvkLA/+r" +
                "FvwKhT8D4Yz2ena1Y8dvD3wbuvvfuOR/4B4p0twDeEwmTZ7x2T" +
                "BK+nUE4HgreTxokT2dbbT+Nrn3m9n2bFe2M9Prs/wvmV1xZK/k" +
                "f9XpG9mb6jqdHzbvN7MPsg/1TPEodYxOF6gT1EluTlvOZ0fUOQ" +
                "Xj5uzZzD6V7Xw/26O+l+3N9mX7df1MdhDeb5Y2x6sT1WJ1ijpN" +
                "nanOUp/RNZ9Tn8+ezJ4q741bzPvN7PnshWy3eb+ZvZS9mr2WvQ" +
                "59lh3S6btqnrY7Wen5szo92anOUJ9VS9S52TNa959Mn6PZjoLr" +
                "5ey/ZZ/tzA5kb+u6d7L3syPa8mgbz7FqvlqojlPDyq7a1KfV2R" +
                "q7ILHPZyCHA3Y9EvdwnY+BvDWJFoAvoljgJKfVfXabw3JP1Ibu" +
                "IcaPheqxDlPTBs6GUYa+JcbQA85e3Dhtlud4e6k0/6LYQbfO7S" +
                "Fj/880pfOfxiPNz9pf7m921vxkHCN94aT7bzEc1ro8v+r7YjiH" +
                "HbjP7goZKVc1L2h9PMYjWUMbesfbPC2O4TFK4wzX6M3TP/Jx9u" +
                "ePe5wlz/U3zpLnBh5nJ8Fhe6o8I+v7YziH9Z9r9OyzjSEj5aJl" +
                "OUqO4fFI1tCGal7b6gvjGB5jWXseHPTcTC9KL9LXgwslXDLLua" +
                "lh9D1Trmpe0Pp4jEeyBo+9420ui2N4jEyzBo/i+nmjPn7sdDBK" +
                "m0dTOc7Ur0bPoNagtppXt+wYjqLxJmvEc3ON79f34RBVUUu65H" +
                "w47PXsj2Wf6dlY+3oJ57BCqypX4Z1NoWfKFecFbesIx/B4JOtk" +
                "U29ewMUxPMZyLnkTfrOH94D23QHuZ2qLujn+zV7l8189m+zsCm" +
                "pv4XPaSoafqk3yN3sqi94DtlvLnt/sJdv7/2avNjw0NLY20bPc" +
                "ZNHYWlOuFe92oDS21tSWzIscFuuMvjZMJZrXhjtPuxrO5RgNl9" +
                "Ml0XckoHUY8IexgB0woXeMDPToHVpKS7XhuG+IMbyPhuvN1p/S" +
                "a8L1Zuu12HoTSq3i609jG643gXH2601/hdt7vQlxxNeb6TUx5v" +
                "h6s764budrLoeS2dMV6Yr64vZvnc7VYhl2LqUrgMPgOKPusRVQ" +
                "G9qVM4PFfMcaU8I4Jc9Uh2no0TGBBPFKzH48iKlao7fXuDW6Tc" +
                "0avRlfo6tW7zV69+Rsj5b7XqOPHRlkjT52RFijb+1zjb61/zW6" +
                "9OTGjcz2Bmk+zOfe1KpqHg367qkhLlwH8DMoNicPz6RshJ9jdi" +
                "azI87HngXtGA/ObP/cD7dGs9G0s4Xy+7T2k6iT8xhT9Annliq0" +
                "i6B6oyhTwpgka/BINT6qaPVlcd8+s9rg97r7e3TNc4U/FmKjiv" +
                "89OnsrzN4ArFxU9a4q/nv6o8P9PTq/oktzjZXD8lj25eaV/a83" +
                "G1kjwxwO2PU4e4rrfAzXOJ3PjGh9bp7tsNyO2tA9xPixUL0fk0" +
                "mTu302jJLyAk5iDD2QePNGbnv86+U4uwp1ci6v0amWb91zhJGf" +
                "+xH0ODdzakFjkqyTzX5EPgrkZHPct89cK7/7aEw0JngN1cm567" +
                "Ma+9qEarmmu1SIaMKPoEefaZRjNXiMSbIGJNX4KNfquO9YXNI9" +
                "sXbnR37fvPbjvm+aNvRz3wzvmPH7ZnjdRQ/tHdyeX6/DN1Kx90" +
                "5knF3H32CZdYDcDvlvq6tmRm4dwKNINvqRyr9GsrH6ySdbByyt" +
                "2/PF5VACqb2X6lwtlrGGSmARMto+u162K2cGS/lOo5Fj8bF+Ov" +
                "E3CYHeyKwkaGXYskI3Wve+ZIEacyQvgw5TxHKNY6FcIbPusxso" +
                "Hu1CK+cNMDEUj0qKwm8Db0VkFjkaqwN7t3ai7wOyXbper53q3+" +
                "RrJ627s5wLfWCxR9m0x/tN4vkZy8TWTvx8Nmsnm5+pGYT3m4A3" +
                "a6cijpf08Zo7NzP7/1bUPMSuPFGdYfnsN3u4dqq6rpq1E3/joO" +
                "3Z2knptZraq7art3T+rukzVXwPosh3buoLdvaxUXi6JH7dq94J" +
                "anTP1y6Y0/caF6hdlc/XZoQ5rfg2SZVf16riqdnKk2YTEazRm1" +
                "vwmWP9O8FT2K1NG8Nsnzl2fyTUC88c1WmRp8AnV/2dcLgO0Gv0" +
                "JfIzR/UV7x6wRH7mqOYLfyf8P2aQTWQ=");
            
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
            final int rows = 63;
            final int cols = 77;
            final int compressedBytes = 3110;
            final int uncompressedBytes = 19405;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNq9Wn2sFFcVX6XRRNNYNVjFB01BrRGEmrRGNGl2dnbX+BFqbW" +
                "LVxFgaoS/WCEFjFdu4MyvM8v0eEG1smzQE3nvUfypKCZaSZ/8U" +
                "qzYmftTgw7bR1qB8FqUU6sw9c+ace+65M7vvQWcz995zz+/8zu" +
                "/evTM7OzO1Gm7BHtphSz5BvmCjqTfGb0Ssf/N7NQ/1pVlKeWu1" +
                "eJaN4npRo8bOeWUORJSp1nzR2lqt80zeXlzM2Z0O7r74huj+FP" +
                "sK9XUntTzRf53xfrBW25Q4uB9E3Hpr2YxF348XpLn/Qj2dv6f7" +
                "i6mnYzK8TxnvkIm8zst5GnGd017MW6KPln+XNGfhPm3OZN9Ac7" +
                "bHnbPaAFu8sNSrzdncitGe7g+nb51nujeazMWcxUtqtfV3Gead" +
                "+fx8JMe+UqpjmzqilL2hfJe0zqLt5essXflzsnUWjcp1ZqK3xh" +
                "92IyAjX2fRllQL+56jTTnXNNZZMEF7rvDb3Jdt9UvZ+ax+iTA+" +
                "pv491BdMSH/9knI+m+ARFB9MSDSxc16ZAxFlqn2+YJz2fM6+w3" +
                "3Z1h6CkjA+pv491BeMSz/kk3geQfHBeGfIx855ZQ5ElKn2+YL9" +
                "tOdzdi/38TEQxsfUryed//3kLed1UVxvsF+ds/0yr8yBiDLVPl" +
                "/9NJT4G1A/nSQ6hupBt2TIl7d/fokn241Kx/Ben0cc9V/Wc0IG" +
                "zRdcA7s5440VI9zswyF20K3xqsvIucp5wSvxpEeLhozVeuOv+j" +
                "G2Rm1r3lPM2RbpW/cpU366Ns0tuW4wfPxnn7Zy5SzjvNoV3Ipr" +
                "2nydJduTrXitUSgYrbrWiFfY523barxWdq1B17TRu8quNdxrWs" +
                "jins+id0NGeU0bT0Y3i56V+rVG9KZ+rmnj4ULhSLpfb63SlX2c" +
                "o1f6rcalcnTlOp1fllPjalxSx+hch8dfn846C8ZozxXu5D5Tr7" +
                "BtlWcF92KEL46jg7EyXg1VaF5h+lfoo7LzyhyI8Of2+4JHac/n" +
                "7MfcZ+rh+JvcVnmGuTcYljn86ODRMl6zGlbZqELzsOkf1kdl55" +
                "U5EOHP3YeuNcWcPeBE3+Xi5f/NsvPZpvX9MA6+QRaNK17d56/N" +
                "vTPJH68t5sw9ku7u49i5229t2jcdRqbtvrKcGlf8vT6Z75/JnL" +
                "F7Qcq/LPe+hodFva+RLJjZeoo7g97XWD/S332N6a1tPIpozhqH" +
                "0lF+QMNp/wMpxjOidZq3PcS5/LzgjTfYGFuP9t8JMpbzVuW2Na" +
                "pjm12ss/3985ZcHzzOjs2fXw5G7xWmNmejfa7gndPJ2Bptjdq/" +
                "AfGP8Jq2NYoYu1ZyP1Dm3XRQyysVlMz/h2xU1iJNWnRju9Sbtf" +
                "n1WfWYNOb4QesIL8788UPp/sjl/K+x+Z1X9J+Mss7in1zJjK2t" +
                "ra0my+7iWz1IPr3WrjVsr5izeVpeqaDkCBq3UVmLNGnRjRGpSK" +
                "LyUe/15/brCvbSns/ZEe4zd0Yumvu0FwnjY+rfQ33BXumvXxRz" +
                "NstGcb3BXokmds4rcyCiTLXP1+q2uuI34Lfk02sfk3edLSxDo4" +
                "KK46HLI7gmLbqxVSqSqHzUP/Pn9utq7WjtsK814n3k02sfk3fO" +
                "FpWhUUHFnO3gEVyTFp0slIokqnpM1bri4gojfnwaV3res3x8oM" +
                "x7Obb4F4PouZwbu0/7p8vNndw8w1n55aC/m8lNV/R3c1vLPJeM" +
                "f13kW0o+vfb8bm7zHpuLtbxSQX86MZY0adE/fEwqkqh81E/7c0" +
                "vmYEOwgWrY4ZPO2bO2T2Kg7k5SBOBtZkKnc7YEsXYmHsM/LkZq" +
                "4X7qozKpSzZS6ebWGN0MzblN8dQderI9OQo+KgkLFnqoJXkE9z" +
                "qOp9qNwn7A6BptnB1LGe0xyHzapvmt+DnNOcbKa2iBlUxxH/ZS" +
                "m3q4BREuoznaP6nHFcrm2B+uRtcisbJM2hqCsrFZcUbpjowi6u" +
                "NQ4gd7sxaW+fpdjlh23Wk9Y66Pt4coAi3il3HAKHO7G9ciFcI/" +
                "/fp4sJzyUoxtUy8fNWfVcvt8rV7+23Rr8Sv1e+nz2f16k8+WoV" +
                "u9cl4XlbXQ0qOTz0hFvrH4c0vm+vH6caqzkj7Jc64vq3mbR4FP" +
                "MhM6XduriZ3H8Rj+cTFSC/dLTVnpZsQRcIXaDEg9hAnPhmczC2" +
                "togZW8wH3YS23q4RZEuIzmW/+cHoe6OZONAU5Xi8TKMrlVQ1A2" +
                "mjN3lO7Ict/L4cvGymtogZWc5z7spTZ8upPcQg6X0ayzzRwpo2" +
                "xeGwOcrhaJlaWbEZm4Qn2Ukr/AnAvPGSuvoQVWbxb3YS+1qYdb" +
                "EOEymnV2ux5X6D5nf7gaXYvEyjL5vIagbGzOnFG6I4PavdcNPd" +
                "nem4/vm8lnBrwPIsBr36t371s3t0h8p6nfe6c7/YjW79BzVZ0h" +
                "rgozSqXV99M1P4/v/K6T3/Gpn8RnKJ2/pf3PpXO2uPNCb0na/o" +
                "dRdCL1tRDbMc/0ozeY8s3R1dE70rodzWb/W4ci502BxlHDdLzz" +
                "785/0vpk5yzlBd7omujtpk6vg6J56f7+6IbObzpPU3SG7xztTO" +
                "U6nk/3lwpVZjVEVxFf42j0HsNnnkl1/tD5Y1r+1c5qjz/1v2r3" +
                "1k+m8W8zzHOj69PVdiY8Y1ZdXkMLrNbh8EzrMPqwl9rwSc9nzE" +
                "IOl9GMeoojZZTNa2OyVmMqU2RrkVhZuhlBnT3m9KxxhztKyV/E" +
                "nwhPGCuvoQVW66nwROsp9GEvteGTzhmzkMNlNP83b+RIGWXz2p" +
                "islXzJ1SKxsky+qCEoGzufOaOU/AXmVHjKWHkNLbB6H+c+7KU2" +
                "9XALIlxGs86O6XGF7lP2h6sJTzWOuVokVpZuRlBnj1kfpTuy3H" +
                "c+PG8f19CT7b3bwEdl6ptAFC8xBv1ZrTBPFF7Cq9kxG2LQkijC" +
                "yViWUyiVfruW8Xaf1Fu/ACX+d0pbx5zz5AW7jqcGvOeovIWCXB" +
                "q/+u9P4Ml2o+oXkuU+T3UPZfDpwV9YnLP2UPy8i+m+VP70u/uv" +
                "/Ht5WPNqvdq7B2VXA9q7B777tO0hyNge8rGgovBhPad7DZViD4" +
                "QHeA0tsHpfyEq08v4x1rY9uT/nGEMvYxjDXjfOzu1ishbp1BnU" +
                "ckxDsGz5nXzAcWapx54lsRZ+ha3eHbadtcPS5+rk13G+aJ7Dzz" +
                "0dRFjxHkC5Xk/MwfAgr6EFVu8rWYkW1YThHrSAo3uEbNZaWxVn" +
                "f7garlNnUMu1GoKydfOnH8lKd5Suwtz3RPiEsfIaWmD17sxKtP" +
                "L+Xaxte3J/zrELvcTQPYa9bpyd28VkLdKpM6jlLg3Bsu3K27sk" +
                "s9Rjz5LvHNtb455zwz3l/9TQr+HCPeGe1/sdl6qMZXq9MYfDw7" +
                "yGFli974aHk3vQyvvHWdv25P6cYxy96E/rcex14+zcLiZrpUfQ" +
                "N3ivpsMpxzUEvEtFeo02wSz12LMkngkXzyB75g2h6Hbmuy3c3S" +
                "57njwb/WH+dhFHh7vD3TNbST3Pamg7uShnxZrZ3R/OinkyfJLX" +
                "0AKrtzcr0aKaMNyDFnAQnrfaV1XF2R+uhuvUGbTSzYhMfMwas9" +
                "TjRrDn+/QM5Z/OW0Vfq/4WbIxtJaunw8i0vViWU+NKVr0e72uw" +
                "OTue5lwj7vVdW3G/7lq7tn1u72Dv0ybfGvQ3QNdL74aW6fUem4" +
                "fCQ7yGFli9n2YlWlQThnvQAg7C81bSrYqzP1wN16kzaGXS1hD2" +
                "2LnlZuMKeUSwAHbzHRT3R2Ln3XnEBdN8zz/Z4DJyrnJe8Eo86d" +
                "Gik141b1VuWyP//9msO88DHuvneUB3ku7YVz4PWKw9D8DMM3ke" +
                "kHGozwMW9/c8INlYdr5o1nl8c1lzmenNa2iB1TvCfdhLberhFk" +
                "S4jKa1SI8rtC2zP1yNrkVinXKRhqBsbF6cUboj4xHBfNjNEfm/" +
                "4th07r0hLpg/zWPzQZeRc5XzglfiSY8WDffP+tHrx9ga05m7pX" +
                "kLr6EFVu9Z7sNealMPtyDCZTQjeEiP42p0buB0tUisLJNVGsIe" +
                "O7fcbFxj7ms0G3lMUWetbO9NQR+ViOF93cksgiM5jzgvLEUPsF" +
                "EuGYX9gJFMtmqOdVQstccg86nvn7m6Gzyevf0y0hoR12evkU+v" +
                "Pe/ReL3Nj5WhUUHFe0EjPIJr0qIhI/dIVPWYHOb/Ax1okSI=");
            
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
            final int rows = 63;
            final int cols = 77;
            final int compressedBytes = 2943;
            final int uncompressedBytes = 19405;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrVW2usXFUVvraV9LYooVeFxJIqVvEPhtRqjVU7Z87MlQQTY6" +
                "om9cmjanuJtL4VX3Puxo6TVEwIivhofEaDNI0/gCKJvdr4Q00U" +
                "GkpLSwmILwRbCipCIrr3WWfN+vbae5+Z6Z3bhLOz11mPb6317X" +
                "PPzD4z0+YH84MT9sirM2lkffkoxtgrOg0zhxbXEDxqrXWI1Fl+" +
                "XR9DNUMuGqtl2JEr4Zrjq9T1dQbkHmKtd0Y6Fj90XNvdH08s4B" +
                "Fj171sOOajHK1lrWV4Jo2sbAfG2Cu6eNCijLCi03a+Pp6HbOK1" +
                "qWbIRWO1dGsIEf7a0Qq7IUc/Y2KifV37OneefSt7zBKJxc/xIx" +
                "3dub4OzQzqD0Q5TTjFsrs/14w0iuzuHeneunI2mU3KmSYNW+dm" +
                "P6YxfoRjurKg7TV7A2P9PMzBEWI0F4xrTk52f6qrCcuwd6xipM" +
                "PybHlplWeaNOz72aQf0xg/wrE+h+VscdResw2M9fMwB0eI0Vww" +
                "rjmVcp2uJiy9uutSFSMdlmZLS6s806Rhr9nz/JjG0NnMSQbhKx" +
                "ZL2eKovWYNxvqdMAdHiNFcMC4+kGt1NWHp1V2bqhh2CA9zbn/f" +
                "PGuYXcTMDb/j7MwWct/srIywe9FwuebFp9Kx3aPz7NX9OufpWM" +
                "oeJZpGt3vDZCLKaWzFs7OLNKP4WrKL0r115daK1go8k0ZWbwpj" +
                "7BVdPGhRRljRr+7nIZt4baoZctFYLbM1MYS/9vKarQlXGa4MM/" +
                "CAZ42XTjyrjthrM3vNcLnD4rz77/r29U7KtHWagzEk5Zyqncah" +
                "h2tK/ThPxnJl5MIekm4NqXpSK8yTWsjeP6ZXTq9kjaZ9bb4whs" +
                "HJUs6p2mkcerim1B/EE7EdL5tk9zfpelIrzBMmyF7d13d27qpe" +
                "kautdV95lW9QmJPm5Vb+x86n7fyf8xWLKVasqHnN3N051Dlc6Y" +
                "/oaIF6+RRUvKCwu10R3ck6Bzv3VtoDdj7YeajzN3v+u1yzzlOF" +
                "/fxSTBXnWHmuW0PxkmKV9R8Iav2581ewHu/8y8onO/+11+AVxa" +
                "KSyXOLM4rJYlnx/MQraEd1zS7oe27UsZQ9SjSNbu8YJhNRTmMr" +
                "nk1rwEhqLeneaV7tbnU+1v+sdo+OpexRoml0uztMJqKc1ucdze" +
                "4e1IxSa0n3HszLXNh/ph37E2jzV6d731y4jr0WXNWTfW97YmLH" +
                "5WO9Zr8cis10KtK9cORrFu04OzefVbSLdiFnmjQs90v8mMb4EY" +
                "7pyoK2z4S3M9bPwxwcIUZzwbjm5GTYUViGvWMVww51zxG9t4z7" +
                "WaN1y+l+1mjdcirPGshk0LOGeTU+a/TeFjxrrJ3fs0Zr7+l+1m" +
                "jtPZVnDZtT86zRWtNag2fSyOptxBh7RRcPWpQRViy1W+N5yCZe" +
                "m2qGXDQ2kLfGEP7a0Qq7Iccqtr6lvm8mT2u9uan3ZoqJFKwf4S" +
                "pYK6xsfbchXvL8rN7F0o0wcY6aVZTFbZqpH4/+RrI+5eMOhb2v" +
                "i0eLI8UT9vyUfW2+rnio2nPyYXaR4o9R75OB5y/z33eL+2ujj0" +
                "X2zegaij/1tcfH8TxQvKrfb/rZ/73Gwq3BNGPXrPto5MnmgoG1" +
                "1st9ZuCpePaVY+D5xrro7OpIxsUD7pCh7zPzJrOhep0uby3HM2" +
                "lk9d7OMfGTJTpajPQrY7z7D64ueToHR4jxOyISYyK707qasAx7" +
                "+6v0+eiMbIqm/912e44mfDM3ZaYRO+rRPRF81zeFterrZlOOjc" +
                "YLn1h29/jguoN6+xzxaFTrafavktOac3XYyPtH7acSjLKOtVhP" +
                "1Xc5Gi92mNU4QV10pDkXw6ZW2jjhx5obmhvkTJOGvcf2Nze090" +
                "tMY/wIx3RlQdu/+mOM9fMwB0eI0Vwwrjk52Z3W1YRl2DtWMewA" +
                "V/R49V53Sf++Ppm6Xxg7jgNrsV5XX+PFDrMax83GVGS4NVEHP2" +
                "bea+f7zUy4b5qPN8FrPiH7pvlU5euoneXdpdxk3mcuq/bNy3Hf" +
                "NNsSO9InzafN58wXzBdrdq13mQ/yvmk+VPm2QvxK82G3b5qPld" +
                "ZnSvlZe2fgGt5TyktLeYXZTPum+YDZEu243XzEs682n6/e53e1" +
                "d8mZJg2r7bPnfRLTGD/Csf4Osostjoqu8zAHh8aUjFR/iWtOTn" +
                "af0NWEZdg7VjHsALvDHpnV91hfxZjG1uxAe0aJiC/bU183RCHf" +
                "bE/0t7o9um98LXW907Fst8zqmv0EYxpbs6rdo0TEl+2urxuikG" +
                "+2O3rNduu+8bXU9Y7FWh+NW05jK/uKyGEOv2a9d1xHjN1CdTTf" +
                "NNWr1HzdfEc+O/XgjjRfq8723jM/9LLVE5m53ny3/9npW94e8I" +
                "3m6gSD75Vy4L8cHfzZyXwb0D+wewB0NDekPzs1Xzaw9/fNj4K/" +
                "0qLqL7Ov/zfal/yLLhr9L5OqhrVYT9V3NTRe7DArW0RdY5Hh1k" +
                "QdBq1X7jMTvPNlzxE56jH7pVN4talO5mejoMuMmxfmtZmfQ5IH" +
                "e51m9jrJPsHqbLGyKyTDWRKR2rq77p1CmduZlzB003WhXsgPef" +
                "jS/e6Eq8aqsd5hrHGfucNJHuwlzUn2sSa2rws6hpPaYYbfO4VC" +
                "XqyJ5fZN7Kt5+JykGjNI9w5juf18URzJN/Kg72mdxlHS2ELb1w" +
                "Udw3HFsLv0Tv6tS9SOp4UXa2K572mxr+bhc2KLq9T1jsfsNbOf" +
                "Mmjyd9v5DMt8RlWZievaU5enffmMG7XvITPlNZvBDMyPfbcta/" +
                "BliEj3DmP4ewDtAXzNegfG+a45/98DmmeN/ntA95/1vwec4lrU" +
                "byhwzQ6P+zeU5vmn/TeU8xfyN5T8KpnisTvnL9CHkVDXnro87c" +
                "uvcmMQQ8JJBubHPjtxjpbpqrEakTWeSZIHe9nPCLunXyM+P1ss" +
                "wjAuu0YiUhueqSq03zvC+0zkiQzddF2oF/LDHC39VWPVWG8dax" +
                "x20w22WWtdOs73s8bheb+fnV1fIxZtnr0wXBrHzH4nebCXNCfZ" +
                "l20Vn2T7tQjDuGyrRKQ23GcV2u8d4yg1fYZuui7UC/lhjpZYjR" +
                "mke+tYzR7wyNj3gFXz/Dbh1yPvAavGvwe0n2k/g2fSyGq+FmPs" +
                "FV08aFFGWNGv7uchm3htqhly0Vgt3RpChL92tMJuyFEy8mUkeb" +
                "CX/W5m15avpGvF13+H9P4XKKGlJkakdpjh906hys+biqFYjiP2" +
                "1Tx8TrhqrBrrHcYa95PkwV72M8JesyVOI+lnoyUZPk5qhxl+7/" +
                "673RJdV3ixJpZDY1/Nw+eEq8aqMYZhLD+PJA/2sp8RiNXZaEmG" +
                "j5PaYYbfO4VCXqyJ5Z7PsK/m4XPCVWPVWO8wlm8myYO97GeEzg" +
                "h18ZjfxXAhlv1+7xQKebEmVnnNNuOKQhvx5a7yW65S11vH0vtm" +
                "99/j3jfn/S38yPumeefCfHYyvy+v5hZzt5335tUvpOYm0pxkX7" +
                "aYZA6/oubBL6riwZg5EMOSzxwy9+Rb3FDvZ4v72X/gbEGRJpZD" +
                "myMl+mgp75IcLW30Ts4zh7FqjGEYazxs3LduD/NgL2lOsi8z4p" +
                "NsvxZhGJcZiUhtuCoV2u8deR+Gmj5DN10X6oX8MEdLrMYM0r3D" +
                "WD5Jkgd72c8Iy21GfH62WIRhXCbfpUFtuGYV2u8d+VtPIk9k6K" +
                "brQr2QH+Zo6a8aq8Z6p2L5dpni8bXsSh3xdcJgHmfEsYjOt7tR" +
                "+73GdsZVr65jzNd10b38HC3TVWM1wlj+DpI82Mt+N7OdPlZno8" +
                "VT48wDPhYz/N4pFPJiTSzHEftqHj4nXDVWjfUOY42j5kEnebCX" +
                "NCfZl20Tn2T7tQjDuGybRKQ23GcV2u8deU+Bmj5DN10X6oX8ME" +
                "dLrMYM0r3DWL6JJA/2sp8RiNXZaEmGxvlY9Pu9UyjkxZpY5fPZ" +
                "Jp+lthEvq8aqsd4q9n9iG70q");
            
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
            final int rows = 63;
            final int cols = 77;
            final int compressedBytes = 2687;
            final int uncompressedBytes = 19405;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNq9XF2MXVUVvvpkBJ+cKUVoER1tocyUvxZt/Jk9954KmogReQ" +
                "RMH0hMRpAp01Z96Z2ztZ3bGP+woG0VQTTht0r7Ql9KQkg0vqCB" +
                "BGIgINp2oEowsX+SevZeZ5219t5r73Pu3DtzTvbZP+tb6/v2vv" +
                "vsfe6Z27Zak8+0imTOlj2oNH+y1eBAdBNcdk1roCPNJVllxqaa" +
                "E1zH8zfMFU9shZK5YpvaQW3k7cYCDOLUDrJQbDoQ7XJLGimmq9" +
                "AkwwJcXB/38a88GiqIc/u27kKR3u6+0v1PWZ/o/r2cZ/9qMuLd" +
                "N8TWU0HLP4tPfeVgn2731aT1HWGeiYzdN6vSu4tT0r4ArnhiK7" +
                "Yjovg8t1Gb6001wCBObSMLxWbzrES73LJG0skVmmRYgIvr4z7+" +
                "1e01jypxx2ztGUrU4pbUN32LWwYM90MPGcvR+T/aM6E99OYo1G" +
                "tYfC7XxzK8KWkIo4YxfNvkEZPMiXUszb/TaDU80nDVLHDZFQOu" +
                "vEf6tcqMTTVHV4H4evbu4tczAWfWsw3Lvp5tSK9ni/zs/miSOb" +
                "GOpeyhpv5NWoFncK39WeU+EHKxitq3wxVPbMV2RBTrxnR+gjy4" +
                "Nx75gpomj/btaprjXCxEhHaXW9ZIOrlCkwwLcJFm18e/ur3mUS" +
                "XumK09TYla3Db1LW4Jy4DhEdBDxnJ0e9qcyc92GnHkAa2Gxefy" +
                "++Be41GlGKGt/SG44omtppS/ba7Ypu5BrO9NNcAgTt1DForNxq" +
                "xEu9yCbovKT6IuUmiSYQEuro/rcK9zR91e86gSt2RT6yHZ9fG3" +
                "2Dr/7xgOsfZuPNp8FciuDSPyWLwsq3Qxrh7JGxjTceu4XY1O9G" +
                "PZMdg3qzF7j2xyLo8Zt9Ix973CMi7x+gpqxv0Y9+CaJG9g5BYf" +
                "JfUpzuiN5mOUyjE7z2023+7WxTjbuRU9Yn4crR5LxZVQlebttn" +
                "273CuX1+dARJw7tPHns+4Zej7LH20/PfTvm6PL/nw2Ovzvm/mp" +
                "2Ji1Wr3RoY/ZqmUfs1VL8x3dPTrVvtq7sNGo97MHrBiWtqbWQR" +
                "lr+nM8O+7uAb1VZJPzWCTxs32psHwkhUYF8WPuwy7KlEiT5A2M" +
                "3OKj6vsU16Uep1SO2eXcZvNZty7GmeVW9Ij5cbR6PBVXQlWaZ2" +
                "37rNwrl9fnQESc27d17uzcyXMoQa23ltuwlcrUwmvgEUa0n9iY" +
                "7MfVyLEhZqjFx/rXkBEjcYVyL8OeuR6t1s4XINfV+7VdZ4MRv6" +
                "vEnk3Mg7viNX1hiO7y+/eC+nm282VWfr1Ix5HF5SpXWxu+e1mD" +
                "uNHdoPvB7kZe37x682qeQwlqvSu4DVupTC28Bh5hRDvPPir7cT" +
                "VybIgZavGx/jVkxEhcodzLsGeuBxtV2gOEv9nMrWn4VGCfNTTb" +
                "6efWFmN2+WD7VP7Z5A4xJqzfNYzNnzX0xfqSaBQaswHfEJZRNr" +
                "IxW7/sY7a+tURH70ZxzG4a+jy7qpGaLw5xzK4a/jzrzHZmeQ4l" +
                "qPW+wm3YSmVq4TXwCCPaHqyW/aq9a9Y9uRpZi4/1ryEjRuIK5V" +
                "6GPcM88d3py0P/7nRpo3l28xC/O126lN+d1JOUSu23cpvNt7p1" +
                "Mc5WbkWPmB9HqydTcSVUpXmrbd8q98rl9TkQEeeO27LT2Wm7Kl" +
                "Sfsr6abHIeixS1fCKFRgU13/FOcw+uSfIGRm7xUfV9qtfF9oDd" +
                "g+4BjqfZA64cbK/S1/S9B1w5rD2AqfiC3qA36pvEMfshw32Oxk" +
                "xPJiNeV6RPlfvmp70x2xj16ujNTZ81tLB76hthzPQm1jblMurr" +
                "7fUGd8z0Z/TnG42U0u3gEzmVnXLHLHsWkrEhxs0jczlqzS5OoV" +
                "FBIvKzLsqUSJPkDYzc4qPq+xTXNVn+2mzqD9hCpRg2POI+lv2G" +
                "dCwsTyZ++ebjqR56TZ4ERsnSrE/AENOjyiv+3Um1pp6SMZQLY/" +
                "ZUcsyuj/E2jR/ilWjBFmBUySgpTmCI6ckWsgV7b1Z/q9M3k03O" +
                "hXV4KmXNLpN4fQU19+cC9+CaJG9g5BYfVd+nuK7sRGZ/VzD1RD" +
                "VnniCbnIfvto0Pt3ocH5d4fQU1Y3aCe3BNkjcwcouPkvoUZ/Qs" +
                "e7O9dgy+VO2b+8gm58Lu8tWUNfuYxOsrqBmzvdyDa5K8gZFbfF" +
                "R9n+K6sjPZGTtTDldz5jDZ5FxYzw6nrDKvr6AOzz24Jslb3+Lr" +
                "9VH1fYrrys5m9u3r1KGq/4fIJufCmB1KWWVeX0HiGelWF2VKpE" +
                "ny1l/z9fqo+j75kac2TW2iHBKcxb2537X5GNeCNj8yoQv2NYh1" +
                "/bgPP31M74Cvhdt9TeYaMpLKkFuKGDJ0tnS22LccZQ4lqHXu5r" +
                "aqdQthzJkf5TWMEUa0Y3YdR/peblwXAzFDLT7Wv4aMGIkrlHvp" +
                "x/c92Gyufp+m7xjme0399WLMLmkt4aFvE+6rJWFMvD87OPT3Zy" +
                "MDau3//dnI8N+f6R36Xv1dzf7Kpe/G0q73FhHv21HLTJG2DTiT" +
                "ZpNW4bfuu/7XMPJ3+n7fOALJPsn/qXo++30Mp0Ziz7Q1e+REGJ" +
                "HHUiN1Kl2Mq0fyBkY10mwMYpaYbfI8XPUc1ntPyxjK+z2yT8Z4" +
                "m8f38VQPvSbPA6NkqW8hhpgedZBSOc+OcJvNZ9y6NM/UDLeqGZ" +
                "8jjlYHVYMVlKMqzTO2fUbulcvrcyAizl2vi71zfL7J3Onr3rxo" +
                "wPVsT9I6P3xGkecH3QX9k8i++WcB/+OB9s11y75vrmu2b+ofLW" +
                "qteSt7y+4B1QzvvUA2G/dBk+88jXXb9oAUyRtn+k59tf5lHI0K" +
                "unbF1eK/INH3Iwp9sYbt+pES+StgLEoPG4v+qc+n9+sD3F//2u" +
                "Q77dOCvq/E/Fzv44zV3fWLvOxJvjc/wO7NvzDMz8pddU1R/k3y" +
                "Xr0vfxDnWb6PvVlbmz+QrY34WLX572rXgdq/o+f7GfrhYiQYY3" +
                "6/MOMaP5/lD+WPOKvcBCTLXD2p7H5/sBpOuNjBDzXBY6XjgtXH" +
                "kx7Je/f76uPWcbsanXvkXHbOfU+bfYNsch6L1NeacM5XUIfnHl" +
                "yT5A194BYfVd8nObJaAcndN3d/IIZTQ/tlr1rBY6XjgtXHkx7J" +
                "+/vX1set43Y11jxrvCisGQP9Lqgz4L+b7P93QXWMi//9mRqFZN" +
                "8R6eqNkg5GfNTF9nOE0SAij5WOq0ZNDB9PeiRvYG2iN45xNVat" +
                "45Dc9az3Ugynxhf3TNt5OYzIY6nxZK/G5+ZdjKtH8gbGdNw6bl" +
                "dj1ToGyb039YsxnBrrf56ZfyMmMfNY6bhqzI7ZWEyP5K3/Wh+3" +
                "jtvVWM7f5+CKJ7aaEl5L73sR63tTDTCIQw+M7/sh2uUW7rHnXE" +
                "VcoYkCXMRLPm4drubfb/Je86gSt2RTF0Gyn8wr5T33aO/1GE71" +
                "8S2ux75bdV4NI/JY6bhg9fGkR/IGxiZ64xhXY9V6CyQ7Zn+rxu" +
                "y/MRxi+z06x8KIPFY6Llh9POmRvIGxid44xtVo16/ou+09jf6N" +
                "WD/f0TuvLfd3dJlxGL8NVSsh2Xn2Gs6zPetiOLXI//enE97tK3" +
                "msdFyw+njSI3kDYxO9cYyrsTj+D8md+bY=");
            
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
            final int rows = 63;
            final int cols = 77;
            final int compressedBytes = 2330;
            final int uncompressedBytes = 19405;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrVW1uoHVcZ3gkHCiFbW2NF61Hx1jyoQSPVB/VhnzXn1EAwoV" +
                "qTyIkXojS+lqIgRmfO3lsG9lPAF6sPKkWh6kPx+tbzID70ZvFa" +
                "0kYl0cKRtA3WGAQRZ61/1vzfWv8/O7PdM0POLGat//L9t3Vm1t" +
                "z2GQzsNrqHdrtt3T8ot8nlQbR5nMcuuiXPSo/oa75f0sZ4zkez" +
                "pohN8q3HhDlq2+R5GscPz945aHlLnh/0vHURMTmQHMCRKOJmd3" +
                "kdy4ljmjmviz0zuqAuxtaMwtjoMcSgFPGhDvqLsTfOUsYOqwzz" +
                "CS22vomzmH3PUzNT6L59o1kfby/wF/pj78dZBxGTE8kJHIkibr" +
                "aBOi9lmtp4GznvQ3p01AVExlah3xBDPmUuMVb0FzREWDtyMhrm" +
                "yBZ1x9lkp8lxdnNvWw/2EuWh6hrwkSb4hc7Nv/V+bnYQMXkgeQ" +
                "BHooibHUOdlzLNEuTIQnp01DO6HWaj+yafMpcYK/pnNERYO3Iy" +
                "GuZY6ibJxHHlSJRtxXF2HHUktSNjUOM5spAeHXVJt6vynoSNJe" +
                "RT5hJjRX9JQ3A0mDNRpawstID17FB13byn9aP6L72fm51GHD3C" +
                "ezlnH0VdjJ3vqbmGZaNH5vuVKMxXtyUp6vRa5sWOdcnZ5CyORB" +
                "E3O4U6L2WaJciRBXucfYI1yXO6HWaj+yafMpcYK/rnNERYO3Iy" +
                "GuZY6s4l5xxXjkQRN/sk6ryUaZYgRxbSo6Mu63ZV3ufChtnouc" +
                "RY0V/WEBwN5kxUKSsLLXjbWPXUdN/uuhdLV6Usf0UfkfkakDw+" +
                "2PVbNzVkfy/2K9mF7OVi/Leds6x8B5Tvl+jpK4X9JdXrdSFp4Q" +
                "1D9qe52qtS9rXDKvKvFfWPducseayR/U09Z3oNcs6mr2mexcae" +
                "jT04EmWbOWwOb+yZPO51JLUjY1DjOfIhPVrKetTsMBvdN/nMb4" +
                "1zibFxLyPafms7rJk5jCYrCy3U9exTbT9vrv+q7/Wsm4j15+bs" +
                "W22fm/mr+j4389u6Ws9qnp2+/v8fZ3LO3KpxcLk5m75xrvatyp" +
                "zd0XA9e0tLx9lDrR9nr+79ODvQZM6m71ro/uV8ct4MeTRDSxE3" +
                "+0FyfnyFJISwUqZZghx59VY+hpXYxsj8drSyLfSLUX3c8QtxLj" +
                "GWZNwjhiux1wCPsZTnvGc7cuZeRvwN3mv8sI1rwOxHcLd0d9/X" +
                "gPz1Hdwn35nciSNRxM2+gTovZZraeBs570N6dBW8CZGxVeg3xJ" +
                "BPmUuMjfv8Ng0R1o6cjIY5hhZg+w5JtbWtvdj3cdZNxPprwNr3" +
                "d/9zgF6Dct0ctfOMPl0rYl5r9a9+rW973aLdqgaDrWPVnK23/h" +
                "zwWN/nZhcRzXVzHUeiiJs9iDovZZraeBs570N6dNeA9yAytgr9" +
                "hhjyKXOJsXGfv1tDhLUjJ6NhjjQme5O90TXASew++w7puGcsy4" +
                "rr5l5vg76k5+Kv/kuv8Xg9OkcjTB0qzAptOWJYQ1hFzb3E3jqZ" +
                "VlN0bh5v/Ux5qvdzs4OIyUqyUtzx/4xGdz+8Qq2Q/jxZcc8BBU" +
                "dIK2WaJciRV2/lY1iJew6okPl70ap8DlgJG0sobvEcEOUSY0nG" +
                "PWK4kvI5oKrZZ+k925Ez9zKPmfu8+ZO27zXWX27icfbT9u419I" +
                "jLvdcwj5pHcSSKuNl3be85HhljW3ENAM77YDxS69cQqdmFDbPB" +
                "PHUPWi8jek9Ys+Y5zkdagO0T1d/7FyEf0vNta1aXf3W5dmnRu4" +
                "k459xs/XtA/r7e3wXd1cU7xznXzY+3fhXb3/t1s5eIMGcnW69g" +
                "yS+001N9R1TXgKfN0zgSRdz6j23vOR4ZgxrPkQ/GI4VY3S5smA" +
                "3mqXvQeluDRIS1IyejYYahhfaMnr9/sOu3SSde676hmPvMfUt4" +
                "Vb+hLOPRnZsLf0PRIy73DSU5mhz1FErc/hLJuPcYlBXPm0e9F9" +
                "ajZy0meeNYsVWVw1H2Xpc1YuMs8g+ENcTx9PzqZqmupq0vVcja" +
                "u/bp2W7OpGmD43D6aaDPFPvn59bvaph+toHfz9RqPhf5NInxFE" +
                "rcfpVk3HsMysgC+dizqMP4GBwrtqpyMOy9LmvExlnkH4wzDfV6" +
                "flrGPKa/Tp+K17O0uHNML5lD5lDqzvvUvZVOXyr26shL/+uw7t" +
                "cL2S3ZMHNfyLPbwc9qJtYI49bK9Er6QvpiMV5N/ylWmlsz9+U7" +
                "u6PYi9Ure3t2MH0ifTJEpRfTP5dUcf+d7lRyd9efrWDE7HVO9j" +
                "an/236h6J/dv6cpf9RVkD3e6jsDdmbi5k7lrj7MT8SRdzavajz" +
                "UqZZghxZSI+h99Cu+nseCxtmo+cSY+Pe1iARHA2OJVGlrCy00N" +
                "az/EO7fz2b3NLBenY8OY4jUcSNTqDOS5lmCXJkIT2G3kM7zEb3" +
                "TT5lLjE27m0NEhHWjpyMhjnSuLHqfz+blF+rSGL30UnScc+/tQ" +
                "013ssG/KLVy3LxJcbjbZ8mHBmtfDTCbqxKD5yH3a2PVMkiriGs" +
                "Qt+kPhmh/dzfhn647fcaS99/L/5e4+4+3mtk5S/1zEmjPKObGz" +
                "y3e72OMycHPW9N813I547ZwZEo1zaLBrpKugOYnZgzm6XFptd6" +
                "vaM2dTvMRvdtqTgXLQ/Rb2oIiLYZzoSMhjlirjXvgu5v/V3QsP" +
                "f3Z51EnPM7xyW+c+nP6Pm9fT+jN/1t6ELP6MNkiCNRtpnT5jTq" +
                "vJRpaiFnTpOFxYUe3bF9WrfDbHTflopz0fKIexnReyKO8tU8x/" +
                "mEs1R7bn6h7WM6P9X3uZl3cNUZ7Rvt45F2auaIORLqSMp0qCHK" +
                "HClX1CPeM6PJI6I9hdlgkxiUIj7UcS8jUpUe7/ONa2GPMoLcxq" +
                "+t1rPfNJn1RX4X1P82/WIz3Hip35BOy+fN8cPmjOJ75waxyyuK" +
                "Zlsn7fT+7Ewz/fTLS83ZV6vj7Hc323E2TRe2+EpDXLbAejYcDX" +
                "mknZopttHQ/r+T15GU6VBDlCnf11ncaMgxLE0eEe0pzAZbjMk/" +
                "hlLEYw3Yy4i2p/93onrCmZAelQj7R+6rKY20UyuOs9+HuhhD43" +
                "ibLUbwBdZ7ZvRgsHbQY8NIaINNYuJcUM8y7mVEzlLG1jzKCIP/" +
                "AexHQo4=");
            
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
            final int rows = 63;
            final int cols = 77;
            final int compressedBytes = 1334;
            final int uncompressedBytes = 19405;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrdW81rFEkUH0+CCh5EED2IoLgL4iUoK0SZjN05eDG3JQpeFA" +
                "3Crhc9mMOazszEhoy6EAKymHX9SuIYNYLiNf+ChzUooujGKKIg" +
                "8SDqxeope+pVveqZ/qh+29NVdFV1v+/fvOqq6ekpFNQyPOiPqp" +
                "WC4VL6uUBc0rDY09fTJ3p+8Mrs7ZdpKo9M8WmqZsEtxqoclIEV" +
                "86i+QLrqk9d6McjahJfYtk4jtiDK0CPeV1b6V9wDQUgPfY33CV" +
                "X/wtccOF7ZXsfQEzB+yY63rbgr2xp6N4bQuxREcVY4O1vLDu9v" +
                "RjhhOqvdg9Rzc+RP8zqttdZa2PMRPxudhzT/qhiLK/CMS2CNjd" +
                "GMXg56o9fNdWJfVF7Uzug45NjhGbYGfZQl7HF73GvFUa6PPpXx" +
                "xTxeW57jffDn4slxunUH88Ervk6hX6dL8PqafTkozVvrTrA+oQ" +
                "vLCV3C+/Zzs1yvXjWe1Xep5yaNRfu5j9noM+MR3CTHjMSivdTE" +
                "7IXxCP4mx4zEYnWqidkr4xH8Q44ZiUWw16gbj2CCHLMJSszYGn" +
                "DbeAS3yTEjsVidbWJ2z3gEV8gxI7HoTDbvZ2+MR3ArDNfoW2qL" +
                "iTHb3vT9Qxh+vqdFWj6jOf8TiyDhjq+8u+VdZbMGszYWnSWTmJ" +
                "XrtWUav7YmwmyaHLNpEsyac7O2woS+GnhWYU2S388maedmbZVR" +
                "vY9ZBJcSrulrImN2iQQzkWebot/PnJZ7OqtOnmcpWCwVS0XR84" +
                "NXlmdbZJrKw/vynJDg/LJmwc0iuO7zypagDKyYR/UF0sU10WKL" +
                "wktsW6cRW2iZZ13G8+wGeZ6lYNEatoZhz0deZZjtgDR+1esFT4" +
                "NvDp75OrDGxmgKcqpSsl6Zh+vEvqi8qJ3SccixwzNsDfooS2j3" +
                "Gjvj788CPqFZ8jxLxaLzjh3vnafOJ9Z/8TBz/vuB2S7NN6vVSP" +
                "5VuP2Zs8giuJbQ1+ctqR81mGktOgtJ9mfWBesC7PnIqwyzXyCN" +
                "X/V6wdPgm4Nnvg6ssTG6DDlVKVmvzMN1Yl9UXtRe1nHIscMzbA" +
                "36KEvo9mfuoULHl8py4j3tbuM7m/XUmLmHiTHbYxyzddSYnf2U" +
                "wp52oDQgen7wyjAryTSVh/dsTzsAaapmwc1GG3xe2RKUgRXzqL" +
                "5AurgmWveIqk14iW3rNGILwesmy+ujmnVzPsm6mXgORF43K7+G" +
                "Wzer/yby64E/sh92/hqQRgylrlKX6PnBKxt1yzSVR6b4NFWz4B" +
                "ZjVQ7KwIp5VF8gXfWp0Xar2oSX2LZOI7bQKs/cYznYa/RTPgti" +
                "qE50PmY0MQDMTuQAsxO0mLm/5WBuHiPOs305yLN9xHn2ew7y7D" +
                "gtZrW90Z+f2S1l7L3UmIW1mMwzgJkVw8fe+NRUMOs1y9f2O7od" +
                "Yy6cDtT7mM32k8kQiP5b3ch8SL8HDeVZDOzt7vjUVPKs2yxf23" +
                "WT/M30FNZNkhgAZv05wIz4u5N7Ogd7jSHiPDuVgzw7RZxngznI" +
                "swotZuFKlN+E3TPUmLl/ZBGzSBE45JgR3896YjzBd100O2YCZk" +
                "2Mt5zcs1ElehaJMftmem7Sl3Ax/L9zs3QxY+vmxexjVlzMFmbF" +
                "xQ7AbDZjmM12AGYLGcMsNX+K98NiBjnj0JOV3vVJIsvsGjCesT" +
                "UgFX+C39cIKS+9r1H8sRvLyvsaRe3uMNl7jsbvZ/eylWc0/iSc" +
                "m2MZm5tj2cfMPZ8tzNxzHbDXuJuxuUnij/jdSVfwfxEVH18HrQ" +
                "HefxGTluj/RfT9CYw2zhrwHc89Cao=");
            
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
            final int rows = 63;
            final int cols = 77;
            final int compressedBytes = 1493;
            final int uncompressedBytes = 19405;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrdWk2IHEUUbhHdq0EIQbwIgqsH8aK4SqC7d/oQ9JhDbh5CTp" +
                "5zEAxmZnbGnoOQGOJC/AmYIOaWxMuKS3b9yRIW9JCAYBQPGwkY" +
                "gogi4iluTc3b96rqdU9VV3fNTxdV/erV+/ne666u6umJInHkp6" +
                "O9o/1ZVHLkp6LSI/48mqqjOTz5GeucvT8G489TlrMgeMpzNhbj" +
                "pSnL2aUZyNnVKcvZ1enP2bQdyYV65cblLLkzBzkLEkP/ob3n/e" +
                "rs52zlcv02s/PZeTzLKovk0jFdRh2BMd0ySu9e9adAVvdEJctk" +
                "dCwqWhWTaE2PiNL0zVk0PTBz86vmrnp+IfR9ln8aZA14vsGnyx" +
                "PBn2cNeOwvGJxHaW/5YU/7j9RpzV2f1/DDkR3NjuJZVlkkl47p" +
                "MuqIpNKBbhmlkdb1qA4tpoyORUWrYpJ4dGsQFUWoW6YWTQ/x8f" +
                "j4cO83PMsqi+TSMV1GHZFUfmVvNzmygdJI63pUhxZTRseiolUx" +
                "DfG8p1uDqChC3TK1aHoIu6fNvwj9PHv3SJD92X7aS29N027LHQ" +
                "2vUXdU/QOK9ZtTlbOb9WjUHRXuNdIKu6linU7PH1v3Sn14qh+t" +
                "a61r9Cwp2Usv0jHgIo0cLOlFqWFaVK2rehQNb1vaNLHosnorYj" +
                "Al1Nhpz/RGMaoa3BqQfz0H75sLjez77+3W++3b7b93z/+Judke" +
                "/RaQvmGlv8Ny/zU4d2vA+mvp6J/M3GRjaP+2R/01iXen7qal3R" +
                "/9sXUed97dfDvL75siZ/l3wXN2JGzO0mN12q1j3aywjh8Lm7N8" +
                "q+77LPzcXPkjcM6+n4OcLYTI2crtBt83fwj+vvna5NeAzj6vNe" +
                "BWk/fZySenM2d+e410x+t5vuM+N2095ncavM+escuZuaftLEbR" +
                "8lue75sHS7E9bfLGeYQ9bf5Pcznr/j4mqnsjrJ9w95nJbXoNGO" +
                "cRxv2QkW/Cp+qcm5M57GLwfp5hzk7P/jt6MzHE66KKAn2gBo/Z" +
                "6ttwpR9/rG6jvaVySV9Eza+brRdCP898PU72HX0yv9Mm7Xrlht" +
                "dhvbVOz5JCLo4BF2nk0B7o6RZV66oeRcPbljYHizoWXVZvkxOc" +
                "hBr7MGcnzCjNyKjGHN9nJ+uVCzs3J/OOnrxTr1zYnE3mGDxrKf" +
                "ecU44KvwckiZX+mO8BvbMjzt0o6r9CdgFnKlxP5+8BfAzm94De" +
                "fsf3i0XZQgGuoPoHRQs8lNW1aQ81VDm0bWqovoukEBdS2ONanq" +
                "tHTa1yvovH5nFuJm/XKzdublrqO3yrS172XEec5+bgpYbm5gHZ" +
                "QgEu8EGCyuratIcaqhzaNjVU30VSFBdQ2ONantvZVKOmVjnfxW" +
                "Py6C/P6jzsp8yc61jOzY7jfXZYtlCAC3yQoLK6Nu2hhiqHtk0N" +
                "1XeRFMUFFPa4lufqUVOrnG99LN4WVRSkR7l/1eq3hm07Ltj3+l" +
                "1j23WUjwElqyEKugYshV4D+O/ovv/XiH8SVRSkR2vOkq2+DZeX" +
                "c8fqNtr7oFyyCqrsQfaAniUle8nrdAy4SMvS3aQ9sGFaVK2rVi" +
                "ga1RqlOCy6rN6KGEwJNXbaM71RjHAOOTe93/Pd92dxE3Mz5F4j" +
                "a/AbPb/XaMZjyPts4Hk9uted77O0mfss3ZAtFOACHySorK5Ne6" +
                "ihyqFtU0P1XSRFcQGFPa7luXrU1CrnWx+L10QVBfpApW9arWVr" +
                "lmvemr1sNV/cKB+DN44vRRUF+kBl52z1S59h56gff6xuo3wMvj" +
                "jo80z+FlTH84yR232eZb+EXjd5j/g8q3jtrosqCvSByj6y1S/j" +
                "ghXpxx+r2ygfA0pWQxRviSoK9IHKPrTVL+OCFenHM2dbrqN8DC" +
                "hZDVG8Iaoo0Acq+9hWv4wLVqQfz5xtuI7yMaBkNUTxDVFFgT5Q" +
                "rfu2+jZc6cczZzdcR/kYULIaovgbUUWBPlD2+kXcZFX345OxZL" +
                "VcnxtNVsslqyFqZt0seA84FPy3oBcbeA/4H4usccM=");
            
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
            final int rows = 63;
            final int cols = 77;
            final int compressedBytes = 1910;
            final int uncompressedBytes = 19405;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrdXE2IHEUU7pBTEE+LHs0eBD2J0VMEk52e6d1Lxp+bevGYQx" +
                "CJhyQXAzOzmd2+xZOSg4qwQuLfHuJfIHFDggv5OeQQDx4UVIyG" +
                "BEQREYJxqt+8ea9eve6tnpouov2o6vfzvZ+q6Z/qntlNkoWNZN" +
                "QMJcVGnN+mo10t5EkCtpUXq/01a75cjZyuot7NUbvV+7b3x1h+" +
                "rPdjLf8fVO1fjubnJOn8kwRtve8qrb+5Oj1j76cJ9/s0dSxcNM" +
                "0Qysj5+/toIU/YnFX7a9b8jWrkdBUtnDXNEMrI+fv7aCFP4Jyd" +
                "rWvNPqpGIqfjSjOdN80Qysj5+/toIU/gnJ2va81OViOR03GlmS" +
                "6YZghl5Pz9fbSQJ3DOLtS15m9WI5HL36pVxznTDKGMnL+/jxby" +
                "BM7ZubrW7ONqJHI6rjTTpmmGUEbO399HC3kC52yzrjX7pBqJnI" +
                "7z3YbtevhBjRVOljS5DdNYGRcum2aI+Lr+PlqMH1prPWv2QTUS" +
                "OR3ns6bt/d3smjbvxl7Triw2saZt5jlAwZnngNPRnwNOV8/ZlM" +
                "f7p6YZQhm5/G1ff/884bXWs64uhdR8L9wDWpeTBjftHtBsxmnn" +
                "rM7W2ow+Z41mbB+gRhqp4xaXl5oqP6lrHzC0VYWAIw/uX+Uj+/" +
                "KoWgxpW7hqmiHix9ezd72uMVf9tBg/6Hp2ta41f6caOV1FC1dM" +
                "M0Q8WNLHff19tBg/aM6u1LXqYyDkdBXFXJ+1Ao+z+msNPWPo+m" +
                "x0vr4MPRJqUY+I4i75K3lw74n9JveQOBvL9XbuMhSvCzmStF7X" +
                "ylHzqFpuaYu5ps2fjX2c5c80sqb9zDRDKCOXPuHr758nvNZ6Vn" +
                "0MwXV8bpohlJFLn/f1988TXms9qz6G0DpG5+tD0COh1nDDl0yP" +
                "OsJKby6Rh42j2K6HnbsMRXURR5LW61o5ah5Vy11uw+tZg88BV5" +
                "LIWzMZY641hk9Fvwes+601jj3oX0W6lq6178c9NCDQDm6BBhBg" +
                "R568SDZexfE89sIcxmIIsela/hz5gQ2RRKSBuIPbshZC8mqo5x" +
                "gaSX8DKzRxIbadjyJK2TqK71JzjvC75bL7XkOLUG4hXetuuSfH" +
                "cw/y131By236WKpyu7aozwGXoj8HXJr9c0B2JjvD98CRlmyoJZ" +
                "40XEI/GbHg5nQ/Xo0eu6wWiXX6OQ1hj51LbjZ7PrhHnPtm5+vY" +
                "980mMg5fKz83WzcU/MGg++ar0c/NGw2cm3eyO7SHBgRabnMx3E" +
                "JehKEcwHcu6H68Gj12WS0SK3s3I0biFcrIfFakrH4Gld8vDI8E" +
                "nSkXo5+bUTL2d4xXWLvbu5NkeabfQZiIIVv+ymwy9jdCqsjWs3" +
                "XaQwNq72rvytbzg2STGNuCNhmZ0KMR7EKs7cd9OLkYWQu3y5pM" +
                "72Y0PcyZm1uL6GYoP86a2NJbsc/NOBnrrjV8v9/sfRM8/u/7c/" +
                "V9PM/6wb36jJ4PA1dGr9dda6y8MPu1Ruda5xrfA0fa9DraUEs8" +
                "abgEfm7E4lO/rvvxavTYEBP8eS0SK3s3I46KV0gSz+aOzPZgn+" +
                "WgwavLyejXswYypkfTo7SHBgRabpMY24I2GZnQo3NzFbEyE0dW" +
                "YWQtdrV2TabPV2Q0qtLNrUV0Myj3zQea+9Szy7GPsyYydvZ39v" +
                "M9cKQlG2qJBxpscAljuBGL836JI6WXHdfGlNUisbLPj2sIe+xc" +
                "crPZ88E92PVsJfkfbcNjnrhV/5iLOxd38j1wpCUbaoknDZfQT0" +
                "YszpT7dD9ejR67rBaJlb2bESPxCvVRuiMb2+YX5wtpvAeOtGRD" +
                "LfGk4RL6yYjFnO3Q/SZ1z9vEq9FrkVjZuxkxEq9QH6U7Mtuj+X" +
                "eO/UdH14MTYTEGT1dmeFi5Xp+IcQVocs7yk7HnTP/d9r01Z9lS" +
                "1ZyVW5uaM9+MYZVVz1n/kS1y7y173izmbG/0OdsiIz5vhlXWe3" +
                "/8tq7b7rrvHNtb/FUE2jUcRAx65/he7XeO3ep3jqEVzeTc3FN5" +
                "bu6JfpztCa1bwW7LtvE9cIbarXYr25avoQ21xAPZUrsFMQzOjj" +
                "j6TFugdf14NXpsw43m7Euu1eqQvZvR9OP3tJN6aQZ4Nhmfz9JM" +
                "j7N901sbed7cN1vcVu8cW7eVK8pXdd85Ln9I7xz574KWT01x3a" +
                "j//eZtv3eOdX4XlG3PtvM9cKQlG2qJBxpscAljuBGLdyl9jpRe" +
                "dlwbU1aLxMrezYiReIX6KGV8xHQOdQ4V6+XxHjjSkg21xJOGS+" +
                "gnIxZz1tP9Jqv2QzbxavRaJFb2bkaMxCvUR+mObGw70im+5cU9" +
                "cCCllg21xJOGS+DhRizmrMRvUvcRm3g1ei0SK/tURVA2NmfOKN" +
                "2RjW2HO4cLabwHDqTUsqGWeNJwCTzciMWclfhN6j5sE69Gr0Vi" +
                "ZZ+qiPwLe8z6KN2R2R5slfPk5Pr5i3IP2OK7/Nz5HQ7eAxz9qR" +
                "j3TW0MoyqD/mdF2k27tIcGBFpukxjYDzbII+26kQlNPI8ofTi5" +
                "GFmLXS3qqB8uymhUpZtbi+hmqDzO/vzvv6dtZAz/AhHiqMk=");
            
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
            final int rows = 63;
            final int cols = 77;
            final int compressedBytes = 1651;
            final int uncompressedBytes = 19405;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrlWk2IHEUUbgLqISfxBzR4E/QSIccQNDvVPSEQkouHBFQkJo" +
                "KXKJsocf0BZ6d3x52LhyDI7mENC4FNgggGJbc9qax7kfwIIQaJ" +
                "i8TgQQwYlFXs7je179Wr6p6urp7K9lpFV1W/9733vnpd03/T0d" +
                "HoaJCUaNDDKFqXok5KcYwSrGNnpR33qHpX7YKAazkmjwvH8nbs" +
                "rAmB0YKAxlZnyf1zCyydZ4KCMvlUUFjGzg283E3b+Ali+XTgXL" +
                "rPFnJ7Mp9PXun8UTZ2/Fi8LdfLGTma+T5ofIk/9xGleJ05+L2a" +
                "rO2rbj4mH7K1cI24AXJ22XvOLjc+Z5e85+xS03PmWuxz1tpWL8" +
                "5/zmau+c5Z71DT15n4x3fOXCNugHX2o/d1drjxObvhPWdvjGQu" +
                "t5Ptt861zp2k/yvNWednK/ubRuldTfJLkrObjlwLc975vez5rL" +
                "Nq/xzw/7luio8a/9tc3ezXTfGp/TO6tNF/m/fmGd00h2rP6OVy" +
                "1rq/+b9NT8/oZ+je9Ct1+hZ33Ox7j9cTcXJpZL/Nw81fZ6Lx92" +
                "fRtPdn9OmNmLPukkXOet5z1mv8Ohv3nrPxpq8zcd37+ex6vbgM" +
                "Oy7GsYcNKkjDLajjGFUjddwzooMg3CKxPBJF5mPAXo1P2aqcgD" +
                "/3JmdFGarWqkc9guFYPrLu5WJy5/6T5XG9WF1bYk1b25sj1n2v" +
                "Ib7QR1WsTWXmz8Bz6e2p36c4L85jDxtUkFIdx0DfXUILwKueEZ" +
                "2MViWWR6LIPIxY1bmobKWMtFpEZEljz/yd51GPYMjiZ/rI4hgU" +
                "2gin92dVrMUNH6tZfXaq77qZzeCWU85u+bGpEOUrfVTF2ng++9" +
                "f7+czLuyDxpT6yt+5+Z9KGH/jO2bCI3WXo+3a/Rat321M/aPYW" +
                "77b33Od43rB+t22OqL/bnp53utcuvDqHp4cc19NFuGHWI1hnTn" +
                "xrytniEA6L+bhwcZj1CHLmwDevtC+0L2APG1SQUl1au7/iWNXA" +
                "qHt7wGFe+kC0lFJrRCGyCEOlFK/qsNUjylnBXjiv8tU96hE62r" +
                "m68439vUZnJfcc87VyVN+r6X7o29LrrGTE/gMW17mdYif2sEEF" +
                "KdWlNVzAsaqBUbgw4LogfSBaSqk1ohBZhKFSild12OoR5axgj/" +
                "PVPeoRPK+zk97X2cn6z5HRkegI7WGEUrEidSAVKziWOLonVqQd" +
                "95gcpRWQ6naUjdl3Oko9UKmJB2/1iHJWsCcGR1v3zPlglqKJaC" +
                "LbG/QwQinqpBTHKKF70o57zM4aW8126zmbUCtlY+bCsbztHTIh" +
                "MBpZPdos9ZlBL+bEXJbnrIcNKkipjmNUjdStn3nm5B76C9+XWB" +
                "6JIoswnIvKVuWUtnpEZKnHNnk0RJgVs9le1sMGdSBdRl1Wl3HM" +
                "NLMSn1kuSx8EvQxSao0oROZjpFeuVeegtFpEZAnzMXmmHvUI4V" +
                "q4xs6amQSkvEWsqpE2Uq/ukSv6wxxvjo7RAKPG1iPptrJ8+CBn" +
                "quqN1421PJlpTlX+Q7Ep/Ud9PwdMro3gLcYxcQx72KCClOo4Rt" +
                "VIHfeM6OQcul1ieSSKLMJwLipblVPa6hGRpR7b5FGP4L7ObN45" +
                "Rjt8rzM/EfE9bWsqaHwZxRzEXrEXe9igJmef7aqOY1SN1HHPiM" +
                "Yxt6M2tOoYzoXqOae07Z3j3pClHtvkUY/g/n/ABl9nO+rFpSV+" +
                "O36TSda/dG7tM1z5nrNlPfXxwO9xJj9ln4H4Leuc7av/OBTlrE" +
                "ppt3MjHS/S1pGz6Ss2fKrhZM7id+PXjetsrMKsJopy5nyEC3MW" +
                "G+4Dys4hfqeenFVaZ7sK19ku3zkrG9GGWXs/VCpxytn+Io2b72" +
                "H2Jm3ZiDbM2gegUonTrA4Uadx8D7M3actGdGNG7ml3G66brSHP" +
                "d6/mXTc1+anAQzHNIZnFbqd72oPiIPawQQUp1XGMqpE67hnROO" +
                "Z21IZWjumHnIvKVuWUtq2XuDdkSWMDzuRRj1D3Pa2+zu7xPe3L" +
                "9eJyZv3apnoOeLFeXLbaTogT2MMGFaRUxzGqRuq4Z0TjmNtRG1" +
                "p1DOeislU5pW3rBe4NWVK/gDN51CNs9nXWj0rirJ5Q8r8Lan9i" +
                "uNo5fRfk/J7K+rsg0xxM3wVNXaknZ8Y7BKec9Z/3nTPzN8h1fU" +
                "sltuKGEj4y7+tezFjTmEbJ90u9q7x0P3r0PA4UUTQnpvsPldZQ" +
                "nA==");
            
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
            final int rows = 63;
            final int cols = 77;
            final int compressedBytes = 1871;
            final int uncompressedBytes = 19405;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrdW82LHFUQX8UYBQ8SiBdFYhDxYrzmuPO6Zz0seAlZI7qiBg" +
                "9ZkYh6EAmbnf2a3RHBgCjGQ3IQk3/BWy6yKkIQkUgOgagIMYua" +
                "uBqiEezumuqqV1U90zP9kZntx7xXrz5+9as3/T27ExO4OfaRm+" +
                "szz2vtje364Govztf1yOj61uJ6spa21q/RZ7N1sfVnNN6MPvta" +
                "P2UzXj4hNa0fLb/WDaX5Jcr+yEShrXWpp/UPo14zY+vnVLrere" +
                "v9Yfi4++hDGinZc41i+1oyz5KNy9F9XhpHZ8/iwD161SRtg+1n" +
                "xnc2wH5WdBt8P2u8mG8/s/0yM9V5bO6p/djcU8WxOdiarVy4ff" +
                "uZ2zXEmu3Kt2Yr3xf6LvcN5r90boCqHy22asvv9rSul59x0P2s" +
                "88z4n886M/n2s9LW7NnROjaHWbO1O/Kt2eqp/Cza967eXP0b1q" +
                "x9dxL9L67Z1IPDV9e+R2pW/yu+Zqv/9Mx5l9b1q6G9I+nvLMZr" +
                "8Q2Umq9NjP1WRQ2N043TNMY9tear2haPXOZRYJPI5E2yjOMxvG" +
                "kfyYXbJae4j2vw0bACztBaAcnHrylrP+vMjv9+1nm+jizt3XnP" +
                "BTlZv3A716ycGvqez95JzwWvb4PzWQU1NHc2d/IRJJg1j3Jbqt" +
                "1JPtyCM4jQiD66H8fZ2NiAqblIX9UftTz82vlMZ+Mc/QjrOaA5" +
                "V+r+u1ICxvrA+8RcBWevHvdnhXDN+7POS3Xfn7UPVXN/NnmGxs" +
                "kzuJ+BdvIMWn3fLA3Hkn4+1tT92bYsjv24+H5Qj+VPFulTxXuN" +
                "Ys+bzY26n53sjMZ7jQfKeK8R7A/2R0fTy2Wez2LEes9nRTPmfF" +
                "+zoqVycevEKLeGLuaSW6IRPtBAy23ah1soinwoB8jNL+04zsbG" +
                "zuIifWWvMyISZyiR+arIedd7A3psqI0l7FPkDeq5hs8ogsdybB" +
                "6HWaRdfLcbPiPOUDOT2S0tr5qjWrldn/Nw67Mi72nbT/a8W/qq" +
                "9ueACjK6U+4UjfCBBlpukz6+BW0SmbyjK8or6Cszcc9ePpKLz9" +
                "bnFPftQxKNWOrcFqLOoLflixW+ZZirez/rd0871PuzHY0dNMY9" +
                "tXjuLvi2eOQyjwKbRCbv6Hu64KOThXtm+0C8n5/skhPwl2io5Q" +
                "ytFZB8yMcdcAcSNskIH2ig5Tbp41vQlu7rB3DG8K6hr8zEPXv5" +
                "SC4+W59T0quMxFLnthB1BuO+cbd3NL1V6tnz97qPTTtj580imO" +
                "HB8CAfQYKZO8ttqCWZNHwGERrRR/fjOBsbGzA1F+kr+7gG7eHX" +
                "zmc6G+fYtc2EyS+AOIJEWrKhlmTS8BnGScTkyv+FHZfynvEbZ2" +
                "Nzkb6y1xkRiTO0q9SV4VjrM/r52p/Rz+d7Rh/h9xpbta/ZVhVr" +
                "pq4BnxZ5DuhzV/5N7c8BFWR0s26WRvhAi47ea75N+sC4dI4i3K" +
                "xGJm+SOaKM4U37SC7cTjrq4xp8NGKpc1uIRobD7nAyS0b4QIvW" +
                "7Lpvkz6+BW0ph8M4QyvJMo7H8KZ9JBdul5ziPq7BRyOWOreFqD" +
                "P0OjbDrbKPzfq3cKsK1OxrQPhX2deAwlwHvgbYNZR93WyH6d9S" +
                "zY//mq09Vf6auWPuGI3wgRY9kc74NunjW9AmkcmbZBnHY3jTPp" +
                "ILt0tOcR/X4KMRS53bQtQZ+j1vjvu2/lHOt1THBzhHHgmP8BEk" +
                "0pINtSSThs8wTiImzJbtOM7Gxs7iIn1l3z5kefi185nO5q9HYp" +
                "sPk7MWjiDFLZgOpsP5zgraUEsyNH8WTANG7OcjTkwAohWX8p73" +
                "G2liKbpGf861Fg/Z64yAhNmAL60AzybxU5+FcCGZdUeQ4ha4wH" +
                "EbakmG5s8CBxGxn48YrZkDrY5LeS/4jTSxJLlYPGSvMyISzALH" +
                "c3NkyYf5HA+TIxlHkEhLNtSSTBo+wziJmJxDH7fjUt7H/cbZ2F" +
                "ykr+x1RkTiDO0qdWVd21yYvKPHESTSkg21JJOGzzBOIiZr9rAd" +
                "l/Ke8xtnY3ORvrLXGRGJM7Sr1JXBOLV3am88wxEk0pINtSSThs" +
                "8wTiIma/aYHYe8OZLvk8VF+speZ0QkztCuUlcGY7AZbCZ/19Ad" +
                "QSIt2VBLMmmoNRsYx1FQajbsuPSvKzb9xtnYXKSv7HVGROI1Iz" +
                "O/Somfxl8Nriaz7ggSacmGWpJJw2cYx1E0uh+XrtlVv3E2Nhfp" +
                "K/vGJcuDsqW/Ol3SVerKeIT3XLFvO93TNr4t18/egh+Gs1n2fv" +
                "7lbla2xnf5mKBf0fcajd/G/xndrqHsd9vFjs3Oh6N1bK69V+/7" +
                "s8aVbbCfXcmzn608Mbq/O7nLhX67uDzE/wlfzndstp8raT+7tQ" +
                "32s1vj/vtm8Hbda2ZnNPazI8OuGVwDylgzwy9as87Jutds/ene" +
                "azbstviJh5f+bWg7+o4WT4/P/evCQ0ZtJ6vIVO6x6T4YrfMZ8q" +
                "n2/mzx60LXto9Ha9+rjs/aibLWbPLsaK1ZJXz+Bwi3kiU=");
            
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
            final int rows = 63;
            final int cols = 77;
            final int compressedBytes = 2255;
            final int uncompressedBytes = 19405;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrdWsuPFGUQH4NsvLjEmLCSGOOBsHjhskA8GDPT0xsTk8XsjZ" +
                "NH/wJ8JBx0GZaF2aO6GB8oiDEhSqJgfBCS1ZNmZbOs8YEYJaC7" +
                "Skh8HYycnO7q6np+PTM7PSPQX6a++qp+9auqb5qe7l7i0Xi0Uj" +
                "n4fJzOyZFouEKZzGglnSw0qhcxTjNKdhlXqWivxoRq0Vgtqxc9" +
                "BGWrVHhu2aXmp4ipq63Ptanvp/5uzf+2PtumrlS6OKYu81V0OL" +
                "P+Y3ArlZ6PqR8LvX9YG9ajkD/n2l/dVxFvjjd7FrBqSVhYoYe0" +
                "6skwM/dgvGTlnJQjVKPEyVg8qidlDzqfX1/IlsVvjbemq2wGja" +
                "zkQyvpZKERzWGcZpTsMi6vbKscvBq/Fo3VMprzEJSN7YrpUvMj" +
                "pnq9ej39PtI5kTSStfaBjXQelWrv5t9wxkxo0nUcj9EVSIyuhf" +
                "t1TVCPZsMOeIXeDuh6CBOvi9epszC1gFVLwkoPxnAuy8w9iPez" +
                "UzbAhFCyKh6LR+1+Xan0E46zezXzDOX+BuTWG+Q3oHZfZ78BPi" +
                "7wnQ/FQ3wGjazkQyvpZOErjNOMaWWb/Dhejc8dqkVjtbQZkYlX" +
                "6HdpO8t86+P16SqbQSMr+dBKOln4CuM0Y6I1j/hxed3r5eDV+L" +
                "VorJYHj3kIysb2zHRpO5MRdNTPidUQX0tfu1jHP9Tbv83ieC+7" +
                "H9FbHfXl+jKfQYNVdLa+3DyOK5oJwz0worPAQXiuJYx+nMxtMc" +
                "DZ+FjXorFa2ozAJHumFc9mK5QRLHYxv4f+RK6l7sdCTOgo9vZ6" +
                "eNX1J2P4dzM6uoZnp6M32LPT0fKfnern6+f5DBpZm3twRTNhuI" +
                "eiOIYY0w4utIuTg1fD6/QZPGkzJnLfvOzZY9b12Aj2HWzjq+ae" +
                "Ms/p6LvKgA8/I+zZWo94Ip5AjVvSz+NgI4kYbmvMJxEcqZm9nM" +
                "BGuXRUXsMEsYeq5lhdRfMd2YPO59cX2qUsfjKeTFfZDBqsoie4" +
                "D62kk4WvIMIySnYZl9c2KQevxq9FY7VMerAIysb2xXRpO8t8O+" +
                "Idak9TC1i1JKz0IAvnssytb/19jqc4G4XZABNCyaq8KmZ260ql" +
                "3z3PdoRsEF+/Wr+aXt2yGTSykg+tpMNozPMVchCea9EKR+ooyS" +
                "sxiRat2Fo0VkubEbviPftdav4cs1pfTVfZDBpZyYdW0mG09oyt" +
                "kIPwXIsEUkdJXolJtGjV1qKxWkYugrKxPTNdan7ExMPxcHrWZT" +
                "NoZCUfWkmH0foNYCvksIxpzmc5UkdJXokJ1aKxWtqMyMQr9LvU" +
                "/DlmQ7whXWUzaGQlH1pJh9HaM7ZCDsuY7tmTHKmjJK/EhGrRWC" +
                "1tRmTiFfpdan4dwa6aH3Z3t9Lo4l6n/tSg78/6kbE2XBumGT4w" +
                "wBotkS8Z0RLp0gNagk+vXEvIQehoCaw8mlCEDGOQVXtlD1zajF" +
                "Ql9OMxc0Ynw2QtvfOAGT4wwBotki8Z0SLp0gNalD0nJ7hadveD" +
                "3mgRrDyaUIQMY5BVe2UPXNqMVCX04zFzRpuh6Bm9+Ys9L6e/vb" +
                "nebU/PdfaMPv11T3Vtq9ywx/7ZQu8h59qzt8Nr1N6u9ujW/hvK" +
                "zg7/hrKznD2rjfZzz5orA9mz0c727MDrvXyXjXvEu5QF9W5loc" +
                "27l4UwLlpoF932zc5CORH0Lqio3rXsWfPXyk1/NFf7cH+2q7aL" +
                "ZvjAaGlbpE9jpAd9mpnQpOs4HsOHxehauF/XlMotmo2qtLk9Rp" +
                "shHolH0qeCbAaNrORDK+lk4SuM04ySXcblzy8jcvBq/Fo0Vsva" +
                "mIegbPmOjdkubWeZb2O8MV1lM2iwqj3KfWglnSx8BRGWUbLLuH" +
                "zPNsrBq/Fr0Vgtkx4sgrKx503Tpe1MRrDr42P5k+e5W+B6FneI" +
                "+72ne6AP8vP14bI7mFke9J6V3wMd49fGryWSPq18E+0xIGkOcY" +
                "dx3IKcxO/XiVhk5rWgBWTSQ4iPuHQc5+LVtz/PvOPAM728C5r5" +
                "qqe7s5/6FzN7ZxfnV3O8STN8YICV+zRGetCnmQnd0i4iVmfiyC" +
                "KMrkVWK2tKpclIVdrcHqPNUPgbcKH0q8vnA7+eDSQj27MfSu/g" +
                "i4HvWR8yxg/FD/EZNLKSD62kk4WvME4zpleN2/w4Xo3PHapFY7" +
                "Wc2e0hZO98ZbPJ/YBZv9doPNi/d0Gztw/6XdDM7s7ea3RZxwDf" +
                "n83ecWvsWeg9bX2sPlapHLpU5pUgYRzs4Wfs7f8FdXuezfzWy3" +
                "kWnRj0eeZntOfZzJreGdVO08fzFa079RZz104X81oUr9ePBSv3" +
                "+b0U5Q77aqfo4/mK1p16i7lrp4p5LYrX68eClfv8Xopyt68rnr" +
                "daKXc18/8HR7k9BLN8ms1folYu72A5yu0hmOUzq5XLO0iOcntg" +
                "byZebhzJtLnGa/7fhBsvZr/Sd7X044VsLzTeyPVX2G/VN42Xxg" +
                "NvMRvp/0hvvN32ue7uIu9z97Y4XmWsb7aeqlnGxuGedulY4612" +
                "92cdc3V4xdg33brbe7rHb/e9rs+zP/vwL/FMfIbPoCWjvr2+PT" +
                "6zfwF9YE1mwnAProDDMqZ3mNv9OF6Nzw2cs+t0LRqrpc2YSLin" +
                "5Zm9Lm1nMmLt59mNfYyfKBeX4z8CiQOtiYZSY3U0X1GExBG3jZ" +
                "C5wyiUvEJbGcmwlXfNWb3cYZ8+z2ZHbv7z7OAjfXqO3QQSB1oT" +
                "bf9CItFGWB3NVxQhccRtI2TuEOrgCNZFFdLKk741uZ7xrjmrl9" +
                "v66g+AxIFWtCOCY3U0X1GExBG3jZC5QyheF2q08qRv1V1zVi+3" +
                "9VUvg8SBVrQjgmN1NF9RhMQRt42QuUMoXhdqtPKkb9Vdc1Yvt/" +
                "VVT4PEgVa0I4JjdTRfUYTEEbeNkLlDKF4XarTypG/VXXNWL7f1" +
                "VS+BxIFWtCOCY3U0X1GExBG3jZC5QyheF2q08qRv1V1zVi+39V" +
                "WvgMSBVrQjgmN1NF9RhMQRt42QuUMoXhdqtPKkb9Vdc1Yvt/L9" +
                "B2LZZk4=");
            
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
            final int rows = 63;
            final int cols = 77;
            final int compressedBytes = 2110;
            final int uncompressedBytes = 19405;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtW0uIHFUULRezFUUQhYkgGGI2Ab+Jg6PTXd0TRwbcxRBduM" +
                "liwGTID3QSJT1dk3RsXCVRiZ98/EHIQgIuJGGYjZtkkZUKkfhN" +
                "BEdBiYuQTbCrXt++591335vqrzPBetSrd88999z7qqqrqmumo6" +
                "iyGEWVPyuXK/80tjcb67rKr1G2FB+JciyVX1T0hoP8FnW9VH4I" +
                "ev92MX0Olaut0fXOKomHTU+NUMKJgVwZjRZH2DzWdiPs3D4W1k" +
                "UjtrReR+WsUVXL7fPF23hlRGLocccSCcVJLN6WtuCx3UY8jsD4" +
                "UIzs/aqahs8X7+KVETlybenLGyex6rV4l+aXTGRhvXosxWQZrv" +
                "prCuXWfGM/m54aoYQTA7kyGi2OsHms7UbYuX0srItGbGm9jspZ" +
                "o6qWW/r894D6fcvtHlD9ut17wKHj/bkHFI6anhqhhBMDuTIaLY" +
                "6weaztRti5fSysi0Zsab2Oylmjqpbb9cWvmJ4aoYQTIzvSv3ME" +
                "Rrf8ixgheTYXcTu3j4V10YgtrddROWtU1XK7vsIR01MjlHBiIF" +
                "dGo8URNo+13Qg7t4+FddGILa3XUTlrVNVyu754q+mpEUo4MWSE" +
                "O2akelHjuVzC7dw+FtZFI7a0Xkd5W71AKqHcPl88xWumdtqMEE" +
                "OuNpZIKE5i8VTags8aU8TjCIwPxcjer6ppuL6xP0xPjVDCiYFc" +
                "GY0WR9g81nYj7Nw+FtZFI7a0XkflrFFVy+3zxdO8MtI43+YRQ4" +
                "87lkgoTmLxdNqC59k08TgC40MxsverahrKHDeZnhqhhBMDuTIa" +
                "LY6QPJuLuJ3bx8K6aMSW1uuonDWqarld39iPpqdGKOHEQK6MRo" +
                "sjbB5ruxF2bh8L66IRW1qvo3LWqKrldn1ji6anRijhxECujEaL" +
                "I2wea7sRdm4fC+uiEVtar6Ny1qiq5fb54h28MiJHri19eeMkFu" +
                "9IW/B6toN4zTv7FaxXj6UY2ftVNQ1ljltMT41QwomBXBmNFkdI" +
                "ns1F3M7tY2FdNGJL63VUzhpVtdx+X/Nb67qob0v9/mjAy8HN/d" +
                "GN7zI9NULTUXIx7QljroxGiyNsHmu7EXZuH6v2ANXFFbKl9To6" +
                "u2DPGlW13Lav9qBznn3WZJbiUhQd+knEl5bY/yU/zyh29S7oq7" +
                "bPBzVjus/yzcfzWfS+P9OW2ppu3p8VVw/8byir870/m7u3q7qa" +
                "17N4Mp5UjtvkEsd10s/TFfu75K23n+dZd+9pi6sGfp6tGuh5Nh" +
                "6Pp/cAcVzGlzhu4z7e7JxR7Oq+O9z2eTa+xPWsq4oKt3jVfCE7" +
                "rzesXbgV1nVZWK8ea1D06XMJ5Za+8rnyOdyaEaPsI5THjKBFcV" +
                "LRVrfjsBpd21eL5Mq+vkpj2HNHy81m7w+M0J5pC3/14iqcnPHg" +
                "pwdxD+jNHOylfL58HrdmZKzCTfQRymNG0DIRrJicsT3UktMyyl" +
                "aytY2mW4vkyj6dg8uw546Wmw1rtCPU8+xGtOKX/szB/6xRWGz/" +
                "WaO+Rj5r0GdT/k24k8/mgTfafdbQ59D934RFXQej22ipTeTkPd" +
                "/VefdlV09QDy+zffZ2Tt4LvXim7fD60deztL52OdRT2lrailsz" +
                "MlZxHn2E8pgRbsV5E+EqZt9k5vU4rEbXNppuLZIrezcjKWGF+i" +
                "ylPnHKQ+Wh7C7a3JoRo/U7yUcojxlBy8S5itk++16Pa93vh+yG" +
                "1WCdematdzOSElaoKct6mJNsTJ5InkwmlvpsJs80v6k17ovJmP" +
                "VOa0EwH2usG8x9M3kKvuMFr3RJKcnxza86mnFHlfjnGhkeamxH" +
                "ACs2Ps2Pgv141q+3rkTXk6eTZ3M9mxeSON/1LD3PercULw/6Hj" +
                "CYjPSetpHvVAc1noqW1TKYemCffdFBjcGY+MrA3znmzFhf38Z9" +
                "c6Y0g1szYpR9hPLYtOoCWqThKmZ79BoyZZSta3PSUfGaW4vkyt" +
                "7NSLPCCvVZSv0WZ19pX2Y1t2bEKPsI5bFpjX0GFmm4itlRn0Wm" +
                "jLJ1bY6vFsmVvZuRlLBCfZZSv8XZW9qbWc2tGTHKPkJ5bFpjn4" +
                "FFGq5its92I1NG2bo2x1eL5MrezUhKWKE+S6nPEeOt98W1KWOn" +
                "iEHHh4uXeEw2R1FPGuznOLjWXTLelI/xuLAWZWS2UZUsVtk/7O" +
                "rJeu1ZsJ8RfcH45LVkt3gOaf2vVf3FXl6Nk5090NjT9retTb2/" +
                "r6T7LNmbbNf2WWGmA73X/7t9lii/sMk7h2SmN/uso/e+o6F95v" +
                "f2a5/lzdhOZeUN5Q28NatpjfN6s+1zOeghy0SwMkfyWMZhNbq2" +
                "0XRrkVzZ1zdpDHvuaNk1yZnZEfAmaU90Gy2FN3vLW+rdds74Zf" +
                "2b10Nr873brr/UTV2zF1r7fqIXR3rO8zuiucMDOc8m+qleHOJV" +
                "84XsvN6wdnEorOuysF491qDo0+cSyi19hZOFk7xNe2ijri/d4h" +
                "ijjE8qzx0lL6GmzR3mOIzBhtqoKr2yTuhHpRrNgCt0lVHRzRD8" +
                "/eZ25RP13cq6ntXuyHc9m/umN/eA+vTK32dvvZxvnx04/v99s/" +
                "WJ25JvnxW2dHXfbH3HKb+68p/P+jMH/3lWO7bczrP2//eg9n6+" +
                "86zdpXqs+lFz9E71Q/4bShl+fVFtPmXN3t0YfxJUO1I90RpDxZ" +
                "Vvq+95Y7K7UfXzJT8D94S8+4cbGh+A6sdiDu92c7yqp6qf+ny1" +
                "E21qLSznz2Z95yCy1JrPLPFIPOL+PqC7JR4Z9D7TM8726DgXzv" +
                "Kq+UJ2Xm9Yu3A2rOuysF491qDo0+cSyu36As9n+1f+s0ZtY85n" +
                "2nb+1/1fYbO4vQ==");
            
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
            final int rows = 63;
            final int cols = 77;
            final int compressedBytes = 1554;
            final int uncompressedBytes = 19405;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrlW02IHEUU7kDCXnIMnoIHZcEI6t3ksP0z2Zxm9xBEWdCrZw" +
                "Vl8eLs7K6ztJ4WL/EiCgpLQC8eBMU9GEV00RXJYYVk40rAn8Mm" +
                "AWVzcmtev3mvXtX0dKeqKzNjNVv16tX7+d7Lq56unknv40i0zk" +
                "cwpufT81G0+n3ksSmLLi1/1Y/Hle3Ia+s8XU++69m/35a/FsLL" +
                "lOXs9UZy9OewnMUvOFj9twmsa4+Wrj5u8uwxdH4fUHcL3cf81V" +
                "nyi8+YXa3V17druEeVXIMeL+QqCnspK7X5jDS4LrfN9dCLXDcx" +
                "ckQcoYlMerdxedTcqs23XFN7s/N3Z69z73g8UnXWOShklyvV5W" +
                "/V9mbndlWLJb5ulK4eWuJdrrg3H3HB1ftsfO/oKxt1NVpX/cqN" +
                "qrP40HedOX9e1a4zewy+6yxZNykv9//1h2HDbwzD2upe8QTdTt" +
                "tRtLEvnqvbI56728PlwKJL635e+xzQLj8HuCISZ6dW2rJgaI3A" +
                "2BouZ7fYbKuK1631viisLaaL5nlT8UoxLA6XA4tOT/Xd2jlbHF" +
                "Fniz5yFu8/PG3/rSoeN9wXzw4q7ivvp7+3m8zPm2ctu+aDIM9A" +
                "rwxyfz+a+BYmhrk7nEpPeL0jnwitb9dww5Fdzi7zESiYxXf4Gn" +
                "DzHtEop89AgyyuXtVX8Frdklq6Jd022NSx2HDIXsVgSuix85np" +
                "jWPUNdjefHlQ139Nwd4MEgN7f/bPFOSskRiGnzcr6tc4b+bvhT" +
                "5vvvV8tfPm+OYs/Bm99/Ok5yz/cDrqzPgc3uNU6vf016g+IR+l" +
                "4Yqj5P3Z7OTvTXsMvutMfIeyI95H7Yx4X7UzXC7ZGaU9qtXXt2" +
                "sQtwzveNzPkgOnjB08wPcBB9XqrHfL0968NAV781K1nNV9t53c" +
                "hx4v5CoKeykrtfmMNLgut8310ItcNzFyRByhiUx6t3F51Nyqzb" +
                "dcK6mzecvbnE/q1tnau/Y6W9sMUmfzYZ/P4gtTsDcvhM1Z/ukU" +
                "nAPmm7ifTffZKX6xSs7WnrLLDXl27qZdPgIFs/gJ1eOMRpJRV3" +
                "ebz9AGyXOKa+pW5KqUAZuI027B1qsYTAk9dj4zvXGEOA7/LVXr" +
                "ikNFDOos/9Lf2av+b6nsMViez/540L0JOfOxNy1y6oy+PW5ndP" +
                "fzZuM5+3r6ctb0Z0D6Ruic2T1a9ua9ca2z5DB0zpLDsHWWf/v/" +
                "eT7z+i7ous93jq7W6uvbNVxxBH23/d24fQb4PgfEdyd/b9pjGK" +
                "/vUPIfxux+9k4z78/K7mej25j/P5RrTVgt+dzc9703158NXWf5" +
                "c/7rLN1Kt/gIFHFpDblEw3V8RmcztEHynOKaOi1XpcwwLFJW9v" +
                "k3Ngk9dj4zven5UGN2JjujZjgCpa50IV3ANeIDD2jUQg1FpQsF" +
                "ggVpUfFMbZLivrlFXYZzuby+Rj3gkHjJFuI1LUs8TOZ0dro/K0" +
                "agiMt74MIf0nyGkmSHW+zvlB20TnpSh1+mjO6RS/I16jfa0hqh" +
                "NH3rUep4mMxSttSfFSNQxE1P4RpyiSYOn4GeabFffafsegPcS/" +
                "rF0ag/0OdYpKzsTY8YFUdIM+7NjIw0VrRf6+Bv3aNo4+Tx2vvR" +
                "RLeVKyG8dF7SPiNO+rTtaq2+vl3DFUfQd0E3g78LulntWSPfHd" +
                "vz5vXQOZv83+zlv05DzuLNeJNG+IMLuHxNysDY3SaNeNO0TNLH" +
                "OfsJZaUnLlkmI7HoaJFHff6jtEYoTd82i6aH0rPTLd91Fv6MXv" +
                "nsFLvgyp40KV9n9PRG6GeNZjyGvJ9lz4SuM7tHt/tZNpvN2jjA" +
                "zWaTXaJxTjNcIQrXk13TcrILqyhPo4kH+SCDEuQdOSSn65JPzk" +
                "N/yS6t66PU13mF/rnsXH9WjEDBLF7ma8glmjh8BhqmRd26rjdA" +
                "dk6/OBo7FikrexWDKUHeWFaMKM3IYJw7mjtSMxhVT1d80VxTI6" +
                "e5FqxhQ8skTbTU4zr8MmUkFr4uMalexaBbwwg4QlsGJB6SyWay" +
                "mX4GixEomOW3+RpyiSYOn4GGaVG3rusN/q1n9IujsWORsrLvzd" +
                "skyBurMyNKM7Ji/A+i08ph");
            
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
            final int rows = 63;
            final int cols = 77;
            final int compressedBytes = 2004;
            final int uncompressedBytes = 19405;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtW01oHVUUfpu6bLuSVgWNCxeKXbW7djFvZprsSrKxiFjEYh" +
                "cFwYIgSpGXNKnPPFfSTX+wO0HcZNlCICCoUMFFEcGFFJUSioja" +
                "an9Mwblz5sz5nZd56bzXJLy5zL3nnvOd73x3Mn/v8dJq6W32CF" +
                "rdva0a29xKq/a2+HdrxNviX81zxtfj63wEC2aLf4QeZzQShkdw" +
                "BhyE5xbH+nmycTVcp8/g9R+/5iHk2vnMVuMKZQbL/b56rmPr5Z" +
                "q/+u1hnlNe9bNHR3E2d58trecavzb/Gfm1eWckx2yitJ5v/Jjd" +
                "Hfkx+3cI5/NqvMpHsPJ2IGur3RsYA28YGWbVzoCD8MI64OdxNT" +
                "43cFotGmv6Ax5idkWu2WPWegiT7E52hxmOYJGXYuglmzx8hnma" +
                "Mf+r3/PzUDdnkpgqLRqr+7NHPQRVo2NmV2lXBmPnmj7zOt8Mfr" +
                "Z2vquMfC2ulLVmro7Ot3WRI3oGvFCcq5PxZDa7oa7lyXWu9clq" +
                "HDA+yjZ3ZeB7j1txdqXueupt8++jFTXyZnDmywr/F6M4A6LbIz" +
                "nPXirr3Wtt+W00a+i+/EgaP9pkx2wIeqIj0REaYYcGXh7TGBkB" +
                "q93TzIQmW+fxHN4sRmuRaqUm0KPZcFVcoWbmjLZC9gy6le2/d3" +
                "7qZFd+53627+v8WrCkzt3uR/MM+8V9spVvr/PnCs9NxfTpBp6X" +
                "P/eN/umcE6mL/K20im8N5n8YSEflMUtqvUOvd8xKz80G3jEGPm" +
                "b+GuwxW/isvopkT7KHj2CFFh+MDyZ7wrsGxMAbRsLwCM6AwzLm" +
                "T/aDfh5X43MDp9Wisbq3FUMP7xq8srdKuzKZwa6Zzxu8W15s9N" +
                "578XErgC1dTBdphB0aeHlMY2QEY5w5Oo8WRhCrK+mohwlsWotU" +
                "KzWFPjqv2Uilre0xOhV6af6kgxF2aODlMY2BcW6FMtIe09DDGf" +
                "H1nkKsrsSR/TBai1SLPup7ezUbqbS1PUZbITmUHMqv1GIEi7wU" +
                "Qy/Z0OZW+Aw5LGN+zJ7hSJ0leSWmSovG6r73tIegaux+Zlap+U" +
                "vMWpJ/bsYRrNDi6Xg6WcufAXkMvWRDk7N4GjgCTjJmT4Bp8Nq8" +
                "UveabOQJVvi8yb2eDt3biqEvngGlXjoCvJrmLzHnkvz9CUewQo" +
                "un4ikeQy/Z0OQsnoKMgJOM2TGbAq/NK3Wfk408wdJaPB26txWR" +
                "CWag12PWehhmKVnKZ8UIFsyiOzyGXrLJw2eQYRklu8wrdS/Jxt" +
                "X4WjRW92ENFkHV2LVpVmlXBmM7akdhBiPs0MDLYxoD49wKZQAe" +
                "NuQgdHam3UCsrsSR/TBai1SLPuptRVJpa3uMtoLzjryvfLdJts" +
                "H3GkNYQ/tE+wSNsEPLnqxvy5jGyAjGNDOhydZ5PIc3i9FaeFxr" +
                "Cn1Yg2Qjlba2x2gr9P286Xz31H1li33evFfv8+aAOqqP2QPnmL" +
                "26xY7Zg+aPWXt/ez+NsEOLZ+IZGQMv2TICVjxTfM8+g8yEBkaO" +
                "Rour4c1iuJfjZYx6WxFWiXjUq9dCjLZCv/Os5t9enGftT4Z5ni" +
                "2cHvQ8Qz1NnmfprnQXH8GCWXKXx9BLNnn4DDIso2SXeVyNzw2c" +
                "VovG6j6swSLk2vnMVuMai9jl9HI+y0fYoYGXxzRGRjBWasg80S" +
                "W0MIJYXUlHPUxg01qkWqkp9NElzUYqbW2P0Vbo937WwNvRhUbf" +
                "tS48bgXFc/B1c894ojzypxq4A+14vO+0TazBcB5Lj9EIO7Tsfn" +
                "ZfxjRGRjCmmQlNts7jObxZjNbC41pT6MMaJBuptLU9RlvBOfPe" +
                "KN9t/tv6n52GsQb7G5fuW6V1ojZL5W9cum8O40jU/43LaDa6nz" +
                "XCtqO17bb0eHqcRtihgZfHNEZGMKaZCU22zuM5vFmM1iLVSk1y" +
                "PdIrFWpmzmgrHJ44nP8WFEewyEsx9JJNHj7DPM0o2WUe6uZMEl" +
                "OlRWN1bysiE1for9KuDMZkOVnO75XFCBZ5KYZessnDZ5inGSW7" +
                "zCvv2MuycTW+Fo3Vva2ITFyhv0q7siJ2Nbmaz4oRLPJSDL1kk4" +
                "fPME8zSnaZV+q+KhtX42vRWN3bisjEFfqrtCuTGexJd3I4d87Z" +
                "+UfnmFvaHE+BZr/X2Gzfn1Ug7e+Cnhwfs+Ees2F+Rt++W3oFem" +
                "zoDRb2Gquz+YwyJI64bYasXY3Cniu0yqiv9vJVc1avto6Nr82N" +
                "bPGL0GNDb7DOXAs9+girs/mMMiSOuG2GrF2F6r6Lukghzbze94" +
                "bfa/BVc1avto6Nz7ONPAPaX0GPDb3oRwTH6mw+owyJI24eA7Ss" +
                "7WsknVwh57F9tZevmrN6tatjxTvte+On4qDvZ92T42dAjWfABP" +
                "TY0Bus7BkwgQiO1dl8RhkSR9w2Q9auQnU/QF2kkGZe73vzZ8AE" +
                "V9W/to6N8hnQPb1dngHxTuixoTdY3Q9Djz7C6mw+owyJI26bIW" +
                "tXocL/ImqFNPN635ufZzu5qv61q2PD/ezUxPcam2Nr32rf4iNY" +
                "5KUYeskmD59hnmaU7DKPq/G5q7RorO5tRWTiCv1V2pXJjPFn9I" +
                "1s0UPavVi/ed1of+7oYX9ei+J6/Vzw8pi/ln61dSxdTpf5CBZ5" +
                "KYZessnDZ5inGSW7zONqfO4qLRqre1sRmbhCf5V2ZTJjfG3W3x" +
                "ZODfZ+tvDO+HNA6393eeor");
            
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
            final int rows = 63;
            final int cols = 77;
            final int compressedBytes = 1867;
            final int uncompressedBytes = 19405;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtW8+LHFUQHtZVR0HUg56MEEG85ZzcTO9McjDn+E9EDYtCUE" +
                "Kmw2SWBT3lkltuIRcPq8nFqxcP0QQEBUUFFSUSf6ASiYvizKup" +
                "rvq+et3ZmcmEuOl+9Hv16qv66ntveqZ7Z9jNd8qfOp3yRvlF+c" +
                "d4vDU+95XfdWqPzbfZU36biyv/Cp4fOgsf5deN6G875vm+sn7v" +
                "3IGjvNyEjk507vujv9XfslFOaeL1GMcgohgzW7TZnOdzfIsxrA" +
                "XVoiZcD3pRITN7xlihd613bTLTUSzzGqZes83jZ5rHjMiOearb" +
                "M2FMnRaO5T5WVCavML/KuDIdZ/s8y7yX7+Ln2ejkvfF51u7ZrE" +
                "dxsjhpo5zSxOsxjkFEMWa2aLM5z+f4FmNYC6pFTbge9KJCZvaM" +
                "sULmtdxo74wzX//tnt3uWeNi/6KNckoTr8c4BhHFmNmizeY8n+" +
                "NbjGEtqBY14XrQiwqZ2TNmKqz0V9JsOoplXsPUa7Z5/EzzmBHZ" +
                "Ma/SvYLNq8lr4VjuY0Vl8grzq4wr0/H/dN9c3t9OZ56e4b252l" +
                "/1o1jmNUy9ZpvHzzSPGZEd87yaPHedFo7lPlZUJq8wv8q4Mhl7" +
                "e3t7cRfFI17uLVZmipjFPLlD422MWeqXmLxGjMNcH+t7rpfXV+" +
                "eT/OJIcSQ9eaRRTmni9RjHIKJY9bxzRGfGZ7FcidG6GNaCalET" +
                "rge9qJCZPWOs0DvXO5d2cDqKZV7D1Gu2efxM85gR2TGvejXPYf" +
                "Nq8lo4lvtYUZm8wvwq48pkLI4Xx9MOplFOaeL1GMcgolj1uh3X" +
                "mfFZLFditC6GtaBa1ITrQS8qZGbPGCv0zvbOph2cjmKZ1zD1mm" +
                "0eP9M8ZkR2zKte67PYvJq8Fo7lPlZUJq8wv8q4Msxw9+EL1c5v" +
                "zfH361bnPjhm/G775UWez4pn7vbzWb7i/fRd0PKeaTffbfds1j" +
                "0bnV9EV/FStObJvjeOZejpX+5ftlFOaeL1GMcgohgzW7TZnOdz" +
                "fIsxrAXVoiZcD3pRITN7xlih2F/sT69HGuWUJl6PcQwiilWv8H" +
                "6dGZ/FciVG62JYC6pFTbge9KJCZvaMsUKv2+umJ4/pKJZ5DVOv" +
                "2ebxM81jRmTHvOoZqYvNq8lr4VjuY0Vl8grzq4wr0/HQHny3Ht" +
                "oz8YiXe4tFRP3Ixcy+gs+P1S1fYvMaY53I11tHvbiKnR+40sEn" +
                "dE/ZNxjfnQbpbjhI95dBuuMNfh3fkd/XqMG/6Q6dnmDLh8vHpn" +
                "fs94DpuXz9wY3Bz4NfkvVnuKM9UT5Z2c+Oz+fLFwZXBh8Tw1eD" +
                "b9zsemXdrH2uvJTwTwefjfsv57hjP576PeXke41jxbH0Tk2jnN" +
                "LE6zGOQUSx6vPhmM6Mz2K5EqN1MawF1aImXA96USEze8ZMhcPF" +
                "4TRLo5zSxOsxjkFEsUrDYZ0Zn8VyJUbrYlgLqkVNuB70okJm9o" +
                "yxwvDE8PXhW8NXLXf42kLPLi/WIcP1JnRnx/CNRvSVWfTMF5di" +
                "D/QP2CinNPF6LMZ4xLIsxmqgzXleTZ67TgvHch8rKpNXyMx+V3" +
                "g+vtpOFafSVZfGSW9tMmdMfGb7LI2fXuundKao2Zznc1gBxrAW" +
                "j7MmrzV6vcLcDrAeF3O0OJpmaZRTmng9xjGIKFZpOKoz47NYrs" +
                "RoXQxrQbWoCdeDXlTIzJ4xU2G9SE8wMsopTbwe4xhEFKs0rOvM" +
                "+CyWKzFaF8NaUC1qwvWgFxUys2eMFVzOo3aah638PLLkY3O2r1" +
                "LP69lRV+SJ1es0+IimNdVhxQN25rCm+U7RZu7igWbeGOX15nPF" +
                "67H8WppqM7b5Qfv92QLfNK3amcOa5jtFm7mL1WbeGOX15nPF67" +
                "H8WppqR2y26+zM5+33tBuPjG6NbsqebTyUsrdne2/W8HbZM/pn" +
                "cdbR3401V+fQ+WDqV5b5G8rG9d15nW382F5ny7zOdtOebV5p9+" +
                "xe3bOMrrXl/Gq2eW3Zv8uNirvz+9/B8wfP2zjprU3mjInPbJ+l" +
                "8chs0WZzns9hBRjDWjzOmrzW6PUKczvAeiymd7V3Nf3WMB3FMq" +
                "9h6jXbPH6mecyI7JhX/eJxFZtXk9fCsdzHisrkFeZXGVemY/u/" +
                "O4sfp9/stMesd5Kn2j1o92z5R7lvObynz+yWHVrrrnX9KJZ5DV" +
                "Ov2ebxM81jRmTHPK8mz12nhWO5jxWVySvMrzKuTMZiu9hO3xFN" +
                "R7HMa5h6zTaPn2keMyI75lXfVG1j82ryWjiW+1hRmbzC/Crjyj" +
                "Bj+e/N3XP0u/2uH8Uyr2HqNds8fqZ5zIjsmOfV5LnrtHAs97Gi" +
                "MnmF+VXGlWGGu0Y3onUnjjvBNjtHsaT/4C0+kl6beieW9hzL2X" +
                "5mGT7Xc/s8rcJ41OgVeYVRGVfPef2qPWuudj02/Ty70H5i3eZZ" +
                "49LaJT+KZV6d+9FiPGJZmOctH5vPw+bVeJ15hlwfKyqTX3OOmf" +
                "XEjPa+udCV92H9nLHb5c5bc76YeavPfPwHuTp03A==");
            
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
            final int rows = 63;
            final int cols = 77;
            final int compressedBytes = 1336;
            final int uncompressedBytes = 19405;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtW02LHFUU7X8gbkRXCq7NQt24k65qGLIRssuQ7SzEnzAgtE" +
                "MI0yHD/JLsXPgPXGarCxcqBsWA6EJcBOzu26fPuR+V6eqOLQyv" +
                "inrvvnPPPfe8SlV6ekKm96b3Jstjup7tstNQzUWOzyCHAxpkM4" +
                "51WqNn5kQv3q335PfjUe8wKqti7jCZzH9bXr/Pv5//tZz/WV53" +
                "5j9NRhzzH0v074T8Mjn4mP/wyuwfO+v8vI3+HO9i+mD6gLNddh" +
                "qqucjxGeSiMtmMY53W6Jk50Yt36z35/XjUO4zKqpg7tOdsvIv+" +
                "or/Q2SKizAFlTERXqIuKXt3XqZtae8hL5MYxd4SSOqx3mXdm8/" +
                "R8er5+6tazXXYaqrnI8Rnkts/6OVbUIzd2itkhTvTi3XpPfj8e" +
                "9Q6jsirmDu3d3OPdfNw/1tkioswBZUxEV6iLil7d16mbWnvIS+" +
                "TGMXeEkjqsd5l3ZnN33V2vVpgtIoq1zuRohlW+TiPl1nX+VDfq" +
                "s1aoxtwRSrrnSjn60Yr2bu7ho92z5fHwrXbPxt2zhx+McdE97Z" +
                "7qbBFR5oAyJqIr1KlKVvd16qbWHvISuXHMHaGke653mXeGuT1n" +
                "7e+z23bPui+Pfc/qju05O/5zlvTuTNrRnrP2brZ3s72b7TlrR7" +
                "tnr+no7/f3dbaIKHNAGRPRFeqiolf3deqm1h7yErlxzB2hpA7r" +
                "XeadbXKn/el6tZktIsocUMZEdIW6qOjVfd3W96k/1U3tJXLjmD" +
                "tCSR3Wu8w7s7l72b0M3zbWiKFxJNdnUIO8X6XvM8Kvu7ObcXzv" +
                "3CnXKtc7jfnK3xCGDu1z8/CfNRbvtHs29ui+2y9X5W/iv97jEO" +
                "ejPjev+2udLSLKHFDGRHSFuqjo1X2duqm1h7xEbhxzRyipw3qX" +
                "eWe+gsdXnyFavNd+FtvxCf92eB1zN9Xu23M/zr7dDz/kOXu/PU" +
                "Htu1O7Z//b9827/V1EitgqjuDEDFSYV+Wqp6lpL18F3DhRybtW" +
                "bnSRncZ85W/oLm3qT/qTDb6dV5Gt4ghOzECFeVVODk7QQ3v5Ku" +
                "DGiUretXKji+w05it/leNX7WnR3s3Rx+KjW7WbD3fkfXxIl4u3" +
                "23Mz+k/mk3YPxv5eo/0uqN2zY/wuaP51e/d2O2bPZs8Q2TXE0Y" +
                "vMuoJ1wzxFoHmTB/pUrq9WvNajVq5jpboP9S9mL1Yjr904YNYV" +
                "rBvmKQLNmzyAC2X1AkTxWo9asU4r1f2G+WT2hLNddhqqucjxGe" +
                "SiMtmMY53W6Jk50Yt36z35/XjUO4zKqpg7jP0MePTr7fwMePT8" +
                "v/vcbPds+a1z3s91togoc0AZE9EV6qKiV/d16qbWHvISuXHMHa" +
                "GkDutd5p1tcpf95Xq1mS0iyhxQxkR0hbqo6NV93db3pT/VTe0l" +
                "cuOYO0JJHda7zDvb5K76q/VqM1tElDmgjInoCnVR0av7uq3vK3" +
                "+qm9pL5MYxd4SSOqx3mXdm8+xsdrb+RFjPdtlpqOYix2eQ234O" +
                "nWFFPXJjp5gd4kQv3q335PfjUe8wKqti7tC+O9k87v87tX8PaN" +
                "/Rj/OcffrcRpxAgYOh3FitK1Z4HrVzhe89xFJfiLiqxhqNu1bV" +
                "qnfMHfM5W3x+W56z7k0bcQIFDoZyY7WuWOF51M4VvvcQS30h4q" +
                "oaazTuWlWr3jnXvWsjTqCraPHFagRGbqzWFSs8j9q5wvceYl18" +
                "A190yFU11mjctapWvYdz7XNzxLv5ho04gQIHQ7mxWles8Dxq5w" +
                "rfe4ilvhBxVY01GnetqlXvkPsXUSZgzQ==");
            
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
            final int rows = 63;
            final int cols = 77;
            final int compressedBytes = 1279;
            final int uncompressedBytes = 19405;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtW8FuHEUQHVkQwQ/wA87Z/5DseCUrB675B2QjPoDDRLaXmA" +
                "v8QG7h4jO/AARQTiAkQCBIxAFxQCCBEuQI79bW1qtX1Wtmvd4s" +
                "657Rdle/qnr1uj09M7tRmqb7tWm637pvuz/P+2fnn53uSVM8Tt" +
                "5mpPs5i+v+DsgvzSWP20+6H+b5u9+znDTy6cz6Q/r3fuqjpOea" +
                "vfPy1uyco/eaFSLDmh29cSldO82VHPeOmg05ds92z6yXj5yCoi" +
                "/GoMeyLMZqeJvzUE3OXdLCsdzGisqECpkZV4XH2d68/9a8vZlc" +
                "lyvcm8fvXtXe7LkX65pdXteoqcf8+9nD3YfWy0dOQdHHMd6jPm" +
                "a2aLM5D3PwjDGsxav1mvx8POoVMjMyxgrJdftxvZIueE/cub1j" +
                "vXzkFBR9HOM96mNmizab8zAHzxjDWrxar8nPx6NeITMjY6xQr7" +
                "NlPDfnfw94uc/N6/Y9YIOemzd2b2AvlqHmU9RsQ3Ckeczo2X0e" +
                "qsm5S1o4lttYUZlQYT7LODPpB88Hz/0qCiIotxbrPZqjfj/iA+" +
                "Pz6lZNYnztWCnmYqxXyv5MXwnTCv3uZ0ffbOj97OurewYcfriZ" +
                "a3b4QX1urvb75jo/Nw/fn+u9v57vZ/V+Vq+zRY7R68fPjv+S62" +
                "w0eQc5/qff/azA+1r4jelsCb9TzX1LGL2ygM5XJ+1Wn5xbD249" +
                "sH7c2jkes08wszFL4z2zRZvNeZjDCnwMa0E/a0KtEUWF2QqwHo" +
                "tpH7ePxyPtxTLUfIqabQiONI8ZPbvPU93I5GNKWjiW21hRmVBh" +
                "Pss4M59Rv2/W97MVv599VK+jep0t+zprt9vtDBGUW4uVkXrMms" +
                "eMHs33rMhpNUoafZzPxVhsuV6ur4RJ/vDm8OZ4pL1YhppPUbMN" +
                "wZHmMaNn93mqDJl8TEkLx3IbKyoTKsxnGWcmfbvf7k9WcNqLZa" +
                "j5FDXbEBxpHjN6dp83+2vu+xPV5Fo4lttYUZlQYT7LOLOp76A9" +
                "mIymvViGmk9Rsw3BkeYxo2f3eTPdB/5ENbkWjuU2VlQmVJjPMs" +
                "5M+uH2cLJTtRfLUPMparYhONI8ZvTsPm+2P7b9iWpyLRzLbayo" +
                "TKgwn2Wcmc+AX3G/i9b/41id3vquUdesrtk6rtlgNBhhL5ahOs" +
                "beYtBjWT4PLYzN8/yJalBnzpC1saIy4ZwzZtYDMaeD08lo2otl" +
                "qPkUNdsQHGkeskR2nzfTfepPVJNr4VhuY0Vlwjnns4wz036Ve3" +
                "PQrXpv5hWv07871d816nPz+qzZcGu4hb1YhppPUbMNwZHmMaNn" +
                "93moJucuaeFYbmNFZUKF+SzjzLSv11l9BtRnwFr+tn23vYu9WI" +
                "aaT1GzDcGR5jGjZ/d5qCbnLmnhWG5jRWVChfks48x8hh333lTr" +
                "5Ku6C9Pvmy8GLzJEUG4t1ns0R/1+lNXUiLy6VZMYXztWirkY65" +
                "WyP9NXwkpzGjwqj9l3Ue5//ss9WkbMotWXcOV9Xh6z76LcRWsu" +
                "FrNo9fquUddsHY/2TntHLURkxK3GsEdZzI/MWU1hw1o+S3GJYS" +
                "avGmNZRVTK/kxfaZWm+Xvt3hSf9WNLRtxqDHuUxfzIHBTsaQ2s" +
                "5bMUlxhm8qoxllVEpezP9GWKre++CHvz0wX285dFzydXcv/4bL" +
                "1268n39Y5V12wFa/ZjXYPea/a0rsEmvZ/V/7uzTr+f9fq/O/8C" +
                "7a1QqA==");
            
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
            final int rows = 63;
            final int cols = 77;
            final int compressedBytes = 1334;
            final int uncompressedBytes = 19405;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtWruOHEUUXdmChMRaGQJEsCIhgshfsN2zP+HECGm/g4ZlzY" +
                "4za//B4cZkiAAhGScOCRACRIAsCwESIBJ2pnz2nPuonukx+7Bd" +
                "VeqqW/fec+6p3n7MjL215dvwwdaVbQf3RqNHl6Vr/mSrtbXa7P" +
                "HsMaxy1HL0YGaOIK6epx5wrtJAnZpr0erP+cgVcUSq+lq7yvfm" +
                "VW3zP9o5mHzO/mznYM3n2dPZ08XIY70cZOYI4up56gHnKg3IBb" +
                "NqgUf9OR+5PE6Rqr60/rg/1rlY9DIGL216dAWcZ7TsFqdqcu6a" +
                "Fp/rx1gRTKow32XcGebh19PjyfDdcPocG/5ZvAOGn0bu3L/CO+" +
                "PH9E0S8355/rth+H40+tvaPD+fWb9vcE/emN3QuVj0MgYvbXp0" +
                "BZxntOwWp2py7poWn+vHWBFMqjDfZdxZmed/+7N4+Pr/+Zw8fO" +
                "0lfPbfnt3mXI7Si1djPsdGEPPMzKbtcYrRHnO8FqvWarL7sV6r" +
                "0DMrY6wwPPRn8Wj/7Nn179rPh29rkaOPzuMvPXxzqdfZndkdzu" +
                "UovXg15nNsBDHPzGzaHqcY7THHa7FqrSa7H+u1Cj2zMsYKY9fZ" +
                "hL/7q3Wd7c/2OZej9OLVmM+xEcQ8M7Npe5xitMccr8WqtZrsfq" +
                "zXKvTMyhgrJFfGh+1b0crrfNJn2gT/yn2mbb8Fbda6t8uIDu/C" +
                "OvphMcLHXI/WFRE2j9wRYWvXsj79ArqokKtszL2ffGl3raxZbR" +
                "9r92Z7nl3M86x7t4zo8MKPDM31aF0RYfPIHRG2di1LdcHiKhtz" +
                "r9+1sma1Y6zbLiM6vAvr9Hm2jQzN9WhdEWHzyB0RtnYta/k8cw" +
                "q5ysbcu3yebauq8dr1WHtvTrg3d8qIDi/8yNBcj9YVETaP3BFh" +
                "a9eyVBcsrrIx9/pdK2tWO8a6m2VEh3dhHTxcjPAx16N1RYTNI3" +
                "dE2Nq1rHtvQBcVcpWNuXd5b95UVeO1fWzae/Pz917O9+Znb7Xv" +
                "ARfbDu+2c7Dit6AHswecy1F68WrM59gIYp6Z2bQ9TjHaY47XYt" +
                "VaTXY/1msVemZljBXavTm99ff7+zoXi17G4KVNj66A84yW3eJU" +
                "Tc5d0+Jz/RgrgkkV5ruMO8Pcvm+27+iX8Zvj3jsv6nPm4wtXvn" +
                "uNRxYbW68bHefevTbOG7NUb44tXo3lexmr7WP9o/6RzsWilzF4" +
                "adOjK+A8o2W3OFWTc9e0+Fw/xopgUoX5LuPOLILt7pvt00T77t" +
                "TOWTtnL2rb/bqM6PAuLIw+16N1RYRilVtxqOLjUaMqUoVRma+e" +
                "eXXXyprV9rH2mXby9815P9e5WPQyBi9tenQFnGe07BananLumh" +
                "af68dYEUyqMN9l3FmZu+Nu+T+RMReLXqx1Zo5GiLI4tTQ3x9mu" +
                "alRnzpCNsSKYdM8Zs9cjOSfdyXL1bC4WvYzBS5seXQGnLJHd4s" +
                "50n9iuanItPtePsSKYdM/5LuPOMLfnWftd4/zP2d71ves6F4te" +
                "xuClTY+ugPOMlt3iVE3OXdPic/0YK4JJFea7jDvD3K6zdm9uds" +
                "6m/ftmO2enZ+z99n3z3L+Tt+tsYusOugOdi0Uv1jozRyNEWZxa" +
                "mpvjbFc1qjNnyMZYEUy654zZ61FEu87avdnem1fxvdnf6m9lnu" +
                "L1I3NtBCzKFZk1Uthq1Vmt5NSyrKpMRVTq45m+mg8V2nX2/K37" +
                "qr72sVXYTWtulrNp9cm/0+70O5mneP3I3LJChNYYs0aAt6zKyR" +
                "o1jTbPYjVXR18v11fzPavwHxkkZzE=");
            
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
            final int rows = 63;
            final int cols = 77;
            final int compressedBytes = 1329;
            final int uncompressedBytes = 19405;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtXMFuHEUQXSNI4IzEhVusnBDxmSMzs39hDhES4sAPIE4m8a" +
                "4dn/IPOcAnIC4cQeELEEIERyAjDggkUGJFwbvlcr1XVT277dhy" +
                "hLpbU91d9V7V687YszuJMkyGCbdh6RlO52wHQKFVzgDRIWTGCg" +
                "OMsbpVGwAdUaxqIFUlpZOiLmZM0iyLsX/eP+eoeMTrrWE5ohyN" +
                "88o3xOfVrZpguHasFLmIZaU+nukr+Up76h+W1z62irtuW4e3Gn" +
                "Pe6i/eDrYnrdWe2QftDFa1nd9Prj92ftj5+2R8cnLd2jms4v+S" +
                "ev8Nnl8vQOtPo9E/187z+Gz210Wc4b332n1U22bzdgbV9/+tdg" +
                "bVz4CP2xlUn9kn7QxWfE/YG/ZwlJl5LaZem5sHV8rzGTk781BN" +
                "nrukxWO9jRU1EyrMdxl3JuP09vT2YiWjXNLFizGP4YjGtGkOQ9" +
                "vc85CDPWK8FlbLmng/7GWFPjNmjBXa5zMZd9+q/Lb3jljt6l3M" +
                "9n9eWPUZ1rNxZQzGWe7I4Nol1J2vVJcptFVmc+/n3/CuMWtWO8" +
                "b6t8VqV6/6FYFYz8aVMRhnuSODa5dQqEtntsps7vW7xqxZ7Rjr" +
                "N8VqV+9idnKfbSoCsZ6NK2MwznJHBtcuoZb3mVNoq8zm3uV9to" +
                "mqxmvHWP+mWO3qVb8iEOvZuDIG4yx3ZHDtEgp16cxWmc29fteY" +
                "NasdY/1NsdrVu5jdfbiw6jOsZ+PKGIyz3JHBtUuog09Vlym0VW" +
                "Zz7/I+u4mqxmv7WHtu1j43u6PuCEeZmddi6rW5eXClPJ+RszMP" +
                "1eS5S1o81ttYUTOhwnyXcWc61t1nB59d3X3WHdbfZ93hevfZ3q" +
                "P2XqO9p33Z3tO2M2vvaS+/TR9MH9gol3TxYsxjOKIxn9nQNvc8" +
                "5GCPGK+F1bIm3g97WaHPjBljhW6r21o+X5ajXNLFizGP4YjGzp" +
                "5YW7qyfIb1lXy0hPFaWC1r4v2wlxX6zJgxVmi/z873Luj0nK/Z" +
                "lcXG1utGx3N318bzRhTqzbnixVi+l7Ha5Vi3YVcWG1uvGx3P3W" +
                "2M540o1JtzxYuxfC9jtX1s/sbsyewf+dmcL09zdlz3s5m3+evh" +
                "efzsAp7pT0drvnoOna8t7SuX+flsfvT//H02/63ivfb1/jqOMj" +
                "OvxdRrc/PgSnk+I2dnHqrJc5e0eKy3saJmQoX5LuPOmNG+b7bv" +
                "6Jfduu/EalfvYqbWYz0bV8ZALuZGnlbx8agRFaHCqMxXz7y4a8" +
                "ya1fax9pn2xdv7j66OffFtXT11utt91t6ftTNrZ9bOrJ1Ze3/W" +
                "zqzmzHbfrVHRz/oZjjIzr65xNAxGjMU8nCE253FHNagzz5DZWF" +
                "Ez4Z6zzF5PZLTvmxX32f3+Po4yM6+ucTQMRozFPJwhNudxRzWo" +
                "M8+Q2VhRM+Ges8xeDzLa77P23GzPzZfyubnb7+IoM/PqGkfDYM" +
                "RYzMMZYnMed1SDOvMMmY0VNRPuOcvs9QDmuD92p7j0iNdbw3JE" +
                "ORrnVfhzAnxe3aoJhmvHSpGLWFbq45m+kk/4w43hBkfFI15vDS" +
                "srjdjM58ma4m2MLPULJtfIOOYiFq2vl+sr+U75G4P7u1DxiNdb" +
                "w3JEORrnVagP+Ly6VRMM146VIhexrNTHM30lX2lP935sn1prWz" +
                "uz+nbwRTuD6jP7sp1BbWv/NnTlJ+Lw/8Xsf3R2x329dpbvS5H9" +
                "Dy9F9bdXeWbT7em2jXJJFy/GPIYjGvOZDW1zz0MO9ojxWlgta+" +
                "L9sJcV+syYMVaY/AcJdMvK");
            
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
            final int rows = 63;
            final int cols = 77;
            final int compressedBytes = 1541;
            final int uncompressedBytes = 19405;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtWc1uFEcY3FfIAU4cI4iiiJcYhreAA6/AC7D5kTAOFxAgcU" +
                "XiZEv2Mbe8AoJTDhEKKIcoElZyiLhld74pV30/7fU6wrFQT2u6" +
                "v5+q6urR2rtej3fGO4vVNU6r3Tasqr2I8R30cEGDaMaRpxwdGR" +
                "O9eLfekz+Pr3qHUVkV8w6LxfKP1f3n8pfl36v14+q+vny32OJa" +
                "/lZW/0mV3xf/+Vr+emL36NQ674+jv87koz+z1fXD5e2cDN/YjI" +
                "Eq6kAoNrI1I8PjqJ0Zfu8WSn0hYlbNdTWeWlWrvWOvv876z+Z5" +
                "PLPVa+9LmzFQRR0IxUa2ZmR4HLUzw+/dQqkvRMyqua7GU6tqtX" +
                "fs9dfZtu8B48vxJVe7bVhVexHjO+hFZaIZR55ydGRM9OLdek/+" +
                "PL7qHUZlVcw79NfZWT5r3LwS83XFqnEm1ndQ91pRWXdQft6dfM" +
                "PWHuM+9660dvRO26hNT+lmY4ezKF6M697/5nx5/eI+le93T+zu" +
                "nNsz6r/Ptv1sdn+4r6tFrCLXlRjtkOV5Gim25vmhbtRnrVDNeU" +
                "co6Zkr5ehHMI+Hx1M2rxaxilxXYrRDludppNia54e6UZ+1QjXn" +
                "HaGkZ66Uox/BPBweTtm8WsQqcl2J0Q5ZnqeRYmueH+pGfdYK1Z" +
                "x3hJKeuVKOfgSzP+xP2bxaxCp7qDJmRTPwVCWre96x730/1E3t" +
                "JWLjnHeEkp65PmU+2dx7NDyasnm1iFXkuhKjHbI8TyPF1jw/1I" +
                "36rBWqOe8IJT1zpRz9CObB8GDK5tUiVpHrSox2yPI8jRRb8/xQ" +
                "N+qzVqjmvCOU9MyVcvQjmL1hb8rm1SJW2UOVMSuagacqWd3zjn" +
                "3v+aFuai8RG+e8I5T0zPUp88k8g9fuu0W/trx23/dn0P92+lTX" +
                "+Gp8hcjuFkZvImsGeW2cVqC5yQN9KtaztV7rUSvzyFT3gf9mfI" +
                "PI7hZGbyJrBnltnFaguckDfSrWs7Ve61Er88hU94H/Yfywnnmf" +
                "DgNkzSCvjdMKNDd5ABbK6gUVrdd61Io8Zar7GXlrvMXVbhtW1V" +
                "7E+A56UZloxpGnHB0ZE714t96TP4+veodRWRXzDtOnjks2Y6C6" +
                "jnbermfUiJXPNZfc9ySXeEcctTPD791CffcTfNEhs2quq9/+7E" +
                "+tqtXesde/P+vfOX76Zza+GF9wtduGVbUXMb6DXlQmmnHkKUdH" +
                "xkQv3q335M/jq95hVFbFvMONuzfurjOsFrHKHqqMWdEMvKjo1T" +
                "0PvlXJY1peIjbOeUcoqcP6lPlkWPvPZv99dg7/Q3kyPNHVIlaR" +
                "60qMdsjyPI0UW/P8UDfqs1ao5rwjlPTMlXL0I5idYfrLFqtFrC" +
                "LXlRjtkOV5Gim25vmhbtRnrVDNeUco6Zkr5ehHMM+H51M2rxax" +
                "ilxXYrRDludppNia54e6UZ+1QjXnHaGkZ66Uox/BHA6HUzavFr" +
                "HKHqqMWdEMPFXJ6p537PvQD3VTe4nYOOcdoaRnrk+ZT4a1vwds" +
                "/R7wbHimq0WsIteVGO2Q5XkaKbbm+aFu1GetUM15RyjpmSvl6E" +
                "cwB8PBlM2rRayyhypjVjQDT1Wyuucd+z7wQ93UXiI2znlHKOmZ" +
                "61Pmk2HtP5tn/m779fgakd0tjN5E1gzy2jitQHOTB/pUrGdrvd" +
                "ajVuaRqe5b149f9P8jfU7/d7pY13g0Hq1n3qfDADkenazdxmkF" +
                "mps8AAtl9YKK1ms9akWeMtX9jLw93uZqtw2rai9ifAe9qEw048" +
                "hTjo6MiV68W+/Jn8dXvcOorIp5h/6+2b8L6s/s4n4+G67ajIHq" +
                "Otp5u55RIzayNSPD46idGX7vFmr6X11wyKya6+r0v7qr6urkvX" +
                "NvuGwzBqqoA6HYyNaMDI+jdmb4vVso9YWIWTXX1XhqVa32Dmd8" +
                "OjzV1SJWketKjHbI8jyNFFvz/FA36rNWqOa8I5T0zJVy9KOM/v" +
                "usvwf0Z9af2ef1XZB8r3Gt//W99TP7qj+D/l3QObzOvu7P4BR/" +
                "O12zGQNV1IFQbGRrRobHUTsz/N4tlPpCxKya62o8tapWe4fevx" +
                "lxjhs=");
            
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
            final int rows = 17;
            final int cols = 77;
            final int compressedBytes = 585;
            final int uncompressedBytes = 5237;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtk81O3TAUhPNEfQkrD5Y1CLYgeC243C5YQMWiYoHaRdUdKM" +
                "7pfMcec6FVdzeWfX48Mx5HyTQt36dpeV6+Lj/f4u+3+WV5nD7x" +
                "LA+2+6vrPE3//Cz37+6+fFjn25/sx1/5OL6z4zv77++snJUzxp" +
                "qpGzWjMNwRK/OYEet5edANfXoFt/YnhhLv7JRbP2Qcv7NPf2cn" +
                "5YSxZupGzSgMd8TKPGbEel4edEOfXsGt/YmhxDs75dYPMFflaq" +
                "22WDN1o2YUhjtiZR4zYj0vD7qhT6/g1v7EUOKdnXLrB5jzcr5W" +
                "W6yZulEzCsMdsTKPGbGelwfd0KdXcGt/Yijxzk659QPMRblYqy" +
                "3WTN2oGYXhjliZx4xYz8uDbujTK7i1PzGUeGen3PoB5rScrtUW" +
                "a6Zu1IzCcEeszGNGrOflQTf06RXc2p8YSryzU2799Ix5N+8iq7" +
                "N/KoZTSM8Qb4xjJzQPeZBPYjObfa8nrZ4nJt03/Jv5JrI6RxhO" +
                "IT1DvDGOndA85EE+ic1s9r2etHqemHS/fZHX5ZqxZupGzSgMd8" +
                "TKPGbEel4edEOfXsGt/YmhxDs75dYPMJflcq22WDN1o2YUhjti" +
                "ZR4zYj0vD7qhT6/g1v7EUOKdnXLrp2fM+3kfWZ3mv9jHnjBkDf" +
                "/NfVZvd9tc54w8yCexmc2+15NWzxOT7hv+7XwbWZ0jDKeQniHe" +
                "GMdOaB7yIJ/EZjb7Xk9aPU9Mum/4d/NdZHWOMJxCeoZ4Yxw7oX" +
                "nIg3wSm9nsez1p9Twx6X57XgF1SAGF");
            
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

    protected static int lookupValue(int row, int col)
    {
        if (row <= 62)
            return value[row][col];
        else if (row >= 63 && row <= 125)
            return value1[row-63][col];
        else if (row >= 126 && row <= 188)
            return value2[row-126][col];
        else if (row >= 189 && row <= 251)
            return value3[row-189][col];
        else if (row >= 252 && row <= 314)
            return value4[row-252][col];
        else if (row >= 315 && row <= 377)
            return value5[row-315][col];
        else if (row >= 378 && row <= 440)
            return value6[row-378][col];
        else if (row >= 441 && row <= 503)
            return value7[row-441][col];
        else if (row >= 504 && row <= 566)
            return value8[row-504][col];
        else if (row >= 567 && row <= 629)
            return value9[row-567][col];
        else if (row >= 630 && row <= 692)
            return value10[row-630][col];
        else if (row >= 693 && row <= 755)
            return value11[row-693][col];
        else if (row >= 756 && row <= 818)
            return value12[row-756][col];
        else if (row >= 819 && row <= 881)
            return value13[row-819][col];
        else if (row >= 882 && row <= 944)
            return value14[row-882][col];
        else if (row >= 945 && row <= 1007)
            return value15[row-945][col];
        else if (row >= 1008 && row <= 1070)
            return value16[row-1008][col];
        else if (row >= 1071 && row <= 1133)
            return value17[row-1071][col];
        else if (row >= 1134 && row <= 1196)
            return value18[row-1134][col];
        else if (row >= 1197 && row <= 1259)
            return value19[row-1197][col];
        else if (row >= 1260 && row <= 1322)
            return value20[row-1260][col];
        else if (row >= 1323 && row <= 1385)
            return value21[row-1323][col];
        else if (row >= 1386)
            return value22[row-1386][col];
        else
            throw new IllegalArgumentException("Unexpected location requested in value22 lookup");
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

        protected static final int[] rowmap = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 2, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9, 0, 0, 10, 0, 0, 0, 11, 0, 0, 12, 0, 0, 0, 0, 0, 0, 0, 0, 0, 13, 0, 14, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 15, 0, 0, 1, 0, 0, 16, 0, 0, 0, 17, 0, 18, 0, 19, 0, 0, 2, 20, 0, 0, 0, 0, 0, 0, 0, 21, 3, 0, 22, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 4, 0, 0, 23, 0, 0, 0, 0, 24, 5, 0, 25, 26, 0, 27, 0, 0, 28, 0, 2, 0, 29, 0, 6, 30, 3, 0, 31, 0, 0, 0, 32, 33, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0, 8, 0, 0, 0, 9, 0, 0, 34, 0, 35, 10, 0, 0, 0, 0, 0, 0, 0, 36, 0, 1, 11, 0, 0, 0, 12, 13, 0, 0, 0, 2, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 4, 0, 0, 14, 2, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 15, 16, 0, 0, 0, 0, 2, 1, 0, 37, 0, 0, 0, 0, 3, 3, 17, 0, 38, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 7, 0, 0, 0, 0, 39, 40, 0, 0, 0, 41, 18, 0, 0, 0, 0, 2, 0, 3, 0, 0, 0, 0, 0, 42, 0, 19, 0, 4, 0, 0, 5, 1, 0, 0, 0, 43, 0, 0, 0, 0, 0, 1, 0, 0, 0, 6, 0, 2, 0, 0, 0, 7, 0, 0, 44, 8, 0, 45, 0, 0, 0, 0, 46, 0, 0, 47, 48, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 8, 0, 0, 49, 9, 0, 0, 0, 0, 10, 0, 0, 0, 0, 0, 0, 0, 0, 11, 0, 0, 50, 12, 0, 0, 0, 0, 0, 20, 21, 22, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 23, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 24, 0, 0, 25, 1, 0, 0, 0, 3, 4, 0, 0, 0, 26, 27, 0, 0, 0, 0, 0, 28, 0, 0, 0, 0, 0, 29, 2, 0, 0, 0, 0, 0, 0, 0, 0, 30, 0, 0, 0, 31, 0, 0, 0, 5, 4, 0, 0, 32, 0, 33, 34, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 37, 0, 0, 0, 0, 0, 0, 13, 0, 0, 0, 0, 0, 1, 4, 0, 38, 0, 1, 39, 0, 0, 0, 6, 40, 0, 0, 0, 0, 0, 41, 0, 0, 0, 0, 0, 0, 9, 42, 43, 0, 0, 44, 0, 5, 6, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 45, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 46, 47, 2, 0, 0, 0, 0, 0, 0, 0, 48, 1, 0, 0, 3, 0, 7, 49, 50, 0, 0, 0, 0, 1, 7, 0, 0, 8, 51, 8, 0, 0, 0, 0, 52, 0, 0, 0, 0, 3, 0, 0, 0, 0, 9, 1, 0, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 53, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 51, 0, 52, 54, 55, 0, 56, 0, 57, 58, 59, 60, 0, 0, 61, 0, 0, 0, 0, 0, 0, 0, 0, 0, 62, 63, 10, 0, 0, 0, 0, 0, 0, 0, 53, 0, 0, 0, 0, 11, 0, 0, 64, 0, 0, 0, 65, 12, 13, 0, 0, 0, 66, 67, 0, 0, 0, 4, 0, 68, 0, 5, 0, 0, 54, 69, 1, 0, 0, 0, 14, 70, 0, 0, 0, 15, 0, 1, 0, 55, 0, 0, 0, 0, 0, 0, 56, 0, 0, 0, 6, 0, 3, 0, 0, 0, 0, 0, 0, 0, 14, 16, 0, 0, 0, 0, 0, 0, 17, 0, 0, 0, 0, 0, 0, 0, 0, 0, 18, 0, 0, 19, 0, 0, 0, 1, 0, 0, 0, 11, 0, 71, 72, 12, 0, 57, 73, 0, 0, 0, 0, 0, 13, 0, 0, 0, 14, 0, 74, 75, 0, 76, 77, 78, 0, 79, 1, 0, 2, 15, 16, 17, 18, 19, 20, 21, 80, 22, 58, 23, 24, 25, 26, 27, 28, 29, 30, 31, 0, 32, 0, 33, 34, 35, 0, 36, 39, 81, 40, 41, 42, 43, 82, 44, 45, 46, 47, 48, 49, 0, 0, 1, 0, 0, 0, 83, 0, 0, 0, 50, 5, 0, 0, 0, 0, 0, 84, 0, 51, 85, 86, 87, 88, 0, 89, 59, 90, 1, 91, 0, 60, 92, 93, 94, 61, 52, 2, 53, 0, 0, 0, 95, 96, 0, 0, 0, 0, 97, 0, 98, 0, 99, 100, 0, 101, 102, 9, 0, 0, 2, 0, 103, 0, 0, 104, 1, 0, 105, 3, 0, 0, 0, 0, 0, 106, 0, 0, 0, 0, 0, 0, 107, 0, 108, 0, 0, 0, 0, 0, 0, 2, 0, 109, 110, 0, 3, 4, 0, 0, 0, 111, 1, 112, 0, 0, 0, 113, 114, 0, 0, 10, 0, 1, 0, 0, 0, 4, 115, 5, 0, 1, 116, 117, 0, 0, 3, 1, 0, 2, 118, 119, 0, 6, 0, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 120, 121, 122, 0, 123, 0, 54, 3, 62, 0, 124, 7, 0, 0, 125, 126, 0, 0, 0, 0, 0, 6, 0, 1, 0, 2, 0, 0, 127, 0, 55, 128, 129, 130, 131, 132, 63, 133, 0, 134, 135, 136, 137, 138, 139, 140, 56, 141, 0, 142, 143, 144, 145, 0, 0, 5, 0, 0, 0, 0, 0, 57, 0, 0, 146, 1, 2, 0, 2, 0, 3, 0, 0, 0, 0, 0, 0, 15, 0, 0, 7, 0, 147, 0, 148, 58, 0, 59, 1, 2, 0, 0, 1, 0, 0, 0, 3, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 60, 0, 0, 61, 1, 0, 2, 149, 150, 0, 0, 151, 0, 152, 8, 0, 0, 0, 153, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 154, 155, 0, 156, 157, 0, 7, 4, 0, 4, 0, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 11, 0, 0, 12, 0, 13, 0, 0, 158, 9, 0, 159, 160, 0, 14, 0, 0, 0, 15, 161, 0, 0, 0, 62, 0, 2, 0, 0, 0, 9, 0, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 162, 163, 2, 0, 1, 0, 1, 0, 3, 164, 165, 0, 0, 0, 0, 7, 0, 0, 0, 0, 64, 0, 0, 0, 0, 0, 65, 0, 0, 166, 0, 0, 0, 10, 0, 0, 167, 168, 169, 0, 11, 0, 170, 0, 16, 12, 0, 0, 2, 0, 171, 0, 4, 2, 172, 0, 17, 0, 173, 0, 0, 0, 18, 13, 0, 0, 0, 0, 66, 0, 1, 0, 0, 2, 0, 0, 174, 2, 0, 3, 0, 0, 0, 14, 0, 175, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 0, 0, 176, 0, 0, 0, 15, 0, 0, 0, 0, 0, 0, 177, 0, 178, 19, 0, 0, 0, 0, 4, 0, 5, 6, 0, 0, 1, 0, 7, 0, 0, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7, 8, 0, 0, 0, 0, 0, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 179, 0, 180, 181, 182, 0, 2, 0, 3, 0, 0, 0, 0, 0, 0, 0, 20, 0, 0, 4, 0, 5, 0, 0, 0, 0, 0, 21, 0, 0, 0, 22, 0, 0, 183, 0, 184, 185, 0, 20, 0, 21, 0, 6, 0, 0, 0, 0, 0, 8, 186, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 17, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 18, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 19, 10, 11, 0, 12, 0, 13, 0, 0, 0, 0, 0, 14, 0, 15, 0, 0, 0, 0, 0, 187, 0, 0, 188, 0, 0, 0, 189, 22, 0, 0, 0, 0, 23, 190, 24, 20, 0, 0, 0, 0, 0, 0, 191, 0, 0, 1, 0, 0, 21, 192, 0, 3, 0, 7, 16, 0, 1, 0, 0, 0, 1, 0, 193, 25, 0, 63, 0, 0, 194, 0, 195, 0, 196, 0, 197, 22, 0, 0, 198, 0, 0, 23, 0, 0, 0, 67, 0, 26, 0, 199, 0, 0, 0, 0, 0, 0, 0, 200, 0, 24, 0, 0, 0, 0, 0, 11, 0, 0, 0, 0, 1, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 17, 201, 27, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 18, 19, 20, 21, 22, 0, 23, 202, 0, 24, 25, 25, 26, 27, 0, 28, 0, 29, 30, 31, 32, 33, 0, 203, 0, 64, 65, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 68, 0, 0, 0, 0, 0, 5, 0, 6, 0, 7, 2, 0, 0, 0, 204, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 26, 0, 0, 0, 205, 206, 1, 0, 1, 27, 0, 0, 0, 0, 0, 0, 0, 207, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 6, 0, 0, 1, 208, 209, 13, 0, 0, 0, 0, 0, 0, 0, 0, 210, 67, 0, 0, 211, 0, 0, 212, 213, 0, 0, 0, 214, 0, 0, 0, 215, 68, 0, 216, 0, 3, 0, 0, 0, 69, 0, 0, 69, 7, 0, 0, 0, 0, 28, 29, 0, 0, 3, 0, 0, 30, 0, 0, 217, 0, 218, 0, 0, 70, 219, 0, 28, 220, 0, 221, 222, 0, 31, 29, 0, 223, 224, 0, 32, 225, 0, 226, 227, 228, 0, 229, 30, 230, 33, 231, 232, 233, 34, 234, 0, 235, 236, 6, 237, 238, 31, 0, 239, 240, 0, 0, 0, 0, 0, 70, 0, 2, 241, 0, 0, 0, 242, 0, 243, 0, 35, 0, 0, 0, 244, 0, 245, 36, 0, 0, 0, 0, 0, 0, 0, 37, 0, 0, 23, 0, 0, 0, 32, 33, 34, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 35, 0, 0, 4, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 14, 0, 246, 0, 247, 0, 1, 38, 0, 0, 0, 0, 0, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 39, 0, 0, 0, 0, 40, 0, 0, 0, 0, 36, 0, 0, 0, 248, 0, 0, 0, 249, 250, 0, 0, 0, 251, 0, 0, 0, 252, 1, 0, 0, 0, 5, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 253, 37, 254, 255, 38, 256, 0, 257, 39, 258, 0, 41, 0, 259, 0, 40, 260, 41, 0, 261, 0, 262, 42, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 263, 264, 0, 0, 265, 0, 7, 0, 0, 43, 0, 0, 266, 267, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 3, 0, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 42, 268, 43, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 71, 269, 270, 271, 0, 0, 0, 0, 0, 0, 0, 272, 0, 0, 0, 0, 8, 0, 0, 0, 0, 44, 0, 0, 0, 0, 0, 0, 0, 0, 0, 273, 0, 0, 0, 0, 2, 0, 274, 11, 3, 0, 275, 45, 12, 0, 0, 13, 0, 14, 5, 0, 0, 0, 0, 0, 0, 0, 276, 0, 0, 0, 10, 0, 0, 1, 0, 0, 2, 0, 277, 44, 0, 0, 0, 278, 0, 0, 0, 0, 0, 0, 45, 0, 0, 0, 0, 0, 0, 72, 0, 0, 0, 279, 0, 0, 0, 0, 280, 0, 0, 0, 0, 281, 0, 0, 0, 46, 0, 0, 0, 47, 0, 282, 0, 0, 0, 46, 48, 0, 0, 0, 0, 283, 284, 285, 0, 49, 286, 0, 287, 50, 51, 0, 0, 8, 288, 0, 2, 289, 290, 0, 0, 0, 8, 52, 291, 0, 292, 53, 293, 0, 0, 54, 0, 3, 294, 295, 0, 296, 0, 0, 0, 0, 0, 0, 0, 55, 0, 297, 298, 0, 0, 56, 0, 0, 0, 57, 0, 0, 0, 0, 24, 0, 0, 25, 5, 299, 6, 300, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 17, 0, 0, 0, 0, 0, 4, 0, 0, 0, 2, 0, 301, 302, 3, 0, 0, 0, 0, 0, 0, 0, 0, 18, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 303, 0, 304, 0, 0, 0, 0, 58, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 305, 0, 0, 0, 306, 0, 0, 59, 307, 0, 0, 0, 0, 0, 308, 0, 0, 7, 309, 0, 0, 0, 310, 311, 0, 47, 312, 0, 0, 0, 60, 71, 0, 0, 0, 313, 314, 61, 0, 62, 0, 2, 19, 0, 0, 0, 0, 0, 4, 0, 9, 0, 10, 315, 0, 8, 316, 0, 0, 0, 0, 0, 63, 0, 0, 0, 0, 72, 0, 0, 0, 3, 48, 0, 0, 317, 318, 319, 64, 0, 0, 0, 320, 0, 0, 0, 321, 322, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 49, 0, 0, 50, 51, 9, 323, 0, 52, 324, 53, 73, 0, 325, 54, 65, 0, 0, 0, 0, 0, 0, 0, 66, 0, 0, 326, 327, 0, 67, 0, 0, 328, 68, 69, 0, 55, 0, 329, 70, 330, 0, 71, 56, 331, 332, 72, 73, 0, 57, 0, 333, 334, 0, 74, 58, 335, 0, 59, 0, 0, 0, 75, 0, 0, 0, 0, 0, 26, 0, 0, 0, 0, 0, 336, 60, 337, 61, 0, 0, 6, 0, 1, 0, 0, 0, 0, 0, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 338, 339, 0, 340, 0, 0, 21, 0, 0, 0, 0, 0, 0, 341, 0, 0, 0, 0, 0, 0, 0, 0, 342, 0, 3, 0, 7, 0, 0, 35, 8, 0, 1, 343, 0, 62, 344, 63, 0, 64, 345, 346, 0, 0, 65, 347, 0, 66, 0, 0, 76, 0, 0, 348, 349, 0, 0, 77, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 0, 67, 350, 0, 68, 0, 0, 0, 0, 351, 352, 73, 0, 0, 0, 78, 0, 4, 5, 0, 0, 6, 0, 0, 0, 0, 3, 0, 0, 0, 353, 0, 354, 355, 0, 0, 0, 79, 0, 0, 80, 356, 0, 0, 0, 0, 0, 69, 0, 81, 0, 357, 0, 82, 70, 358, 0, 359, 360, 361, 83, 84, 0, 362, 85, 71, 363, 0, 364, 365, 366, 86, 0, 0, 0, 0, 367, 0, 0, 0, 0, 0, 0, 0, 72, 73, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 368, 1, 0, 4, 0, 5, 0, 0, 6, 0, 369, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 87, 74, 75, 370, 76, 0, 88, 89, 77, 0, 78, 371, 0, 372, 373, 0, 0, 374, 375, 0, 0, 0, 7, 0, 0, 79, 0, 80, 376, 74, 90, 0, 0, 0, 0, 0, 0, 7, 0, 16, 0, 377, 0, 0, 0, 378, 0, 379, 0, 0, 380, 0, 91, 0, 381, 382, 383, 0, 92, 384, 385, 386, 387, 93, 94, 0, 0, 0, 388, 0, 0, 389, 390, 391, 95, 96, 0, 0, 0, 0, 0, 0, 0, 0, 2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 97, 0, 0, 6, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 9, 0, 0, 0, 0, 392, 393, 0, 0, 394, 395, 0, 396, 0, 0, 0, 0, 98, 99, 0, 0, 0, 397, 0, 0, 75, 76, 398, 0, 0, 0, 0, 0, 0, 100, 0, 101, 102, 399, 0, 103, 104, 0, 0, 0, 0, 81, 0, 0, 105, 0, 0, 0, 0, 82, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 400, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 106, 0, 83, 107, 108, 0, 84, 401, 402, 0, 0, 85, 0, 8, 0, 0, 0, 403, 0, 404, 0, 109, 0, 0, 86, 0, 405, 0, 0, 87, 0, 406, 0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 407, 0, 0, 0, 0, 408, 0, 88, 409, 0, 410, 0, 89, 0, 110, 111, 90, 0, 0, 112, 113, 0, 411, 0, 114, 412, 413, 0, 115, 414, 0, 0, 0, 0, 0, 415, 0, 0, 0, 0, 36, 116, 117, 118, 0, 416, 0, 417, 0, 0, 0, 119, 418, 0, 120, 121, 419, 0, 122, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 123, 124, 0, 125, 0, 0, 126, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    protected static final int[] columnmap = { 0, 0, 1, 0, 1, 0, 2, 3, 4, 0, 2, 5, 0, 0, 2, 5, 6, 7, 8, 0, 1, 1, 9, 2, 0, 10, 11, 0, 9, 0, 12, 0, 6, 9, 13, 14, 0, 13, 0, 15, 1, 0, 3, 5, 12, 0, 16, 17, 13, 1, 12, 0, 18, 1, 4, 13, 19, 6, 2, 18, 20, 21, 18, 3, 0, 21, 22, 23, 3, 2, 24, 25, 1, 26, 27, 0, 28, 5, 1, 6, 5, 29, 1, 1, 30, 1, 20, 31, 2, 32, 9, 7, 3, 3, 0, 0, 33, 34, 4, 3, 35, 36, 12, 0, 0, 0, 3, 7, 37, 38, 21, 39, 40, 0, 41, 42, 4, 43, 0, 44, 0, 8, 0, 45, 46, 7, 5, 47, 4, 48, 49, 50, 4, 24, 0, 1, 51, 52, 53, 40, 3, 8, 0, 54, 2, 55, 56, 12, 6, 57, 58, 0, 59, 0, 26, 0, 60, 61, 62, 63, 5, 64, 25, 65, 1, 66, 4, 67, 6, 68, 69, 70, 0, 2, 2, 28, 71, 72, 73, 74, 75, 0, 2, 76, 0, 11, 0, 77, 0, 78, 79, 0, 9, 6, 1, 80, 7, 0, 1, 81, 0, 82, 0, 83, 0, 84, 85, 86, 87, 0, 88, 89, 90, 91, 3, 92, 24, 0, 6, 93, 10, 2, 94, 95, 96, 97, 14, 0, 8, 98, 99, 0, 0, 100, 101, 4, 102, 2, 103, 29, 5, 9, 1, 31, 26, 104, 7, 5, 105, 1, 2, 0, 106, 0, 10, 107, 108, 0, 109, 110, 111, 112, 113, 114, 11, 0, 115, 0, 6, 18, 0, 7, 3, 0, 116, 34, 1, 33, 0, 4, 8, 117, 6, 1, 2, 118, 37, 119, 8, 120, 18, 0, 5, 16, 0, 121, 7, 0, 0, 4, 14, 0, 10, 122, 2, 13, 2, 0, 123, 124, 51, 20, 8, 3, 20, 125, 1, 6, 126, 127, 17, 128, 10, 129, 0, 5, 130, 131, 132, 133, 134, 135, 40, 41, 136, 137, 7, 9, 138, 42, 43, 20, 139, 140, 10, 0, 3, 15, 141, 10, 142, 143, 144, 7, 145, 4, 146, 147, 148, 44, 22, 149, 150, 151, 29, 152, 2, 6, 3, 153, 154, 0, 49, 155, 156, 1, 157, 0, 158, 51, 21, 53, 159, 160, 3, 161, 54, 9, 8, 162, 163, 28, 55, 164, 165, 166, 0, 167, 168, 21, 4, 169, 170, 18, 3, 0, 12, 171, 172, 173, 12, 174, 175, 5, 0, 176, 177, 178, 16, 0, 0, 29, 0, 0, 9, 179, 1, 15, 31, 59, 4, 4, 0, 36, 180, 15, 181, 182, 13, 7, 0, 183, 184, 185, 1, 186, 187, 188, 28, 16, 189, 31, 1, 0, 190, 191, 192, 22, 0, 24, 0, 8, 4, 2, 193, 28, 11, 194, 195, 8, 196, 197, 60, 198, 17, 199, 200, 201, 1, 0, 202, 203, 6, 44, 61, 3, 21, 204, 24, 10, 205, 206, 4, 207, 0, 47, 208, 64, 209, 210, 211, 2, 5, 212, 213, 214, 215, 216, 3 };

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
            final int compressedBytes = 1382;
            final int uncompressedBytes = 38977;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtXM2O2zYQHnIZl0mDltlugOTGbHPIcdsnmBxa+FJgjz16gT" +
                "4I00MRoE/Q2+ZNNrc+Rh+lkmXLokXqx6IkSpzvEDhryeIMZ775" +
                "IakH89/u1RXAnZb48hm8vccNV/rx+pX4CcQWnn8P+hcAqe53t+" +
                "yvP95/wZ9xC/gBfoS/f/96888OCARCDTL/B33f8uMHTF1P74wG" +
                "xgBUxj/yGbxRsAEFjz+o7DuBcCVA/wZyCxn/wAaFhlvY6oyCbu" +
                "HbN1/hbfL889DC398Rf48MPPrwZ8u5RaZ1PPBAAeW6mfy/8P+b" +
                "3P9fl/5/nSnrQ+b/TOT2K6Xe+/9T4f+wJf8nENYGZZOkKNlR5W" +
                "xqjnnV/u96z55qRfzZLopUZCMEAoFAOIGBp8hA+xPPw2iOj0X8" +
                "3MHw+PnwJ/UPCZFkjzrPD6Vt/+yQOAKacy/hEdTfVv/mm1T9B4" +
                "t8H6Q4UpSpp8TioqR5ov6Femf1L68P/ctj/2Lfv/z10L+E+PqX" +
                "phpOynjCW+7SxDrxwHYP2c07MJj9a6f9XxX2T/17wmT8dQJPQf" +
                "Ry/UFWpeYprD9EEH/O9Is9tYtLt76512+s+PPalX/dlvlX+XwM" +
                "/3yH/LImP0a7fhWEP5Wv/2D372Gd/fvkwRvWb2qsV1B3Of8mkg" +
                "7CUtafqPK6IJTKAfUUNOdR68yfF4Le/W8M1/9eOtr69x33j1Xt" +
                "Z6b+kbf/6+CHSv9XOCQ4jvuetY0WJxy/cY4/5nos9vongP219e" +
                "8n2n95sf8h/LtTvPiYuQJDzO7SPCtc7iCrVOSLYo5y42Tm/eOn" +
                "IuXR+bIKwo3B5jFJb7hUQf2/bIGIrPzyWVeU6wfDJX9yzB+czZ" +
                "85zN+X2vy19G+XuH5xcf1KiGUmsS3ZZiM8tR8bqaagwiKoGsxo" +
                "Fy+xJBwWakRb0uDW31M56VKXfCQtXuLF/ww7/TEjZFUxGlF88V" +
                "JlViVvKk8ycsrwxYNaM4aYidBXT+RzrPONqEpNa7uLIb0MyKb2" +
                "rBXzR3dlGLdrqIMJMmvyznU29P4oILsYPY5lf3E2oyYzQOFXkG" +
                "kYJRuPEGZLxZ2RaROq6F9eRSEb5ktH4wdmsLpFr19mQLh4WkSb" +
                "/lTr7PH5Ru/3ENMhw+XzJJZR51qD8xfNjVssrNsKK1mrjb3W5u" +
                "Pdli/Z5b/rs38zVwD8WI3a0i+lcvgHr+Um1A3skq5d5jUmVinR" +
                "TRZssJ+NCzFdvGRDJDfkSosCTyNUTgRNKjiLwlZc6bC7BlN/f0" +
                "77/uXT+uvGv/5qVtKtWCh/JvP81ROYZ/96jXV0V4ZbVx7FE/Xf" +
                "1fpdn/0/m3j3/1DmSfGXeC91Peje1QHO7oWiIQOYdP/5QtFdfx" +
                "C3/i48vzCn/RPG5zMdPOibhDUaXfxyn78Az/kL3eX8BWN97Gtp" +
                "mJyxVNJZeMpkMVx1U51/GuJOfc4PEf9EGNDOzw/sTucH7qDj+Q" +
                "EzzIbs/T9YtfLa/h8RI7WIfn5NWFGAJxAIAfkfnPzPDvxfXkE8" +
                "Sok5gUCInL/l2f59nI6BTPfrjC+R5O5CU+iAz0+gzByUxq99/d" +
                "S4DySoc9mtZlBef4c9/z/SsSS2asOu6p+36V8Z1aB/KoP7jn1k" +
                "/TfydzwvcpQh7JgwiL8rGnTZ3/7Qa8a/ANP4/7QUYMvvil/70G" +
                "LJDyPLH5NJdx4L85COiTuimgaq5B5JdCDFDsp/4n1/kqSigkBT" +
                "PauelSPJk2ESLsL8TqITKVJjSnD6/eQE/Ccq/UsGUbw/DYe4gH" +
                "Lk4Sxcobg684xw/tOamGW8v5Qwmh9i6Pdfyq73i1j0Ei+XGG+b" +
                "4qRWFMPunzl4EggEAuVflH8RAhTUrmDPPJf8D/AJI1E=");
            
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
            final int compressedBytes = 1216;
            final int uncompressedBytes = 38977;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtXUtu2zAQHRJMwLZZEEEX7Y7NSYgCBbIpkCPkKDxKe5TerJ" +
                "EsR5ZCSRR/Q0rzNnas0BzN9w1FyQDpoJcO8MUhFgiE9jH1Y+E/" +
                "kF+iw7IxSDQwNbxVb2/E5cCDAgby681MVoLpXuVMCMb6oUvz8Y" +
                "rVqMiTAmAm/qem+ux9kV0/kMP/dx+7c6+0rEEViOBYzV/9XJMy" +
                "L5nE9JBa+KfR/szL/lR7/YIqIXjdJ2s9hLyEmGYO/9XdF9glJf" +
                "KZ52uvgGlKf+HJKIFjrfGHThrO1/nD2SGdCVEdwBGxwQKOTGtU" +
                "RfJLX/kJhEypnlBplrNBTQGBcJIeShfKZmqlh+NLEorwXvjKvz" +
                "+t8G/IyL9d/P+z9/w/0zaQ+6B9+5ct/XELf92CqGt3ah3SWjmb" +
                "nyHYj+oPoWlSKlfj13jGjy0lLlnscCRjKALvDTofajpIM+na1T" +
                "pDIbQYPyn4Q+MGKbn+y4/mP9j8N5a/to7BU7Gv31osb63k/IPx" +
                "w+p+w4nS0sg7+Kbg/u2c/jz2mccAE6B/gZTw8voE90ZoeIJnMM" +
                "9vL1++/YPvr7P8pdPlbwEEP48+c/6pun7paQefpZkXK1nlZfMy" +
                "l8lyzoQ0utu2zhn7n/T1U07yAgsXgpz/VA6139y0fkX8gXD4wp" +
                "2tInCKnwZx+N1mnhVUvscDyyfA/v3zJnJ8Bee/2R3oIAMqx1UX" +
                "OdcDIaPh1Nkyii1+/lH7L7D3D0Ga/SN2N1VRkeOTEQrlvH6m+7" +
                "87Ie/6A6LTP7vqn/V7xtz61z7nm5lLsp3zx9YfGo87Prb+ZuIf" +
                "3vyB7E/jMflv6/OfXf/h49PsHzp3/OH3vzS+yvWDEjnhQF1riW" +
                "7nBi1vkGHTk5Ljpt+Rd3Lj0CvDaMlO4b92if1zN/sXunj4ZKv/" +
                "0eufCcZbr6APWNuJMEDk+q93/5h2fpHq/M04v57PX6ICF9JfPP" +
                "/D9p/E/Mm01n/Wyn+UWwl++a9g/aH6jWx/DP6tB+U8wo77r+2E" +
                "P7R+/7W3/LH7Nlh+CrN/9oRN2a3+wal/MegfnPwTDbv9J0z+bP" +
                "bfK7+yAfLT06Ky169J/MCt/W6XBib+B/T8u7bslzH/reTfIvMT" +
                "UCp4lvoRUr898xdg5a9Y/eHyt7L8BxLxnz2cuXb/QVo/aKZ/C4" +
                "finm5aFc516YUl7J8O0H/YNPH7UX/GX3+yIv4mZmlJo3ljkTTM" +
                "/Adur//N44d/tP/jUvw85Le/hdbQ8FqFAbqDmhAbp2ot/8j84y" +
                "FkfGzcLMtf5PdjPL8q2/XX7e/RqP7HQuzPG6o/5+3fTt7/RLaQ" +
                "al9mzMj3pI8KalxvqeH3w6Lz/3L9LLb/runxsfUvz/zRDpRff0" +
                "fYP2rQ9++hx3+z/ld1/qD1i5NA7I0MVoutlvP39f7/32P+ntz/" +
                "r7r8rXyZZdsPzr6HYp2wQp6fUASmJVPmz1PY6zQ2W9axhTQqSs" +
                "4vAHc8VjE/J5lhHzwGQUWMTERI5uCNdEk2TH6FzJm35+fjm7jn" +
                "j6XqX+rrv3pFKozfzzz6/qsj2Y9AIBDazZ9Jnj9qsTq42PqJzV" +
                "+sdpVoZS6CrJDS/mdHX53Vnm1pLC3kGgfNPvvN/BZnfgIhTwwt" +
                "NJ9VNcV+GTBf2P0HFtLekA==");
            
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
            final int compressedBytes = 872;
            final int uncompressedBytes = 38977;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtnUuS2yAQhoHCKZKaBTWVTXbUnIRknzv4CDkCuUly0+hhO5" +
                "IsyUi84f9qFh5ZEqK76QdCMiGOSKu9GElL6vb3UcSQ/s/m2unk" +
                "qA6jlpsHpejHDs+MDel+l6uvHhgHxfT9M/R/N9UXKifGxccv3m" +
                "TXR/F10pIR5If7lcsK7Cd1/zftV6/Zr5rab6OMw1WEOTknYMNn" +
                "AsgKZMRa/CfW8V87H2+DzjIHkPbR208OEAtzRP6e7UeFsB/ED3" +
                "AKTebFXhsVCeQfsf6GTyo7/3HQv1q2TwPlTznrL0b+6Ocym4vJ" +
                "rKDxFyzuhBZWE/ZvET/prIlp/29fDJNqXf8nNJT/t+h/0vN9Gq" +
                "xFXImWqzNR2MDy53+P99x4NZQk7bddfxQSvwHYqEAr8r/wXyBD" +
                "+2Xh7dcEHv2Y7U4+w5BV+0U4XgP9I/6A+hGz+ERh7eXCbeSPBZ" +
                "YAAC/1m1yt39Twf1+/XYYveF+/0Xv9RnsfFK5+40euP9T6TfZ8" +
                "/Nh/NvZf9seHX7mozhWs3tpO2T7Iz11kj7LY4tea+eK8GCewX/" +
                "T/dPy+xrz/4eH+CyXbU23hXUHq50dzfH5Vlz1INakGc3r8iZ3x" +
                "px7jTxAAQFgwIwwACBD/AQBIItB1AAAAAACQQf7lWr+mrn9bbz" +
                "8IupxLhf7bYF3O/fK02/1Z9dg43LUWmdWkZv/902plM1fV62+f" +
                "I/3H82tusKL1h/mnnA2r2ue/dS1Kotb6Y6/0J+Ppj1Wg+Mj5Y6" +
                "sPViVfRlVV/mNeb8smIj/8l0z0+yeJ68Tkv/9iVf8sRWWO2X/+" +
                "+V/d8wSt5t/6nLMuMBcyBKB+W6/f3rfix1sF768GTxXPa2lLCG" +
                "xLiGfihJpJVEGQcBSQ/y0utVh/A1AJBsfnL38920/OEz0+8+ti" +
                "fm5eu/24hrUm7Af+o2j9p33/wwA/KgFqPS5djwcABEOZe6bRpR" +
                "YXIiT51H3+835LTLoRqi79x19X1hVhXJGP+1zVZ/GbfLtCging" +
                "8U9Oo7VPHNtvahGGz/VfJ5INg7EI4o792sav2/tzX79/9ufy/b" +
                "Nk/f2zOQhZwn5AsvgHQB4hTuL64yFWk1m56W+KR+/4T3Zu/dF6" +
                "HXDm+UGBaA3c4n+I9dcm74zSbM+EFFGiO10kM+Tv+onk6HrMYr" +
                "O567/jHyZuhL0=");
            
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
            final int compressedBytes = 889;
            final int uncompressedBytes = 38977;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtXVFu4yAQBeRKqMoHqnoAjoL2f+/Qo7B78tb2NnXWjg02MA" +
                "O8p0pp1VBgmHkzj4ArRBPwolOo6Ut4Ob1MsK/S/Pt2fB3mX9yM" +
                "kEK/Lyzltfj1YD71+KfNVn9WTi+S1/jTYhDAAeb11/wG9uC07v" +
                "DtbjkLf+j9qxgBwr2Frj2REyqsH3X+txT5VwTnL8fbqu7YkE/n" +
                "b1nNX11ZP7U9/r8pxh/kP27XfpLS/wvxh+Iav1vtNXH9GmN/av" +
                "7KYf8Y/bBsL4n4W3nxZ6eQsF99+o1s53UgdwfEz9J+i+LX/LSd" +
                "RdH7slHw/I/jP7B/veo/kf+SzZ9J/eBTxm8i/a6q5m8XPxei9X" +
                "uLWT9Pkb+gm//fbCmgyIagLZam9U8i/lGM6yfsH1y0v667fu59" +
                "/YCu1r/X7fv4eTvi9gBAiab8d1nLW95DtXA9AIiCpqvWlDCb+s" +
                "NOP4/642X6xTDqD/mtP+RISZn1j1rrn7l/Nfdvxv5NFeQF/dWX" +
                "2vKB7/QwFtACUPQBTTM6APtRQjXuHb7XxHL685/h+fmHp+0z6D" +
                "du9lPr+b9t2u+rza2B+ZdVEg80xOuajbvAPgfthzyUhGtKAHDn" +
                "rxWFmR/S2Yq5ISYQo+4PnD1//N1epz9/p47tR5MoCp1fyrZ+jP" +
                "WPuWh/U+v9ndcqz+/62LnbtP1DvwM91w8J8pfLlr/47R8kHH8m" +
                "fcQHjPnvyvrfLt6/amj/BGCyhXOVPSzd0MPZwmCtgdJpomn9RQ" +
                "VEMnBGmdyfwpb28z8V2F7e6/9z/d/bw/9b9lLc+smkIfHRXyv6" +
                "G6i+MO76+Qe1l8PH90d+Pzn/NN8f8XaLYozb2UuYPcCNb/ngO3" +
                "+947/27r9hT4Dd4d89+4lo+xEUPqfHL5KMv1D/T1bQgrIay1+d" +
                "nn+8rnVqAU/poInH70+fvzChYYaYACqC68p/+9wR0c3MB/yD/Q" +
                "sO9rOwX5z+x/wRv2XCFP7L0f9aeH7/Ff+p/f8vFImf2uLPMbN/" +
                "xjmk+v8LJ0ZokbPAP4n4o239SMGfffEH9h+AKvVfF/sX2H8AuN" +
                "Vk3McfF9rp8y91/wBAgAFszQf2kSXKEYYmUhjlKZHX8wuREoAE" +
                "1XZorPoyMQyvBhoWZPH5Yw6I8PsLOfJXov6xfwEAAAC0kr/ZPX" +
                "81P84ogXrKeon1R/wVhkP8FdAvQnwCuGHQHg==");
            
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
            final int compressedBytes = 811;
            final int uncompressedBytes = 38977;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtXFuS2yAQHCicYlP7QaXykz8qJ6FyEh8hRyA3yVEj2dbGyW" +
                "r1BAaG7p/VloWlGZqmB2FpIk1EUd3+3OCtco/D8a+5f/DqSJH9" +
                "SnE6jaKlH1QIeuXzSE83ttz27TxPvUAdbBf+HkpNlp7hP93579" +
                "b4/zok6Gz7rP3HzZ9yFwkVZatbBD6dcJxK5U9cf04/Ptcw/4Zd" +
                "Z8djaQPk6LdQQeuPrDbBd5id/p3R/7lZ/+Zv/4/6e7l9YEb9VZ" +
                "P+qjHCQX8X/d/Y3i3r92p7WmuP8T8z/5h+ButZ/nLnL/o5irqw" +
                "oLn3ew3jKVfMUQAAyJf5vu9/f/v89WsV+SviH9PzT6WqNJqIH/" +
                "V331BVC8v6+Akn69dwevz1nX/u+p8jf6GM/6kk/0f8h29Iv7L5" +
                "F4wf3vohUf7B/7z+tff6U6x/wDoIkAC59VdXfv3KYRJOe+2bGc" +
                "kwWxJskCdA8Pyzg9+Jnn+z3X/TbjeCuA37NwA4vv5giq4/zNn4" +
                "NCo7qV3Yard8hV1oH5kxz7HoMKMLz+LuNsZPJeIPhflb1/qZvP" +
                "o3bLt+E+v/HAhV8Sc3/6U4Yr752536/ZBs/4glq82S45EGAPMP" +
                "6r+E/kvv8F9uoSbQyaoGzF/zcMztueEgCMewfSe3zGdXsfHrR1" +
                "BYcP2g8s3/QBX+u1n+N8E/wfs/c/j/PetPz+1Vrvjr3/IUwV/w" +
                "V6z/KLd+4eFfgD36Vag9AADF/INlisYzZ9M3zYXAF0HESAQAoH" +
                "T9nKP+TvL+4FCR/+VYv6kpfo7+L+vfmp1/sX9VhP/LE0zv+hES" +
                "VSZY/2wE5ffPcL8/+EP9tgvjx7/ptxXS8bHz63P7p9rrn3Pxh3" +
                "z+qfX8c8eP5//Qb2n1F/gLACAjAMqWhe+6mwLh96fNjq0FK2PA" +
                "8Wr9q8T4ufcft5R/WE55+tES/3yc7tQSXcg6+jQc//7ymFOGnv" +
                "OX8fDnVQ8RGk/fp0H7Yn/Rt+v/37f0+/EPLMf79Q+8XxX60Yh+" +
                "dfH8AEsGx4qpf2iC8Yv+BwCRgH8DAPj3jGV2Z/53o3/A/uea/F" +
                "d/7y9r/f0TqJ94xw/0B/kDTuAPjbPIjA==");
            
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
            final int rows = 98;
            final int cols = 16;
            final int compressedBytes = 143;
            final int uncompressedBytes = 6273;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtlzsKwCAQRGeDhUUKixwgR/FoHj3EWGyRIgT8rM5DUBBhdn" +
                "AHBTQniEkSLfjHlgeS5Kk0gYSyvGf3bOwBAn8op5NHbChzTP3D" +
                "+he/+dcE6XB+pPot+jdTfkg9/avXT/h+6f+E8eX2Op39W3zxVb" +
                "dCsJK/E+c382eB/BbWTwj/H0b1g/oJIRW4ABvJHEA=");
            
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
            final int rows = 44;
            final int cols = 109;
            final int compressedBytes = 4030;
            final int uncompressedBytes = 19185;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrlXAeYFEUa/f/qNN2zHAYURREkyh6K4JpYYcnKqogBw6koIq" +
                "CICpJEThTBRRAjJhDhUElnFmQB8UBPFETFdCpJPBMoJ+p3oIjI" +
                "3F+h00zP7uzMLut+19+3HaqrqsPr9+rVXzULCIy9DTGw2RyoDf" +
                "vDAWwoHAqHGXPYh9AAjoQWkK99hs2sg9lpcBwUaJtxi30FFLI2" +
                "MAM66ftBNzbcnAhnQy+2Gv4ClzDHuE+fry2P9YR+cCVcA9eaJa" +
                "DjfMgzRrM6WjEugoPgYPYEW6aPhPrwm/UlNNY3YEx7G5qza+DP" +
                "OBuOjj3gFOJCOJa9CsdDW2inrWZD4FQ2j12IS+FM6KHtxj7Q0x" +
                "6lbWV3sWu1b6A3GwT9tUNhIDZmlvYUOLCfdowxD+dBHTgQDoG6" +
                "Wl04AppAU2jGHteHaK9BK1wNrdlyswucBCezrqzAaQtF0AFLMa" +
                "EPhq5wurWL3c9mw7lwnlYPZsIFcCFcCpfDE3AFOx6uhkFmDDQw" +
                "WE8wwWIGG6EdBXGoZdWFP0E9fMfZwEZDQ7wcGrG49nvMhqOgJR" +
                "yjD2WIF+JeNoxdjU9DGzgBTtRK4BTzMWgPHaEznAbF0N204Qw4" +
                "C87BPDgfLoKLza5wGfSBvmwNXAUDzBmJBOvIivRF+hL9Fqsj6w" +
                "CD2YNsqD6GdbaL4fqEWjhmiYT5Jt/XD8ItDm1ZG+ydSMTG097D" +
                "sdMT3sLOsAtiJYTZOeKoPZUroW1xImE3Z4X62bhI5tO3mutovZ" +
                "Tvm41orwFtv5LnYg/GL08EFo4Z31o3m0ymGNeJ9K2U9oT2DdUv" +
                "MEsksDGlLkikLFrd0NFrVKI7/X1vFou77OqeMT4N5tN/Ernr4a" +
                "XBVPNvfG1fJkquVU89Qm7tT8RRD2eTdrjKfZN5TewRtf8Rq8t2" +
                "m/1Eni5+jVZRqH5bbjFPHau7028GemqOGQwBBiMFzy7hZwTPbm" +
                "CX6k/CX+Em4tkIyKfyzex12ql0tsAaIHIRz2jdybkJhukP2tOI" +
                "Z6Os2znPzNFOO3sFPem/OM/ENYln5mpxn1OoJsEzus+xArM5VE" +
                "tjq40+jWPGeUbHR4u3tw6OpX3iWSLhaBIzf8E+4ln7yiPJM5HO" +
                "MfuQUvZzc8KBEjPi2XDOM6s9xwxaCcyWm+dynpmrzFmUU/BMlB" +
                "kK4itks8VV6FuFGznPFD7H0/Egkc9gn/mYQZzyEs84ZvE5Wldo" +
                "aFucZyLnUfR3jPkxO1Hh8DXrzXnm3aXgGW2JZ/ZAxbMxLmYez0" +
                "bDAHsu5SJttEoEZn1D2jjZHOFqo1GC58Xz9X9wbcTJ5iicBIXG" +
                "RHYldMJ7oZv+FQa00XzL6WCvFPehtNF+FHRWDPTVsOfpDkZyzK" +
                "xOzt1cG417OGaxJZxn0Dy20seMtgozaOfU5tqoUkkbxXaM5JmP" +
                "mbUKBorvUePaqM4IbVSYKW0010vMoLW20DyfY6byEmaivNBGiZ" +
                "nURjqntJH2rqA/0kbnV6GNX3BtdDFztZEwm8sxo7RGqm5XG5/z" +
                "9GiA1EY6d4qLWZI2TonSRvE9X6H9ALfA2NhIbTvrBbfC7YRZif" +
                "We5YhrTWDnaT9aa5EYY/RPpFmcW4NH8T7xxiGul3h7pKTW+/4Z" +
                "qM95Jt7TNKtfIu2iLbXyYHxqun5woK7+jvqG9YAOwrh02mg3oW" +
                "2peUFqrebGpKvXS39n7JuwNiqFbEqlukbc71BrplfrT4lyFnZ+" +
                "ijbephR8IGnjRM4z+0qYBJMFz+5knc06xDPSRrsf5JsHYTO4w+" +
                "gPd0GBWdv6yL4KCs1aXBvhbhhm1YrXIZ5xxhDPCLO+8SbmgWYe" +
                "55neQ2qjuvoyl2feO/2N84w0LWYKZkTxLEobVX37i/UB0Jtj5m" +
                "qjGU/VRtoqbZSYQSux3RbFM18bA9gHtFEcu9q4LayNtCZttK/m" +
                "mIV5xrUxdr01y0OkSzptpLXyIGFtpPR7Jc/0nqwI7oNH4GF4AK" +
                "bCo0AeBKYzKh87w60xdiY2S/89xB6KhzgSfzbu5ZYeBKaotvYk" +
                "yt3DKzdEYibedYy3Z2l5XDsaM1cbJc9czHStvC+Y88zhrWmjyO" +
                "cZnMh4YXujeOasJy6MC2F+v+LZ40HMyq7bxYxKP6S20xSKO/VX" +
                "zT3wLDxFPPu7qG0opf5iTLbvtgDmO7v1FdSeCRRMcYfk9a+iff" +
                "8ZV8THBq+VVys+MayNMFdh1p3zLHjOuMdC9R7fDvgoKwmzQdGY" +
                "hbXRw6xuJpiJ8m9FYjY0KhVmh46eVjVpkdpIPDN3hfLPUZg96W" +
                "tjWZjBWT5mlgbPqNR5akseBJ7j2ujslR4EX+AeRH9Pf9f1IM6p" +
                "uEB/X39HeJAX6Z5IG0XZTvTXjZj1AvcgrjbS8d6gB5HayD2Ivp" +
                "a3Z742Qn1rvtRGSu8HzaO1kTwItYgpHqRn0INIzNJ5EHheaGOT" +
                "sDZCa/1jWqdoo73I9SCURh5EbFM8CK3Jg2gNpQcReciDcG2Eeh" +
                "wz69AoDxL43j5I50FcbdQs6UHo2NXGF6Q2mjuhlHj2Iiwknr0E" +
                "S2ARLIMF5i/iPTaFxXHqQTljkdw7LA3zTN+SyGLxtVG2Z5JnGA" +
                "umpvBsdvn1gueQMtNG8ewdEzkuequo1FSeRajqrPLq9nnmPePL" +
                "Ps9wM+eZqby+2YfzDHc4t7k8o/ZsD/4EwutbC3AL/gyFuI3NJK" +
                "+/DrrhRq0F5xlu5TzD7fgD/qZ/h9+6PMPdoOMGyMNNdB+fx3oI" +
                "r/8i7sJ/E88W4vegXKbPM9wZ5pk4iuCZ9yzl8eyVaJ6Zl0XxjP" +
                "Yy5pnRJYpnArePUnkWxqw8nrleP8Cz5R7P3iSe/RNWmn1jHeA1" +
                "eANehxWcZ0ap/VpMfYnsULc9w8mE2STOM4MYiPeKL2JiZl+lsT" +
                "iFdV3c9qwsnmWyZMWzy3LlmXFWujPl8cxYUi7PpqTw7FXXgwAj" +
                "zIhnVn1YBavJ67fgHoQ1Mkq516ec+awpy2dHsobmXijg2sjjIP" +
                "Zq4xXoxBrAMFHb2fCW356pK6g4CO3pQcxke8aaudoovb7KKXgm" +
                "MfO9frl49U7GLDOvn5ZnGXt9o1cgzfP6ErNUrx/GLFOvTx7E5d" +
                "kal2fYwn4nNkM8bS2kd8ZaSswCPPYwk+1Z4EyDbL9QiZm+NJln" +
                "xsrK4xnOK5tnVsOceXZBtjzDp8ttKxX69hqvTL7PM3ut4FkL41" +
                "F2ERxAz3+D+Yt9DB7NeYYtebyRXSy10eeZ5xtdno0qi2fGm4H0" +
                "QJ+ae30IRE1kXL+yeMYxq2KeDcmWZzKuXxbPfMw8no32eNbG3I" +
                "PH0VfX0m4vYldjoURqozMVJpjvhnlmt5M8o/ZsfUV5FmzPOM+g" +
                "vl2UaXsWFbsqvz2DcVXcng2vuvbMjV357RkWuDzDk809bDKbxC" +
                "bEOmBbPJGNE9o43iiNT7KnU1k+lnIn/d0W1EbCbEPSNe74Y3gQ" +
                "HtfPzINYl+SM2fQq9CApXh9P8p5WxRsprZDHGynlTlGr50EIjw" +
                "VAiPB4o9NKjsXQ+Y0y3qi08Z6KeRB1PwvdPnXQgyTHG9Mq/sGR" +
                "2lg3Uw9iDcxZG2el00bay9GDBGpOijfiKcDoSGDmdMN2HDNVaw" +
                "AzGbvC9lBgt/Mw2xRqz7LCzC7KBbPU9syNN2aI2ZCcMZu/LzHD" +
                "IolZkGfOaOwYzTM8D8XzgFJUgdln1Kfu5GKGnbPBLNalcjGrmN" +
                "fn2pgjZk9XB8+0k7UTcDN2x9OsFjAVi5HuFkilcYdRGtDWPajG" +
                "e2QchFK2GZtpvY7+1HgTiuifjIPQ9luv7G6YgtT24SZjsabGuJ" +
                "zPeRyEayN+H6HkO2tKn9p6O8O7uz+1PSsvrh8onRTXRx5BPgMe" +
                "wTOtlkm1BjDTvK/d8d6x8XnG15ySzoPYRYlKWqoJs0/3JWaoYr" +
                "Ua6YEcP8NecvxM68x5xr1+cp866apfZOr1ozBjaoxNO6X6MMvd" +
                "N1obqoNn2g0CQdmCi7FLTWi1xAzml4HZVxljNjc9ZtXJM21bGX" +
                "muy2T8zD4kw7ubUzHMIBTHjBo/wwHcg2gzZVxfm8/j+iLe6M5v" +
                "nCMx43H9gAf5WsQbuynf2CvKg/C4vvQgfPwsOd4I9cVxSlw/ef" +
                "wsMq4/JvO4vvIgKXH9tB4k47i+0yBdXD/oQbSpqXF96UHKiuvT" +
                "Ns34mfYslGrP4EBYqD0PL+E12ot4rd+ewWK/Tw1Lk76Ub3LvU1" +
                "cvz3LXRqdR1r3xJRUvg4PUdhjegNfhUByMQ9J7ENwT2FcxYmOL" +
                "9I1e+tYy3tKyVMy4b6R1NfrGsrQxQ8w+3KeYebOEoZS8/nDgEY" +
                "mXcCStl+EI1+vDYt/ruzzzvP7WTL0+/ble/xUvdVf1Y5Y7z3KI" +
                "euXAM7G/GW+kJ1mlnuh1kRbon2lv+v2zAGbfZoHZG38kzNjyGo" +
                "bZqIAHEXMLtFXKg7wu5xYEPMg7HDMcKz2IO7fA+M6dWyA9SHBu" +
                "AccsdW6BxEx6EDm3gGOW5dyCP4QHoXUGHiRqbkFWHkTNLQDE8X" +
                "gHirm+HDOxJcxE7IqcPLTw4yBwnN+eEWbbOM+gm/alKNPLbc/w" +
                "B7EWPJOYcZ5JzALfjIeZ9+abR/MM2sW2p2IWxTMfMy+1DtV2u9" +
                "pvEuYZtI6lac/AmwPsYqaOPMzEkeCzjxnt1xLryHnH0DKKZy5m" +
                "Ks9pSWUUZt57maC2D4MYjwQxxx4fAs6EFakeJKiN6qr/ifYgEr" +
                "OAEm3xtTE7DxKrIm2M7alh2vigq434KE7DR7Qd2s5onrle3+WZ" +
                "d1XxtmX/jPMsyuvL/pnYy0v1+rJ/lsqzpFyR2lgZPEvnQf6wPJ" +
                "vqj5/J9kykTnfHYkR75sf1Fc9C7dl20Z4Nk+0ZPhbVnqkYse55" +
                "kJ+D7ZnkWWpcP9yeER+2V01c3z4s17h+KE/Vx/XXuGMx3DcmcX" +
                "Bl+rh+wDf+kIVv3JWNb8xEG/H1zDXG1Ua7fs3SRu9Z38cP8EP8" +
                "CNcG0j5JJMw6PmbmQejFr83aqjf5u/Ej5Xw3ulb+u5iKxUHM/d" +
                "KfS8cz93cxYczMeMbt2baaiRmrxf7EarPQG2MHlI9Z3tHGf/cV" +
                "ZmnL5IiZVkMx003Gf00axuyxFDZGjBOxmekxYzP29TvIRhurMw" +
                "5S/nz9Msou1pPGE/Qj2BI+FhNIOTxqLEaUbpD2W6hXznWbVT9m" +
                "9uBqxOzlHMqGRu2wLVM9ruCYJx3l6w1Txs925DK/sbIX6F+zeJ" +
                "aNNrL1HiOaso2gfvXH56TqR0CJN+Y5IT3PjJ8ryrPUOakVwGR8" +
                "NpiVPSe1pmHm/p463D9jmyrQP/sl1D/rnEn/TM/Ppn9m31qR/p" +
                "lIyax/NqZm9c/8uXLizXVnX3sMaZOsjWnmFuyqjPkg/x/aWLlz" +
                "ePRCYHpXl2f8f7roxS5myb9lSprf+Gvwt0xsRza/ZRLH5c5vtO" +
                "dUEc+erFk8YztVXP9/YadnnA==");
            
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
            final int rows = 44;
            final int cols = 109;
            final int compressedBytes = 1701;
            final int uncompressedBytes = 19185;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtW2mMFEUUfl3d1dMzw7EKqLuwBwussKACHogH4K4nxxoRDS" +
                "qHCIiyS4IcBvhlVliNEv1h/OEvj8R4xcQDWBRBFtF4sehCjERh" +
                "syHxCPEgYYVFcHxdU9PT1d1zdc8w08NUUtVVPd3VXfX1995Xr3" +
                "siET3JaiSiLo4IibbH68qsiC3RfyMZJro9krMED6d/rLwnulUX" +
                "RvKW6EeZnyOt59s10iqpWw5KK6XHhN9PmDGTzkjHjfqv0j9YHq" +
                "NnsDyE+We+/zdW/in9JSGa0u/G8acx/4T5MN2uLDX2npJ62PYP" +
                "h3vrFdvBntxgFuz2F2ayFsdMeQa3K+UBSXi22eGqZ/3OM/mYPz" +
                "FTnod25NlA2ILj3iqXIXor4jyD7XGewccWnv3ngmfPueFZrjAL" +
                "rfKZbWzhmG1ho7jAxKmteklq4zwjo0g9GU5qLFfVf6l2yzNSl/" +
                "p40pFbzMin5wYf5cPs+jOQ5FrlM2WPshfrA4FhB+UwVLeNgIjA" +
                "GHbVz9n+K809qMD23cr7ucdxJpthBa/1s9tGqBRalyZA40ZW3s" +
                "Zbs6ApEWawnNdDxt7BiNAIXh8pYgYTEvEMbjFqd8Mc0/4HMT+E" +
                "+VHWklmpQsD4vT8rKxz7HOeEGUzCfINxzO2Wc+6F++ABE2ZPxT" +
                "BTvpAn8jrDTKnSMSO12OaYKcOiPBMxY09qdSLMlAo7ZsKZdQWA" +
                "2Sv5w0wNucDsJWEGN1NCOIrKQYp3QzbGbCNpw/ws5lbLVS33Rp" +
                "7OpwaR9rrwZ6/5y58ZfDiC43XQvKm1vjo2XQ1CNUODHC4oDfJG" +
                "BnMcyD9mNMgxO0VnKiclbvXgCdiEZVtM60OMe332HtTLvGp90T" +
                "amwGSjG8zgyaSYveUvntEZfBsGgmPTgGMIF8bXZ7ABhuOeeqxz" +
                "bwRX8e31iNkL0IC1Nax9FzmB5f0wzzRfS2EJrylmzGAIXCTM6w" +
                "hTHbkL/FmA8ZivhuvSwHOBFTMoM2qD+LYK1sIoqItiBlcwa44c" +
                "h2thMj9iKkzjtdUww3KFdTAX5pvaTHUDFY4Js9JY5QLqbKjl9d" +
                "GYLxcxg4lwjXHsFLgJGnE7He7AcibcCbNNPS+ERbAY77cXlsXj" +
                "IHSAdU2dhm18PfP1Ge3vxjaG2nNkG7f6c02NZw9CzFrpEHpxhp" +
                "htc4HZ4ELyZ4F5/ow3oo3bbetvN2k0tS6J6kY3iUw5V3PgSjd2" +
                "5A8zcrPH8Vp0Iz2qdkknhCMceMbqGfEMey7PFc/cYJZPnpEFHj" +
                "g6jNbQobSajlEP0JG0kg6no9UaMt9yVH2CsytcX7eOjso7z77x" +
                "KWbj7DxT63R/lopnao8Lno0t8YxjtswDZpNw9XWSIXXUwMzGKq" +
                "f1GVkSKaB0XmE2jU7ReRYYb+LZi+nwzJU/m1pIPMun1g9MyEYv" +
                "mvGuRX1HGOHfONeNkQJPfuOZfNwTVosYz6abuCeTxuzwTNf6As" +
                "9mu+FZmBYfz7xofTonqkECs3KBmdIkYuYuRhwOljBLaWvXC6jO" +
                "c15Tk5eT9LChZBtTYPaqx/XZfFyf4Xohtj7TJgq28818r8/CFc" +
                "XHM2/+TI/rY2myP9pyzO1Kp2lGcMWm7IvH9fmZDZaehLg+ti1x" +
                "fWU/a6UZ19ce0eP6SfzwNqHVzO4z9k66zDbKKsxGXD/OM6e4Pt" +
                "Zn2M6fK7TSjOsnTkqXcC76fWA6LxrXx+wQ19daonF9uiLqz7Qd" +
                "luc2i1pf22lokBZX/mxyGk+ez+L6brS+tstkG1frsSuTbdxvs2" +
                "T1zv4skW0M0mzaxpI/S51Cj5NGUmua3x1O38qxK1cn70lpSnjP" +
                "ddm9Z99hlgXdGPzFQOiTkP7Fjgkz5+8b08EsyT2f55h5iYPQDo" +
                "Jek5SF3k46Iz8m2N+Z1jW03M9Brt95ZvsbHjeYxb7hoV/GYsQm" +
                "n7/JZuP6ijF2lU8N4vEJJVI3/cqq9Z11o67147oRGkTdqGt9s2" +
                "6Man3pNCiGbuwf0/px3WjX+lHdmErrWzRVszCmgtf6lt4z0/pf" +
                "23nmoCU88Uzb6e2pCq8tPq3vah53JbQy7zvsM/4lQPPwbje8Ls" +
                "sxCN/bRpun+zYZZsWQCkE3ekLoIP0e/dl+2km76A90Hz2Q/TiI" +
                "6V3Md67iIK0lngmY9US1fgpV5knrl3iWA53c7bAvNzzrdcWzQy" +
                "WepWDeaRanKKA4SPhICbPMk6oUowbxK2ZqOY7hXTaS91j5gY0R" +
                "bel9+536P4OutX5fCbP8+TN1qCt/draEmcCzYYWvG/vJJcwEzC" +
                "p9gFmghJk5wf/7I9oJ");
            
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
            final int rows = 44;
            final int cols = 109;
            final int compressedBytes = 609;
            final int uncompressedBytes = 19185;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtm8FrE0EUh50xeYeioVShIjZpImqbk4KiYqkeFQvFoqDUey" +
                "+1CL2W1l5EkCp4KfUqaHsQpOA/IJ49G1RCT20pNhRURAvb6TRZ" +
                "duNmk2x313nJ78HM7G6WkM3H997ssGtZlaC05RnysWpPrbohn1" +
                "hM4uCHvbHjvcUyKCNT6v/u9D9LfK5x/FMcv/HQMTBzMcsyYNYD" +
                "Zq6Mdlim6KSbmewyi1kjIT42z4xr0BnzPYuqnrVOUJ92Lefw7p" +
                "TMy6zs9bA0E9jv02AWmFA/PGNI7SyYMSN2Tl3DW30l73S/3D73" +
                "Z2yZnQczhtQuIDe23DzyYmOegVlsRK4kj4giXaLLyW6XQT9cZw" +
                "2ILfuTVfFL9Rt6u6Da1/LxNd1vipL4q8Z1+/w/qn1R7ZtlJY/a" +
                "R3+LFT1+97D3J5jtk+ogPDMvRFGzuVbbM7Edjmd0FZ5FyNGHme" +
                "5rMFNjia7XYub4RjALwbPGmdGNoJ6BWTTM6GaEuXEIzGKj+k9u" +
                "LG8XHNt2bqyawwx7eab2wcwwZnQLudE0ZjTizwz1LOI7sNvhzh" +
                "sxBzHDM7oTtJ7pY6hn/3mu7+cZ3YVnxnh2D54xqnajWG80jEhl" +
                "Xf8+1vVbuZ7tYx1kDMxis3EcudEgGg9por5nNBmSZw/gWSjUpv" +
                "A8CDNi0zSr5iCPYqpnM/AsQpZzzj2ZVy3UZ7/hWQTMnoEZ79hl" +
                "Rs/BzFjDXmgaeC+Gn1k5t2c0j9zIyLsFrDcaRqSy3vgS642MqL" +
                "1y7yfS1bkxcaL5epY4bm49Y0/sdfu958me2RswY8dsEczYMVsC" +
                "M25xYAfYDRzD");
            
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
            final int rows = 44;
            final int cols = 109;
            final int compressedBytes = 517;
            final int uncompressedBytes = 19185;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtmctKw0AUhp2hk+xEUHDpBW8o+AS6Ereu1IVuXbjQl5CiQo" +
                "uP4AOI4B1vK687a6tQLCrFjW0pWusNUWFMY0labavWGHLsf2Au" +
                "ORwSmC//OZOJMsPLpeQVsqCxkzx+n4TZbsosmJFjNgdm5JjNgx" +
                "lFY+Ecvvusq1eWNOYR9qj1cX0e0tpZ2h/V+2uWYC/aGDPin7V2" +
                "qrVz7Q1ZMLxP7EIfr3I8+wFMCupsETojx2wJzP4BxeVUz+tND2" +
                "/krbyO136O5TXFPoU3YaWLJrQCnZFjtgpm5JitgRk5ZutgRo7Z" +
                "BpiRY7YJZuSY7SvbLKzsKjtSiqCyJY7TNP7kHET4DS/OQUwGez" +
                "+Inf52pB9vt2OJB/J8AQ9ibRxE6VIEWFgcCp84EkFxgNxIgFkE" +
                "9YxaPVOidv6LcU2BmTVm4/+zGJhZoMs4dEaO2Q32ICS5Jb+KcH" +
                "mxSiVbz26hMws0doe9Ps2zK7VBSj6R5wxkXGserY0VvhufhAKc" +
                "ZGoz1sBJJipT9UxtEdV/X89EFXKjVca9Zm5U23WPG7kR+8aUzt" +
                "QO6Iwcs04wI8esC8ws2BF228lMjICZbWR7sAYlmxt7oTNbNNb3" +
                "vteHOYZIv631rA06I5cbB8CMHLMhMCPHbBjMbKOaxSzDH8qYR9" +
                "Nj4kPFHDWZZd0TzGjozA2dkWPmAbPfW9kbb97HfA==");
            
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
            final int rows = 44;
            final int cols = 109;
            final int compressedBytes = 640;
            final int uncompressedBytes = 19185;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtmtFLFEEcx525/e0uiiUZhaGo4R154ltQ/Qu99hr4X/SaGY" +
                "igbz4p5qMPgibSQ2+pd/YgZlkgitnVQ2mKilBZGYyz63Eod+fZ" +
                "OTvN0PcHOzu7zO3C73Pf3/zmtyOEECwj8ox9O3H1h+3l+uvsh2" +
                "y3wv6KPN5n72+E7Q7bZQfy/DU3/rc8VuWxJoTXn7v7k30Kz9sF" +
                "3v1dwE4xb0AnM2cNzLRQHRSC98IPBhF5QYssQ69pgd7SEr2id1" +
                "HqjN5AZ4q4TZUa4fTBS2aZxhxkGjpToLF0wMxLBX1a8mYQG/8B" +
                "g9mzj3WH5fgPOnUWuwVm1sXGj2CmQJfSc85+yRzkFzxlELMN74" +
                "ucz7ZO15m3qUhnn6Gz8xuv5hf8Kl5z4t6lPC8uF4mqC8bE97S5" +
                "PvYrI3hmddDGJsJ2Mo9q99nqILwHCjDmX3IRtSvrmF0Bs/851/" +
                "frkIOoZObXH13F0hGuqV+CmZbY2IDYaBiRBNUGOvPjdFVDbGyB" +
                "zrSRvQEfmGRZnbXq0Bldhs4UKKgte07GingKa2pLybbDB9Yxuw" +
                "kfGMXjDsu4nZrW1Lcxn2lheg/rMwOp3D9+5dwNM4rrx7KLOE/y" +
                "Zt5UIO9oLPedPAG/l82rA9+pbTSNNeIHmM8U6KwL+4itY/bYnZ" +
                "F5Y9pNBfsb3Wkw02/uX+5v1Bwb+8BMgc4G3Ef61mfuQzA7v4X7" +
                "rp5g31WkuhhS/cRsXX8EdX2bzEn5oyVGzMJL1s16Y/CBOUb11O" +
                "SPU6P/VPZbqIGaqcA3aUoW+fW1st+boDi8H6nOJuEDo5R2lIM8" +
                "Qw5iHbPnYGZV5JsrmVmirm+YaaxdzUNnCjS2qJUZasQKrOIQat" +
                "2zVA==");
            
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
            final int rows = 44;
            final int cols = 109;
            final int compressedBytes = 579;
            final int uncompressedBytes = 19185;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtml0rREEYx83aOTtTKC8lIilccCNXivIdFFF8A1HIhZSXC5" +
                "Hv4DPIRyCX3lnEtmzYRWzknVqzx7YWx55oznTG/p+ambNz5uyp" +
                "57f/5znPzmFbbJsEY9+M3KZ+Yn5ynTwTJveivzCP90Q7SMxHzP" +
                "6KRMmLGM+S659F2xctEIt5A8nZR3JkjpcW976LwdIY2xU+smVG" +
                "XuUwYztgJoGZ8Lr3wW6V9wmecpcp1NkhdCZFaaF3ZrTYeWa0CM" +
                "y009kxmGnH7ATMZBgtjDNjp4iN2mSzsEqd0Towk6CyMiZ8TSvM" +
                "4ypaTivZucWq2h+uLv3zfWtoNbzvaMZ7ZdEPnaXM76UcRxJj1O" +
                "L6hM4+zUFnDjNLjY2/Y8auwUzr58Yb5DOFsfEWsTFDdXYPnSmr" +
                "CB7hAxfW1E+oqREbLWPjM5hJiXsv+F//Pxon8IF7zGiK5zOjkX" +
                "vS6cxohs4yNZ9xBmYSol6OSmZ470pDneWBmQSd5SvdP2sBM+10" +
                "VgBmEnRWT9dJkK7SFbpB/XSZbjqqszUwk8CsIa4z36JZVft9C2" +
                "Cm3nxLv6jNZgWzVrxHrHs+4+2O5rM26EwuM97JOxAbNchnXcY4" +
                "CRpjap4bjVEwU0a2Gz5wl7HceJ89Z/bzX896JkWbsf8Wz5Ti31" +
                "FP5hJ736fmvdg/00hlIRLkfen3PLEX48J81W9zfgA+cm995mxs" +
                "5IPQmcR8NoR8Bp1ZMWMl8pjx4YzX2Qh0puFzyPgPdZdL6zOYDc" +
                "8J+EA7ZtPwgXss6w1BRCwu");
            
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
            final int rows = 44;
            final int cols = 109;
            final int compressedBytes = 494;
            final int uncompressedBytes = 19185;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtm81LAkEYxhtpZ5s9FQZF14gO/TUFBd2COtapP6C6ROStj2" +
                "uXDkFFt06dtIKOgRCEhnSoNPtA6MNS2CYTEZTW6N1hX/d5YHZ1" +
                "nFWYn8/7vu6MatV1RcZtkHipfybnRaH2yp1408d85fGlbulqf7" +
                "ZyfBLPoqTPudr4T91Sul3p95mr9RbFdeX82OSzX13IQ97MRJmG" +
                "mYqBGRUztW6I2RqYkTHbNMRsA8woZEW/Y6Patvr8Z2b1ghl9Pl" +
                "MTvvpsHMz+L7Vfz0zt+hwb98CM2mfqAMwC7rGdlkceMvn2DeL3" +
                "GbnPjuAzAq/FZUJk5Ik81lXdhYxbSV/rxnMwa5Q8/cPYLc3szL" +
                "4VGTv/u8/sexpm9g2YEfgsKZe0zxbNxEa5AGak9FLN+yPLusW8" +
                "r4+sYA4DxTONOWDHLIs5CG2tn0M+I3DQg0lm1iyYsfNZAczYMX" +
                "sHMwpV18+KWD9jk88+jOazETBjFxtLYMaOWRnMuDFzBJixY9YJ" +
                "ZibkWK3dI4ba1Gdd8Bk7ZgrMjMVHB3MQWp91w2dGPNaDGoQlty" +
                "jmILSxsR+xkcBBA1jzbEOqQ8hn7JgNg1mw8xn+f8aLmZq0E9iv" +
                "zyL2jWK/PkefOWNm6kYwo9HPHh5nCnt42ih2TqNuDKjPZuAzLu" +
                "r4AlHxNXQ=");
            
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
            final int rows = 44;
            final int cols = 109;
            final int compressedBytes = 468;
            final int uncompressedBytes = 19185;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtm18rREEYxneOHc6ZvaAoF8oVH4nP4Ea5Ea4ViuTPik8gpb" +
                "hxwRXFlfK/LKHFDWtDhIQax9ntRBurds7bzJnnrZkzZ5qr99fz" +
                "zDtnOlLyepaVUnTwRvkt2NOPtw/2EI6v2Ivf54Pxsd9Oi/PXQX" +
                "/H7tm7/8yF69/8duK3Myl5Qzj7yi6C560sCfYsERWF6JTSGUYe" +
                "9IovnZXMRaIz9xw6UxHuZYEZhTeKLjBT6IDdVb9kyhn4nzc6Q8" +
                "gipTd6kzTeKHrU6Uz0Ws5sioaZl1bIrM9eYsVavx+1fox2ukHU" +
                "+poRGeN7LMt3+Dbf54d8ix9EqrNd6EwBs3HK81lyFMxMO1OLCT" +
                "BTpLV0uRXJEWRJI17T2M+M5DYDncWQ6ixyYG0NMgdvNI7ZPJgp" +
                "cL0F0vuzDJgpYLYIZvDGP71xCcyMY7YMZiTuuYLv+lbrbBU6M4" +
                "7ZGpgZx2wdzEj2sw3sZ1brbBM6I9FZBjozjtkRmGlIJVduBe7P" +
                "tOJ1g3tqI7nlobMYUn1EDvSt9b32KL3Ra4M3mnY+SyXAjCJSDm" +
                "p9q3VWDZ0Zx6wGzEi8UcAbNSNSW/hn0G2l+GfQbYHOyMjWIQdG" +
                "8WqCN1pdgzTDGyuPxCe7xri1");
            
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
            final int rows = 44;
            final int cols = 109;
            final int compressedBytes = 406;
            final int uncompressedBytes = 19185;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtmrlKxFAYhb1giJBSwdIHEETBahoXsHEpfAJLG0urQbSxE1" +
                "dwaVyeRNGXENFhtBi3QUVQERWuMUhADNPM9TLHfD/cbAQC/8f5" +
                "z0lI1GGtKdtfZZ5+nH2Yx/T4yrzE22pyfBKvs+/r18n23jyY93" +
                "h/k97/Fq/TeJWsbS6lV1/NRbK/y3j2s6XqrqiLHsgx66YHcsx6" +
                "6EFjlT8/i3rxMzlmBZjJMeuDmRyzfpi5qKD1i1k0ELT/PbOgDW" +
                "YOEuGgT50FnTDzRnaIHuTWz4bRmRyzEZg5zCCjZBAZpxojg8gx" +
                "G/fKbApmcn42ATNvapykBw2YQabJIHLMijDDz7KYtZzDzJufzd" +
                "CD3ObGWXQmx2wOZg4zyDwZRMapFsJLUw6rtXUW3rphFlZg5o3s" +
                "Ij2QY7ZED3KbQZaZjQ4UtOL1nfoYZnI6W4WZHLM1mMkxW4eZHL" +
                "MNmDn8DrLJdxCZ3LjFPzz/lOw2Pcitn+2gM4d+toufyUy9PfyM" +
                "2VhzNu7DzOFsPGA2orNMnR3CTI7ZEczqr6ZP44Ma9A==");
            
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
            final int rows = 24;
            final int cols = 109;
            final int compressedBytes = 240;
            final int uncompressedBytes = 10465;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrt100LAVEYBWBvmVJj5WuWilKWpPxJ/8pGWVoJyUJISCHNqN" +
                "c1TRNNWGBymnPqfsztru7TWYyqlZepqt21HL2LHB6+LrIP9ws5" +
                "mXnt74dmjIPzpT9vZSeeWVfhfdeMkRkTVasQnp5l5q8bjUSOyr" +
                "xMYNajGVLs/rsb6Q5f6b9y61nk7Cc9swfs2Zd6NmfP2LOnPXPZ" +
                "Mzgzj2ZwZkozNLNsimZwZmmawZllaBZXsjm+QWJ7VmTP4MxKNI" +
                "Mzc2gGZ1amGZxZhWZwZlWawZnVaBbb/1mdb5DYnjXYMzizJs3g" +
                "zFo0gzNr0+zzpK67a8S4");
            
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

    protected static int lookupValue(int row, int col)
    {
        if (row <= 43)
            return value[row][col];
        else if (row >= 44 && row <= 87)
            return value1[row-44][col];
        else if (row >= 88 && row <= 131)
            return value2[row-88][col];
        else if (row >= 132 && row <= 175)
            return value3[row-132][col];
        else if (row >= 176 && row <= 219)
            return value4[row-176][col];
        else if (row >= 220 && row <= 263)
            return value5[row-220][col];
        else if (row >= 264 && row <= 307)
            return value6[row-264][col];
        else if (row >= 308 && row <= 351)
            return value7[row-308][col];
        else if (row >= 352 && row <= 395)
            return value8[row-352][col];
        else if (row >= 396)
            return value9[row-396][col];
        else
            throw new IllegalArgumentException("Unexpected location requested in value9 lookup");
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

        protected static final int[] rowmap = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 9, 0, 0, 0, 0, 0, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 11, 0, 12, 0, 0, 0, 0, 0, 0, 0, 0, 0, 13, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 14, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 17, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 18, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 19, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 21, 0, 0, 0, 0, 0, 22, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 23, 0, 0, 24, 25, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 26, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 27, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 28, 0, 0, 0, 29, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 30, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
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
            final int compressedBytes = 106;
            final int uncompressedBytes = 38989;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrt2cENACAIBDBHZ/NzAUN8Q7uBcgrRpFcHeImzAwCYE9RUTQ" +
                "EAwByOmgKAXomMybP9UXfG5VCe3Rtg1sJ9iPwAAAAAAOANFgAA" +
                "AAAA+OFPAQD0XAAAzJDWBQAAAAAAAAAAsNQF9dnovw==");
            
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
            final int compressedBytes = 79;
            final int uncompressedBytes = 38989;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrt3LEBADAIAjBP9/N6govVJXkANjYiAAAAAAAAAADgxmvkct" +
                "ZmHwAAAAAAAAAAAAAAAAAAAAAAAPjNvxYA9gsAAAAAAAAAAIYV" +
                "g0yh4Q==");
            
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
            final int rows = 977;
            final int cols = 9;
            final int compressedBytes = 68;
            final int uncompressedBytes = 35173;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrt1TEBAAAIwzCk4xwk8PAtUdCvVQAAAAAAAAAAAABAojl0eA" +
                "8AeDcAAAAAAAAAAAAAAAAAAAAAAAAAAMCDBRCzYSE=");
            
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
        if (row <= 1082)
            return sigmap[row][col];
        else if (row >= 1083 && row <= 2165)
            return sigmap1[row-1083][col];
        else if (row >= 2166)
            return sigmap2[row-2166][col];
        else
            throw new IllegalArgumentException("Unexpected location requested in sigmap2 lookup");
    }

    protected static int[][] value = null;

    protected static void valueInit()
    {
        try
        {
            final int rows = 31;
            final int cols = 129;
            final int compressedBytes = 172;
            final int uncompressedBytes = 15997;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrt2k0OgjAURtHuBkVFxD90AV1Xl84SlGm/kzdtGNyclKZpK+" +
                "3n1KEV0+uUv1YxwAADDDDAAAMMMMBAfSgVb2DZ+906aduZgVWp" +
                "eAMHpeINHJWKN3BWKt7ASal4A2+l4g3clIo3cFXKeUApBpSKNz" +
                "AqFW/gpVS8gVmpeAMfpeINfJVKN7B/vB/obh+4KBVv4K6UOyKl" +
                "4g08lYo3sCjlX6AUA0r1bKBsF5qO5g==");
            
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

    protected static int lookupValue(int row, int col)
    {
        return value[row][col];
    }

    static
    {
        sigmapInit();
        sigmap1Init();
        sigmap2Init();
        valueInit();
    }
    }

}
