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

        protected static final int[] rowmap = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 13, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 0, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 0, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 93, 117, 0, 118, 119, 120, 121, 122, 123, 124, 125, 126, 13, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 0, 139, 140, 86, 30, 1, 47, 105, 141, 142, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 136, 161, 162, 163, 164, 165, 166, 167, 17, 168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 1, 2, 0, 3, 13, 4, 106, 47, 155, 156, 157, 158, 5, 13, 6, 159, 178, 26, 179, 7, 8, 180, 160, 161, 0, 162, 181, 163, 182, 168, 9, 10, 97, 183, 184, 185, 11, 169, 186, 47, 12, 171, 13, 172, 187, 188, 189, 190, 191, 192, 47, 47, 14, 193, 194, 15, 0, 16, 195, 196, 197, 198, 199, 200, 17, 201, 18, 19, 202, 203, 0, 20, 21, 204, 1, 205, 206, 74, 2, 22, 207, 208, 209, 210, 211, 23, 24, 25, 26, 212, 213, 178, 180, 214, 215, 216, 217, 27, 74, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 74, 228, 28, 229, 230, 231, 232, 233, 234, 235, 236, 237, 86, 105, 238, 29, 239, 240, 30, 241, 3, 242, 243, 244, 31, 245, 0, 1, 2, 246, 247, 248, 47, 32, 249, 250, 86, 251, 182, 179, 187, 252, 181, 4, 5, 96, 6, 253, 33, 34, 254, 149, 13, 186, 188, 189, 190, 191, 255, 192, 193, 256, 105, 257, 194, 258, 259, 260, 196, 105, 261, 262, 106, 107, 108, 112, 263, 115, 120, 122, 264, 183, 197, 265, 266, 267, 200, 201, 268, 269, 106, 270, 271, 272, 273, 7, 274, 8, 275, 9, 276, 10, 277, 11, 0, 35, 36, 37, 1, 12, 0, 13, 14, 15, 16, 17, 2, 13, 3, 18, 19, 20, 278, 4, 279, 280, 281, 21, 38, 23, 39, 24, 177, 40, 41, 27, 282, 283, 284, 285, 286, 287, 288, 28, 289, 290, 291, 292, 293, 294, 295, 296, 297, 298, 299, 300, 301, 302, 303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313, 314, 315, 316, 317, 1, 29, 30, 318, 0, 319, 320, 31, 32, 33, 34, 42, 321, 36, 38, 322, 323, 324, 325, 43, 44, 45, 46, 326, 39, 40, 41, 5, 47, 48, 327, 49, 50, 51, 52, 53, 54, 7, 328, 329, 55, 330, 0, 56, 331, 57, 58, 59, 60, 61, 332, 62, 63, 333, 64, 65, 66, 67, 334, 68, 69, 70, 335, 71, 336, 72, 73, 74, 75, 8, 337, 338, 339, 340, 76, 9, 341, 342, 343, 344, 345, 346, 347, 348, 77, 78, 10, 79, 80, 81, 349, 82, 11, 83, 84, 85, 350, 351, 87, 88, 89, 0, 352, 90, 91, 12, 92, 93, 94, 47, 13, 353, 13, 354, 95, 96, 355, 14, 98, 99, 100, 15, 101, 102, 356, 357, 358, 359, 103, 104, 105, 21, 107, 16, 108, 360, 17, 109, 110, 361, 17, 362, 363, 364, 111, 3, 365, 4, 48, 112, 5, 366, 113, 367, 368, 6, 114, 369, 370, 371, 115, 18, 372, 373, 49, 374, 375, 116, 117, 50, 0, 118, 119, 120, 121, 122, 376, 123, 19, 51, 52, 377, 378, 379, 124, 20, 380, 381, 125, 126, 127, 382, 128, 53, 129, 98, 130, 383, 384, 385, 386, 387, 1, 388, 389, 390, 391, 392, 393, 155, 131, 132, 133, 134, 22, 13, 135, 394, 395, 396, 397, 398, 399, 136, 54, 400, 401, 137, 402, 403, 404, 138, 405, 406, 407, 408, 139, 409, 2, 410, 411, 106, 140, 412, 413, 414, 415, 416, 417, 418, 141, 419, 420, 421, 142, 143, 422, 423, 424, 156, 144, 425, 426, 427, 428, 198, 203, 429, 204, 30, 145, 146, 147, 148, 23, 430, 149, 431, 17, 199, 24, 432, 150, 433, 206, 434, 207, 435, 436, 151, 437, 209, 438, 439, 0, 152, 55, 56, 124, 440, 126, 441, 153, 154, 442, 443, 17, 212, 444, 155, 445, 7, 22, 57, 25, 27, 446, 28, 447, 448, 449, 35, 153, 450, 37, 217, 66, 0, 3, 451, 452, 1, 2, 453, 454, 455, 456, 457, 458, 459, 460, 461, 462, 463, 464, 465, 25, 27, 28, 466, 467, 468, 469, 470, 471, 472, 473, 474, 475, 476, 477, 478, 479, 480, 159, 481, 482, 483, 484, 485, 486, 487, 488, 489, 43, 490, 44, 491, 492, 493, 45, 494, 495, 496, 497, 498, 499, 500, 501, 502, 503, 504, 505, 506, 507, 508, 7, 509, 222, 3, 510, 227, 511, 512, 513, 514, 515, 516, 46, 517, 518, 519, 520, 521, 522, 523, 55, 56, 66, 68, 524, 525, 526, 527, 528, 69, 529, 530, 531, 532, 533, 534, 535, 536, 537, 538, 539, 540, 541, 542, 543, 544, 545, 546, 547, 548, 549, 550, 551, 552, 553, 554, 555, 556, 557, 558, 559, 560, 561, 562, 563, 564, 565, 566, 567, 568, 70, 569, 166, 570, 571, 572, 71, 208, 573, 574, 575, 216, 576, 84, 577, 85, 86, 578, 579, 156, 160, 580, 581, 161, 162, 582, 163, 583, 584, 87, 90, 97, 106, 58, 585, 109, 110, 586, 587, 4, 588, 157, 589, 590, 164, 591, 592, 593, 594, 595, 596, 597, 5, 598, 599, 600, 601, 602, 6, 603, 8, 9, 10, 11, 12, 13, 604, 605, 606, 607, 608, 111, 609, 116, 610, 117, 228, 125, 611, 165, 612, 166, 613, 614, 126, 615, 616, 617, 618, 14, 30, 619, 620, 621, 167, 622, 623, 169, 624, 625, 626, 627, 628, 629, 235, 630, 127, 631, 632, 633, 634, 635, 636, 637, 638, 639, 128, 640, 641, 642, 643, 129, 644, 645, 135, 646, 647, 648, 8, 649, 650, 651, 652, 653, 654, 655, 656, 657, 658, 659, 170, 171, 660, 172, 661, 127, 662, 173, 16, 663, 664, 665, 666, 667, 668, 669, 670, 136, 137, 671, 672, 673, 138, 674, 139, 140, 141, 142, 675, 174, 143, 59, 4, 144, 149, 676, 677, 9, 678, 679, 680, 681, 682, 683, 684, 685, 686, 687, 688, 150, 17, 151, 152, 689, 153, 154, 175, 1, 159, 60, 164, 167, 168, 170, 172, 61, 174, 176, 177, 178, 179, 181, 183, 184, 690, 691, 188, 692, 693, 0, 694, 47, 31, 695, 696, 697, 155, 158, 169, 185, 192, 195, 62, 202, 63, 237, 698, 18, 699, 171, 190, 193, 194, 196, 197, 203, 700, 701, 702, 204, 703, 704, 206, 207, 209, 210, 211, 64, 705, 706, 707, 708, 709, 710, 711, 212, 10, 213, 19, 20, 712, 713, 176, 177, 714, 179, 715, 716, 717, 718, 719, 33, 214, 65, 720, 721, 722, 723, 215, 217, 5, 724, 725, 726, 727, 728, 729, 238, 730, 218, 219, 68, 731, 240, 732, 733, 734, 735, 220, 7, 221, 222, 223, 224, 736, 737, 738, 225, 226, 227, 739, 228, 69, 180, 229, 230, 231, 232, 740, 233, 234, 235, 741, 236, 237, 238, 742, 8, 239, 240, 241, 181, 182, 70, 183, 184, 743, 71, 128, 74, 75, 76, 77, 744, 745, 245, 746, 186, 747, 242, 243, 244, 748, 749, 189, 193, 750, 751, 752, 194, 753, 754, 21, 755, 23, 195, 756, 196, 757, 758, 759, 760, 78, 245, 246, 761, 79, 30, 24, 80, 81, 30, 31, 82, 83, 32, 34, 762, 247, 248, 249, 763, 764, 197, 765, 250, 766, 198, 767, 74, 199, 768, 84, 251, 252, 34, 35, 254, 255, 2, 256, 36, 257, 769, 258, 770, 771, 772, 1, 773, 248, 774, 259, 36, 775, 93, 37, 260, 261, 38, 250, 96, 200, 776, 39, 777, 201, 778, 779, 202, 253, 254, 780, 781, 782, 783, 262, 263, 264, 203, 784, 785, 206, 257, 261, 786, 207, 787, 788, 208, 789, 790, 211, 85, 265, 266, 267, 38, 268, 269, 0, 212, 270, 271, 791, 792, 793, 272, 273, 274, 276, 277, 278, 279, 40, 0, 286, 290, 1, 287, 292, 2, 293, 299, 39, 306, 308, 310, 41, 312, 86, 2, 42, 313, 318, 321, 322, 324, 325, 326, 40, 327, 329, 331, 332, 333, 334, 335, 336, 338, 339, 340, 341, 342, 343, 344, 345, 346, 349, 350, 1, 213, 351, 352, 354, 355, 357, 358, 359, 360, 361, 362, 363, 364, 366, 369, 41, 48, 53, 54, 55, 56, 68, 69, 72, 73, 74, 75, 76, 77, 794, 795, 43, 78, 796, 797, 798, 799, 800, 801, 802, 803, 804, 805, 330, 370, 806, 807, 808, 809, 810, 811, 812, 813, 814, 815, 816, 817, 818, 819, 820, 371, 372, 821, 373, 374, 214, 353, 376, 377, 378, 379, 381, 382, 383, 384, 385, 386, 2, 822, 387, 388, 389, 390, 391, 393, 823, 394, 824, 825, 392, 396, 826, 827, 395, 399, 828, 400, 397, 829, 215, 0, 830, 398, 401, 831, 832, 402, 833, 86, 834, 835, 836, 218, 219, 404, 403, 220, 405, 260, 406, 407, 837, 838, 408, 409, 410, 411, 412, 413, 221, 839, 414, 415, 416, 417, 418, 419, 420, 105, 421, 422, 44, 424, 840, 841, 88, 425, 427, 431, 3, 222, 423, 426, 432, 429, 4, 434, 842, 435, 436, 223, 224, 437, 438, 439, 430, 440, 843, 441, 844, 442, 443, 444, 445, 446, 447, 448, 449, 845, 225, 450, 451, 452, 453, 454, 455, 456, 457, 458, 459, 460, 461, 462, 463, 846, 226, 847, 848, 45, 464, 89, 465, 466, 91, 467, 468, 469, 470, 849, 850, 851, 471, 852, 472, 473, 474, 475, 853, 476, 854, 229, 855, 477, 478, 479, 856, 857, 858, 859, 230, 480, 481, 482, 483, 484, 3, 92, 93, 485, 860, 486, 861, 862, 863, 1, 4, 487, 488, 94, 87, 489, 490, 491, 88, 492, 864, 493, 494, 495, 496, 497, 498, 499, 89, 500, 502, 262, 501, 503, 263, 504, 505, 865, 506, 507, 5, 866, 867, 106, 46, 868, 869, 508, 509, 510, 870, 231, 871, 872, 232, 512, 873, 233, 3, 874, 875, 234, 513, 515, 876, 877, 518, 520, 878, 879, 522, 880, 881, 882, 517, 519, 11, 883, 884, 885, 886, 521, 887, 95, 528, 523, 527, 888, 525, 526, 96, 98, 100, 889, 235, 890, 529, 535, 264, 891, 536, 90, 892, 893, 894, 895, 236, 239, 241, 91, 242, 896, 897, 898, 548, 899, 4, 900, 901, 902, 903, 904, 92, 905, 101, 906, 907, 908, 549, 909, 5, 910, 911, 546, 912, 913, 93, 7, 914, 915, 916, 103, 917, 918, 919, 920, 243, 921, 94, 95, 922, 244, 923, 246, 924, 559, 530, 531, 925, 926, 927, 928, 561, 929, 930, 104, 931, 0, 932, 933, 934, 105, 96, 100, 101, 107, 108, 114, 935, 115, 118, 119, 120, 936, 937, 103, 938, 939, 940, 107, 47, 941, 48, 5, 532, 562, 49, 121, 545, 553, 107, 533, 534, 108, 537, 50, 942, 943, 271, 944, 563, 538, 539, 540, 541, 542, 543, 274, 945, 122, 946, 947, 275, 247, 277, 555, 564, 565, 253, 269, 566, 948, 317, 949, 249, 950, 319, 251, 320, 951, 567, 952, 568, 569, 953, 570, 954, 571, 572, 573, 574, 575, 576, 577, 955, 252, 956, 255, 256, 957, 958, 959, 51, 544, 960, 961, 962, 963, 0, 964, 965, 966, 967, 968, 969, 578, 970, 547, 551, 552, 556, 971, 972, 973, 123, 974, 975, 557, 976, 977, 978, 979, 980, 981, 982, 983, 984, 579, 580, 581, 985, 560, 986, 583, 987, 582, 585, 988, 989, 990, 588, 584, 6, 7, 589, 587, 591, 592, 991, 259, 992, 993, 994, 265, 593, 995, 267, 996, 266, 997, 998, 594, 590, 999, 1000, 1001, 108, 595, 596, 597, 598, 599, 600, 2, 1002, 1003, 1004, 109, 52, 601, 602, 604, 605, 53, 606, 1005, 607, 610, 1006, 612, 1007, 1008, 54, 608, 1009, 268, 609, 1010, 1011, 611, 1012, 1013, 1014, 1015, 1016, 1017, 1018, 1019, 1020, 270, 613, 1021, 1022, 1023, 1024, 615, 616, 1025, 1026, 617, 618, 124, 619, 620, 1027, 1028, 1029, 621, 622, 1030, 0, 1031, 1032, 1033, 8, 125, 130, 614, 623, 1034, 1035, 624, 131, 1036, 625, 1037, 626, 132, 1038, 1, 1039, 1040, 627, 628, 629, 1041, 630, 271, 1042, 1043, 631, 632, 633, 1044, 133, 134, 1045, 276, 321, 1046, 634, 1047, 639, 1048, 635, 1049, 1050, 644, 636, 637, 1051, 1052, 1053, 645, 638, 111, 9, 640, 641, 12, 1054, 642, 10, 1055, 1056, 1057, 1058, 277, 1059, 643, 145, 1060, 278, 1061, 280, 646, 1062, 647, 1063, 281, 648, 286, 287, 1064, 290, 146, 147, 148, 649, 55, 650, 1065, 1066, 1067, 1068, 1069, 1070, 1071, 651, 1072, 652, 1073, 653, 293, 654, 292, 655, 1074, 656, 113, 1075, 1076, 11, 657, 658, 659, 660, 661, 1077, 1078, 662, 1079, 663, 664, 323, 665, 114, 1080, 1081, 12, 1082, 666, 667, 299, 1083, 324, 1084, 668, 1085, 1086, 153, 1087, 154, 1088, 156, 280, 669, 671, 1, 1089, 325, 1090, 1091, 115, 1092, 116, 1093, 326, 1094, 327, 1095, 157, 674, 1096, 9, 1097, 676, 677, 1098, 678, 1099, 159, 322, 679, 680, 681, 688, 691, 692, 110, 56, 3, 4, 682, 683, 1100, 111, 57, 328, 1101, 329, 327, 1102, 330, 117, 1103, 118, 1104, 119, 332, 686, 1105, 338, 331, 1106, 160, 1107, 1108, 689, 1109, 1110, 694, 690, 161, 58, 693, 162, 695, 696, 59, 697, 164, 698, 699, 122, 700, 701, 702, 1111, 703, 704, 705, 333, 1112, 707, 1113, 13, 14, 710, 15, 1114, 709, 1115, 711, 1116, 1117, 1118, 712, 16, 713, 17, 1119, 714, 715, 1120, 167, 718, 1121, 1122, 716, 719, 1123, 717, 335, 720, 721, 267, 722, 723, 1124, 1125, 1126, 724, 725, 726, 727, 2, 112, 60, 123, 728, 729, 730, 1127, 1128, 1129, 1130, 1131, 1132, 731, 732, 1133, 733, 734, 1134, 334, 61, 62, 735, 736, 63, 1135, 281, 124, 125, 0, 126, 127, 737, 336, 1136, 1137, 1138, 168, 738, 740, 742, 1139, 744, 170, 745, 1140, 1141, 746, 1142, 747, 748, 749, 750, 751, 752, 753, 754, 755, 756, 1143, 1144, 337, 341, 757, 758, 759, 341, 760, 8, 172, 761, 9, 10, 762, 1145, 763, 764, 1146, 765, 1147, 766, 1148, 128, 767, 768, 1149, 173, 129, 1150, 1151, 1152, 339, 1153, 1154, 1155, 1156, 340, 346, 769, 349, 1157, 770, 773, 1158, 129, 1159, 1160, 774, 1161, 18, 350, 131, 1162, 1163, 775, 776, 777, 11, 1164, 1165, 1166, 19, 355, 134, 1167, 778, 779, 1168, 352, 174, 175, 176, 2, 354, 356, 1169, 357, 1170, 1171, 358, 1172, 1173, 135, 1174, 136, 1175, 1176, 771, 772, 780, 781, 782, 783, 784, 1177, 1178, 787, 1179, 788, 1180, 114, 64, 785, 1181, 1182, 1183, 282, 177, 786, 1184, 283, 115, 359, 65, 284, 1185, 789, 132, 1186, 133, 1187, 1188, 1189, 1190, 137, 1191, 790, 791, 360, 1192, 1193, 1194, 1195, 1196, 5, 13, 1197, 1198, 1199, 1200, 794, 1201, 1202, 1203, 801, 1204, 1205, 1206, 1207, 1208, 1209, 806, 819, 1210, 827, 361, 10, 825, 11, 12, 1211, 1212, 826, 828, 829, 20, 21, 178, 831, 1213, 179, 1214, 66, 830, 832, 833, 835, 836, 1215, 837, 838, 1216, 839, 841, 842, 1217, 1218, 1219, 285, 12, 181, 183, 1220, 843, 844, 845, 13, 846, 847, 849, 1221, 342, 1222, 362, 363, 13, 1223, 14, 1224, 1225, 850, 1226, 848, 851, 852, 853, 184, 1227, 364, 67, 856, 1228, 1229, 138, 1230, 854, 15, 1231, 22, 857, 139, 1232, 1233, 1234, 1235, 1236, 366, 859, 16, 1237, 140, 369, 1238, 1239, 1240, 1241, 1242, 372, 861, 1243, 370, 1244, 371, 373, 1245, 1246, 374, 1247, 1248, 1249, 141, 142, 343, 1250, 1251, 344, 858, 14, 862, 185, 864, 1252, 68, 7, 8, 863, 865, 867, 868, 376, 869, 288, 1253, 1254, 377, 1255, 1256, 187, 188, 191, 1257, 1258, 192, 69, 871, 872, 1259, 0, 195, 873, 874, 1260, 1261, 875, 876, 1262, 1263, 1264, 1265, 879, 882, 883, 1266, 1267, 1268, 1269, 15, 884, 1270, 1271, 886, 878, 880, 1272, 1273, 1274, 887, 888, 889, 1275, 289, 200, 201, 890, 1276, 1277, 892, 894, 895, 897, 1278, 899, 1279, 893, 345, 1280, 1281, 903, 1282, 910, 1283, 1284, 1285, 378, 131, 1286, 1287, 1288, 23, 379, 1289, 1290, 1291, 1292, 380, 381, 900, 382, 1293, 1294, 914, 1295, 1296, 1297, 1298, 384, 386, 901, 385, 1299, 1300, 202, 134, 1301, 1302, 904, 902, 905, 906, 907, 909, 911, 1303, 1304, 1305, 1306, 291, 294, 295, 1307, 70, 387, 388, 296, 205, 206, 389, 390, 391, 207, 1308, 1309, 1310, 143, 912, 1311, 1312, 1313, 1314, 1315, 913, 1316, 1317, 916, 16, 917, 918, 919, 921, 1318, 915, 925, 1319, 922, 393, 1320, 1321, 1322, 923, 924, 926, 394, 927, 929, 930, 931, 932, 405, 1323, 1324, 409, 410, 933, 403, 1325, 1326, 144, 1327, 934, 411, 935, 404, 1328, 1329, 147, 1330, 406, 937, 1331, 346, 938, 939, 940, 941, 428, 1332, 297, 298, 1333, 433, 30, 1334, 149, 150, 1335, 1336, 437, 942, 1337, 1, 1, 936, 943, 944, 1338, 945, 946, 947, 1339, 1340, 1341, 948, 949, 1342, 950, 951, 952, 347, 1343, 953, 1344, 1345, 438, 1346, 1347, 151, 1348, 1349, 24, 1350, 152, 1351, 1352, 25, 135, 954, 348, 300, 301, 302, 441, 442, 1353, 155, 156, 158, 1354, 1355, 1356, 1357, 209, 159, 1358, 955, 1359, 956, 1360, 1361, 1362, 957, 960, 961, 962, 963, 964, 958, 210, 965, 1363, 1364, 27, 446, 1365, 1366, 28, 447, 1367, 959, 303, 1368, 304, 1369, 1370, 1371, 211, 214, 966, 15, 227, 251, 1372, 252, 1373, 967, 1374, 968, 970, 969, 448, 1375, 1376, 449, 450, 1377, 1378, 451, 452, 253, 254, 255, 453, 454, 256, 972, 974, 975, 257, 258, 1379, 455, 1380, 1381, 457, 1382, 305, 456, 458, 459, 1383, 1384, 977, 978, 980, 1385, 1386, 1387, 1388, 1389 };
    protected static final int[] columnmap = { 0, 1, 2, 2, 3, 4, 2, 5, 0, 6, 2, 7, 8, 9, 5, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 1, 20, 2, 21, 7, 22, 23, 24, 5, 5, 2, 25, 0, 26, 27, 28, 29, 7, 18, 6, 30, 31, 0, 32, 16, 0, 33, 23, 34, 0, 3, 12, 19, 35, 28, 36, 37, 38, 39, 40, 41, 0, 42, 43, 36, 44, 45, 39, 40, 1, 46, 47, 10, 48, 41, 49, 50, 45, 46, 35, 51, 51, 52, 53, 5, 54, 55, 0, 56, 57, 58, 3, 59, 3, 60, 61, 62, 16, 42, 62, 63, 64, 63, 65, 66, 67, 68, 69, 70, 64, 71, 65, 66, 45, 72, 67, 73, 74, 0, 75, 0, 76, 73, 77, 78, 79, 80, 80, 4, 81, 0, 82, 83, 2, 84, 1, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 19, 81, 86, 96, 97, 98, 5, 90, 99, 100, 96, 101, 97, 5, 40, 2, 16, 0, 102, 40, 103, 100, 1, 104, 63, 6, 103, 105, 106, 107, 108, 0, 5, 109, 110, 111, 106, 112, 113, 114, 12, 115, 6, 116, 6, 117, 118, 119, 120, 121, 122, 123, 124, 0, 125, 1, 126, 45, 127, 128, 129, 130, 0, 129, 131, 0, 132, 133, 113, 134, 135, 136, 116, 2, 137, 62, 138, 139, 140, 141, 2, 142, 3, 143, 119, 0, 62, 120, 144, 145, 124, 7, 3, 146, 28, 0, 147 };

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
            final int compressedBytes = 2978;
            final int uncompressedBytes = 38977;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrlXc2LHMcVf9UurcvDBrfNKNEtvYoFg+KADzrk2IIl2CYma8" +
                "jBl4AOMeiSo+8jE8JGvsjKopPBOIccc0hA5xUIH4IPMc7RIKO/" +
                "JP0xPdMfVfV7Va9rZ2W10Nf+pr5e1Xvv915V9Xzzk4c3Pzs+04" +
                "vs/NFH6tubP1x9ph8cvfnJ8Z3lv14/0etvPqjw35+9VeGPR/hj" +
                "Fl7X/7+zt9+v8aNvbz6/+mw5qF+K/8j7R0T17/6j+/8Rtw/Knx" +
                "8+vJ6dV+uD6KcfFU8WP6j7y3WRvXFMlFM+w/jnkL+9/j6ebH1/" +
                "/7t///Ozp2e/+s+bTx5/+Ivvfvn8Z8+uPfjq0SdPK/zLD9PLp5" +
                "mf8uzw/Xp+6Mniubqvg+Zn3/Mnrf+y92/P9oUMZX2bkdX/3liU" +
                "vLYficc3MFbFttn2KdPbrx+9f0mM0/gph/8Vy+dFl6/Uvw/0M+" +
                "v8ez7w78i/tfzNjadc33PMHxpfQvuwrIR+2glbVb/01Ur+66Ke" +
                "AsWxTxz8uOnfo32MT1dDMh2JfKX6fbA8bC1xTvduy8e34R8tP6" +
                "SGHw74x/cf/G3Dj84fn2z50cOm/1t+tIk/Ttrx0aVav1L/O+Xv" +
                "4+dTH79HOOLnCH/a4i1/LBr+uMNP5Lg0fpDWH1L+pC2fh/QPzC" +
                "1J7ZuU33flXfp5Ue27yvfxkxanPo74qxQf9v+7tv9ftf3/OYc/" +
                "g/Kh9VOvfoobH805Pivp2wH0tG//x/rH4Zem8OoPih+s/bM8Jh" +
                "iwyX/Kz/aNW+d3IN/Z26f4/jn5kebwWys/AP4X8bv+oJpPjJYc" +
                "Kh+BU0j5jTqrnURV6v5N+a+Ev/bK2+bv8DVa/FVXTWbna1J5Qc" +
                "s/6Pwo/yPdWf7j9bKejAl/NX3+WuEHPn6LcLR+pPUfvucc39/r" +
                "8SH+LOXfffykxYf+FcT3GC8o6BmXVyQr3/qPbPrBXb8yl6Vf++" +
                "x/AfyDD9c9vPCXD5X/9kP50IEZrfrls0H7+c6XqpHwTVFSVpuX" +
                "1qzokg7qvxdNvRl0ydXH1ODH5SR+Ea0vbZQypp4q1erXb2r919" +
                "Wg3mn0i+G/2Y8KKl/wxg/K4/EJ9Yutv9o7v/lkfIaJg/qhfgvl" +
                "N6hIRdgXkumvIXIpkls/O/uhOOOnwfgPbOvHpv9d/ah9uP4KxN" +
                "/NgF0tN8Zpx29E8pfOD446wvpn1/8stv9ofuPsow5dvyNcx8t3" +
                "Wj+yLzSyr7YnS2Yf4foC+rEoLMLvhfjG1X4x5CnOvhZh6jm3fU" +
                "X2Adl/pD9z89PS4eA7Pz7hT375MPK/n0ryv9L86Y1rf1JmQdnn" +
                "5hVFX1aFvr5lSqNXGf3a3HuviOMXOZ8mpd5/Q/nNPm6Lfy6qf6" +
                "78ZyN/1pp28BchP7zs+c+I8hRWvuDYFx1NGOD8mGH4MbI/wf0P" +
                "nH+Uv0X+hbV/I8kP8+J//MA4UIHptOPy/Gvg/E0y8H7/Jc2/r0" +
                "H8xPB/ofnVoPxoRPuB+auo+eHzMyk/n0c/nPllS/7Zxr07XFnK" +
                "7xVPvT/At199/cnY8yPILzf5c5Rft+A0yr+j/Lc3Py/NzzHy/0" +
                "nx8P5dvej2DyT7I0g/0P6FBac+Lt6fEe5/9BeasfJn8ue/gfzR" +
                "/mXdaJNhqy3NJL9xzvEPKtw/9UyUWW0MjlL9/E3GLI/9K2o/Jn" +
                "2oGS6rYMb30vxATP4H9z9jj5+LK2H5WPlPuWI+q/xkOIMf+vu3" +
                "amjAf5dPblVr4+3z10r6eKVX2Rv3TaXX55z8rD8/L90/teapdP" +
                "z6x/53YJ9nz8+Nnm3+qm7ckr+6ce3jFtcb/C8tTmOc7Lg2N6jC" +
                "6V49P69WDVb4Zn7Mxv+Wfdq38w88/wHrr8ef+cbv7z/qH5IP6j" +
                "/OL/r7N8G/DpufCm/k93k9Rl/9kePr6r83qf8d5vo58s4vXz6y" +
                "8aWZf+b687Q/lc9yql8ZR7+ugP6pKP1G9mW3f6N6HlZbt2TsRO" +
                "2OzD+vNvtj62WZlaaspPHnTJvKYrxbc25scMf7L+WYnwF82/9Y" +
                "/ywcP90dlh/zVzG/5PJXu0/F++8gvjgFOK2A/1yF+ddQHO7Ppe" +
                "VveF6F6wvuL6P6ue1L8TTyQ+sX57cAfzSdiNXu/AeNz39YlDhX" +
                "LH4o7V81f3VDWd4Mvi8/E4irOHx7/sGVH0DtF5uYy1n/eH079N" +
                "e5vlntu5+7w/Jb+80dvzQ/n3j9IPtuta8a2N/Lo//4fAc4v7Nq" +
                "8l8n+fpW3dqDira8VVT8JV/45jfA/sP8cjHO4V8wThHltRu/4P" +
                "Wx//zMPvo/o/2A519j2g+cXzOj/Uqdv03Nf0Lzu6aprRLElaF9" +
                "q6xaOWk64GDU7PFJ6BMqf0f8dWHzb2+fEX/545dTup1b1/+q2/" +
                "8xzUtMsjrkz4t6inPKj+mOvn7A63/Df4bnMwbyAzg56i8uRv4g" +
                "/2Cof0Yk71dXuOdPEH9SuP4C/sLkN7H8lDj+3ZN/AeWhfdvu/5" +
                "FnfWVs+2Kfv8wZP4DxwfOpcPxFWP/D5z/CP/L1D92v3CVCh8+a" +
                "Of+Qnyzs/V/PxI8C/K9Kw38Sx1/J97cD+Uvg/GP+mnZ8qfNPe8" +
                "8PEMt+hjwa+0+b/XWUx/bvtmR/EK+f0flVtv3d8jNhfAbP/5SD" +
                "/NrG/2dd3t1Z/rQjjU2ZurZ3d+f/8gj7pYT4PPZnMnNzxceh/r" +
                "ebf1R+v/qN7qfg87lF0vUN7xfg/Cd6fw3Qr8D5KyXysY3P+PkH" +
                "hx/2I/+xfYAK5+8/vJ9hLPdru/0VRnn0fgjp/Q7Uv9TrO46/CN" +
                "dXqH0LXz/5/PlJR3n8/kqQn5C9vwb1j3F+/9omfzMW1vV2cRRq" +
                "wD/s+RkfnjT/jvdPhfPXSS7+fod//qX3p/H9c9H9D0L3O+D6T+" +
                "u/pfc/+PfLdHj/lTw+nf/+ReD9k7nPxwTyT3S6H80/nt8jWf+s" +
                "HdWW7KEdP8p/qyq83v9X9EXV67Kg0tQJ5CrqH7Qdd/+Snx/2l4" +
                "9/vw3J5ceKzzXQL3v9/PPbKqD+ABzsfzP9E3q/R/T+uzh/kTo/" +
                "Et0+Mz8Sxe/mK5+Kl8fs/2jLktq7fF4aXMf5V7b9fNnlK/OPyc" +
                "rvu//S/b/9z6+Mn1xyHNnPo/x4wy+ruaj5JdX8ssk6VX9ej4kf" +
                "XPfbovKDqP7J/ZPp/RpRftx1fydz1z+wn/D+D+D//vs7GbH2X2" +
                "T8KlR/dZD+jxfH2h4fefH+48Bd5fn3s8byH44P4877WaxEgnJ9" +
                "BNzvenFwLcSvvIQ4X//R/UDP+g3lhwOcdT+YNb648aP7jTPIx4" +
                "vP55+uRMWnKD6YIb5m+gcUnzDxaP6K8j+R+SMmv8Tv906U35rn" +
                "/bDx8SF1+UF0v2q2/F/Y/MPxF0AEfpyvX2nax/XflcZ/fvskzU" +
                "9vO+A6f4tw//5J3PtN+P1vzuctBPebrfFBgP1G95sZ+VHt87/j" +
                "+m3xp71/Xf5OpYxvpf4Ly7fk5O+j/Yc4f8p+v54KkL/t/YqR+x" +
                "v7wnOefCLsN/LPYTjmN37/PlN+0n0/mxe/BvinUXn//W+8vu/6" +
                "718n9m+X4vuHM45+xO3/yPevCpn9kt4vmm1/ZU+4ND6Qxk8YTx" +
                "0fyuKb2fbPkslvpvXhsE+z7T/H1l8QSd4P0GPIcfotbF94fmAe" +
                "++r+fjJx+cT3h6T14+8f9JyfLmd6f4LHv+Lvr0ge3ySvf3q/Xz" +
                "f3+x07OLb4wY1v6x/Zd7V5f4CnfRbOa39qXxXNM77Z5OMaX8Ex" +
                "3qh9d/6AHYCJziemO/9mZP5jBv1B/sOLN++HNJStjVK275divN" +
                "9BVH9c/kqHL5/49eOXL3HzGy6c4R+CCEu0/kX2/4LxF73/c+Oh" +
                "/DH0/ZE4/orQj/pZ9/xPVb7YlVfNj087Xgne/7Lv/IN0f4KLvy" +
                "D6NSqP75fFyDckvubu76TaX0pbXrw/CPVv5eB37PxXGec/HQ04" +
                "5UOB+sPFjXV9Z+z8SmHBR/kNz/4W/v5IIb/l6V8G9CMT2UdWfl" +
                "ntJX+cPP8gPT/5f1IiauM=");
            
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
            final int compressedBytes = 2866;
            final int uncompressedBytes = 38977;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtXb2rHccVnxmP5PHlBa3NlXCXecaCi3DhwkXKffAItkhAhQ" +
                "s3gReIQE3K9CsRguI0snmkCoSkyB+QQvUVBFcqLJxeQn9Jdvd+" +
                "7u7M/M7Mmbn3ErSFnqTz5nvmfPzmnDPCiFooKSqhRffpWtzsfs" +
                "6Eaf9Uov8R+F7++vt7f/n6+tOZWj7/Rr669+b2a/3s/KM/XF7N" +
                "n996oBt/SRuoVftJdWT7oP8Cjz9M334S9B/RB7+QbXwvf9aW/+" +
                "/1Z/e78uev7r29/Xq+Lv/vrnxpOlpcenn2/Lr3T9b+/wTmZ0on" +
                "7E+1a1Nt2q9y7a9dtdz5TaQz+Y+//FP3+Wwc/VMuuoc/Nb7+31" +
                "i1v+za1237C2L7ZvND7u9Ptc8nVXAGunKqEu+N5ndBXZ91eekr" +
                "H64f80de/evPW348//Vk/lH9gG6G9HpFV6P2xWT8EeePdb5A/+" +
                "D+sSLqq2PLA/rMOvirFg25/c2ZIv5uHTs+Jp3L34R4ElSOYHnL" +
                "0g9I+9MpP2wW/r88+/4TtbzW7Xm68419MXsjv503Vn142bGEqu" +
                "ufLTr+vv36+qxvX/Tt60H7kP/x2ietjwqwl4H+sJ78JkY9iOn/" +
                "Wr/5x6p/P39AWB/UPzC/sH14PsxwAibyl8ef/7PaP+79u1o/OS" +
                "hfU5lwRRUvqH5i+0B8nWj7aecnn3673F9/B/+A9kHK+Yvg33h+" +
                "2PLDNHp7oPYr+4TEX2LxBdL50QT8ganXsOrH7SvW+CL6Z8SFm/" +
                "8uAvJN+/WTeqK/JOIvCzALDUk/mreVPN0dESn07bahxvYWR03R" +
                "r6j4lUze303SBrSBX9OgGk0Yox0ObiuOpB7v02ZUYr277OB/DR" +
                "LPsZ8lzm9p/cyCnSJvms0cdSbczfnZqvOVeHyB6cJA/Sq4/86+" +
                "ErM/63ZF1LIRsrJi/htdnVe/E1fzf96q93CJKP1QF6bH4KM8Pl" +
                "58fbjylWl/8O1TND7jlM90/dqIqX6oo/XzarJ/TTZ8s5+BxgHJ" +
                "imUEvuKrv0b2c1A+ayOFMd3+lJv92f5Lt536fLV/Ab38+S6ML+" +
                "eRbxhrCtVPwkeI9Tep81O6fump/1D7J1X/IdoP0XqIPdD4j32+" +
                "UuzznO3XRxjfO/rJ0G2/A37ULz5v//LZsj2mv13ohfrwWyFtFv" +
                "mbin+/W5/T5T/v6CdDB3L17se/l2Ym1Hedcvr3Vo//4QtTm/aA" +
                "i1+Yx1/ZABhmSe3f/fjhsP4/reoX6/qPTUf2GSqvzV3R0sXjbv" +
                "zvt0Va+lq/Nyv9/3xEnw/osH/J+i/N/qTPnyy1Pv38+OrX5iGY" +
                "3/D8l+7/dH3j2s9yPhn8nVv/efUr2W6jzj9Cir+15Wsr2vJGiY" +
                "VZYTMp+ExO/QDpr1z99tjlsf62shurWtWma6xR3fJ07arlHn6m" +
                "B/jP/834zRVvfGh+FuuCzXw1v7X442B+ufalEY72I/BLKD+o7f" +
                "u+Rzz80CDVJhL/nJIvaPdbifhosv9k7P1eAFMJ+Kcg/JGAf4Nv" +
                "EUevKf2POf+o/Uj8zkuMlX+ml3/8+T22/VcCP6Ocj737FVUSv7" +
                "sC478qPD9bSLfpf7kaA9jE+k0h/Nb4jogF/I9mH7bz6/FfNpno" +
                "m6/U/RGiX3n8n2Pvx3z9B+PH8tvj32wY/CFGfp82/+L67/L9f+" +
                "sTpzP7v+hwZPGgar7o/v+Z+FJ8alvxWc0M8Xxm6P9J24/Hrp9L" +
                "59oXUH8AdOtFMOPaPaX7OZGNv5Xmj2X8u3bt153/79Uz3S7wnU" +
                "osZ2/kk65Q5/8r/PELB7wfNtvIJf9x6fen9OpXuqdvy6/+0nLN" +
                "2tLOx6r8jb3yeq88aJ9J7/3H3OubK36YqX8ceX/E+k80FP4SSQ" +
                "/IP+y/BPQfHF8dxmdI+I1jE1SSOH9r+TCIDx2tj4rFpwnqimMF" +
                "AvVz6entE+NPt0XqIv0/Nj00f8PxT8sj+rDlNX83hfqfWH+C/Z" +
                "+J/23I/vsLx/7Uu/0J8HkzYAnVfncsaVxYfh3V/o32P62p8nex" +
                "k79Z8PtEfJ1/P4D1j330ZX1+BvHf+Es/v9T7YfOeTLsfdvmPD+" +
                "Yfr3+QP3Lrt8M+10n2owL8J3J9ovYPha78/NUC5SMDfqUo+iFX" +
                "f5UZ7f/I9gPj297fmJY5bu9vxPb+Bup3fPwiIb6E4Z8uKPGBOi" +
                "L/EsivAu03m9C/0vafAPq/oI+fvP6e/DaAP5L4fyj/Dtx/hrd/" +
                "sfwI5hci5B8A8hnkH6DklwnnfwiXJ9ID+XXC8S389Utc33H+H5" +
                "Q/KDX+GOWPyDW+G8720+L/xVD+Xvb4x19H+McO/7y81g58hIaP" +
                "ztzyocmFn9PwgwT7IVP85In7FxDwL6L+G7N/R/I7gO+h/D9c/Z" +
                "FUPjREMH6CfGDm54nkP356RP6bjPgujI8u3D6zf5j/hucf3j9x" +
                "zyfMvwLxo4u0/Bs2Bt/eyzOQim+j9kvff/rkD48/q+GU2T2qpZ" +
                "pQGB/cOXUNp0lS5K8Su/wfcpz/I01+c/PjnBQ90/yk2feIv8D8" +
                "Fcz8GKXx42H/KgeXcdKz4T9o/Oz8M6B86fU7iH8Zw7/61Pt3bD" +
                "r0/2buz9L4Vvr9/NZ+v6Dlb/f1i20/kedPO0BtUv47xcEXLFn+" +
                "64T1Sddfafgu2T/LAP2rEB3GL0L8dBvfKffjM734aTb8bDj/ci" +
                "BZU8Z3IxJfHeHH/vqDX/r8u/LLct4/KMPfuf6JKH4Pxs9B+/OR" +
                "s/9R8XcBmyZL/JIa26cR+f+hfYvoefKblfO/CMfHsee/lc86yH" +
                "+Y8XOx9U/xYVlSf8mnf3rPN+t9m9PMv3ts+t5BZvpXROR/bYQz" +
                "/o6nvx4/P1tZ+w7Gj2H5FSxPej8g+H5BYfuDGd/Gxqe564viY7" +
                "b2ned+Kfl+f3C/pVLnn8//qevn7l8ff1IP4k9aRqK38Sdu+i4+" +
                "hW3fbTWsxPgNEB8C4ze47VPuDzj4FXf/kM8/wk/d+nmfP8gI1R" +
                "gpexb4y2l8RQhVhfrl07D+QY/vQPp36v2glfB+Kqx/ofiRDoMV" +
                "ld3RZV/s6Xpm7co/f9Q++X6Wcn+nwvYh6B9R/vvxiXD9QKdA5a" +
                "f2owYWoIiLX+DK55j7jST791DvV3r5N+t+i3n/heYv3/sPafgF" +
                "sv+L32+Q30cqg08Q9T/h86/H/qWLoXyP7j/VP4YaYJd0vtL9+2" +
                "nxA97+s9/XGvk3qT3/piqH/VLEPz3GPi/Rv3z2GdJPS9uP7Pcn" +
                "i8wfne6+Px3FX8Tk98+Mf8TRPf7ph/aPD+3PZqI/xOkvTST+6W" +
                "2fun95+tvmi3/fNjL+xY9vc98nRQeM+/4vsbyXzotPiN0/Xv3P" +
                "y+DA+71c/pqm3xxsfxxifjj2U+n3c4vsv923eb/2/vr9ybfj92" +
                "uxfYXfnw/HZz6hXQ4w3lcILRHz/S2kv+L6If+llpdp5cP9w+/T" +
                "cssnmEdJ+IEH/8Pzw8X3kH7qrJ8cfwrmD+IX46+m6KenQ0/zf3" +
                "eQk+9pkf0I90fww+9jon7ZhHHpaXnjWwn0vmEM/6kgSj/mb2cf" +
                "eN+n/NetmlAevW8JymP/cR7+y/XfTssfTi9fGn88uP91QxUktk" +
                "z9TST/SDr/h8zPGHN+AvWbtPbT8KtTyB9Oy+8f8f5OEsCF4gMI" +
                "9lvYvzfb+OLeh6G/X8NrnzT/ilGeuX98/rd0/RgND+bHGsqXaf" +
                "6QvO9fTPVrwLPQ+Sa8D8G1X7n2sQD4fji/KRN/sPz8BOz8TeH8" +
                "ms3uR4L/ZCb5FiXf6fYvN/4oQ/wSlz858T36+wC89wvK+N+N1j" +
                "36fUKu/8Wufjr+R6y8Bs3W0QY2KI/yV23+KcP2bar9hOksfLY4" +
                "vjLNf6jj+BeQ//8DuF+iNw==");
            
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
            final int rows = 644;
            final int cols = 8;
            final int compressedBytes = 1603;
            final int uncompressedBytes = 20609;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrdXDuPFEcQrm4a1F4dYmztIWeeRVhanRw4cEA4J10CsiUHDk" +
                "gsXWBLJITkgwPrDAlGF1qyIPAPcEC8SJYjEmTnIH6Je3bnbqd3" +
                "Z/qr7pq+PXkC7m5rq/pVXY+vaiBaPpq8x6x/tXRIWlHRfmYqut" +
                "b8nNB8xWZ9Tqq6zM1T0vbjyXd/OfnlWr5afnzi/i0w//mjQnQN" +
                "+DHdCOm0G/qbb54fPP3u9PZEL17dV28P3u+/M89mnzw6Op6+uv" +
                "Gtqd9cd/R/T7+419Bnbw8+7L+btvQ/G3r//gfOtyLGYygst4/e" +
                "f75Tt/aT9VcUmX1HrMtGd1TF0Q/bOwMd1C/q4Ved9ZsOP5KP6P" +
                "P2T18+YN88H43X79P1mlz133/bzhLyM+c3aB+Y47cTqDb3r+w5" +
                "TMO2T4u957f04tRMiG7eL19P3qsn07rUHx81ClY09yOsH2b7qh" +
                "Tdu4Lun5Tev/71A/ltGTa+6Hzg+THsi+bYFBVxvhHzP1emVn7d" +
                "Y38DdH9//1nt74vV/n62PJ+w/vbye+dzNv+rnfmb4flTrP1ABh" +
                "Cdjz370X8/xfoD1gfptiHogq5s2O85yz7j+0Ny/dvl/uP9CdLl" +
                "8UXf+a2fWfG1cnRdWDf+b864ViVVlqwma33bpdghTASRYV9k/E" +
                "v/U53u3Wv8D72efFBPjOd/7HJ3TGerDLVeqeDZvzA/tA/A/wr5" +
                "/1qtv9//Ov1BdBxfcvMLyK9S4gsxHeVfKH+Tyo/l77na9YYww7" +
                "pYzPzBrm+C2TIDihj2mWOdVDuEE6hMlPtkzT99/O7+lr700vvU" +
                "UuRT8tbPlqt4928jv9z7iCa/GPd9vahJFSVNvzfFrPiBjqd/3K" +
                "gwv/j8AX3v7uD8XnLmh/0nd/+S4+NDEJ8NjKpZ+MPKPw7fT8gP" +
                "zmFWHK3ig2Y+TXxAq/iAlvHBLeb5ps9Pen6ff/pQ2QnpX5v46n" +
                "c3k7+/spU1c0137OO75Tjxc3D8H/3xf16NT+34jPkFnxHki9ZH" +
                "9ECIXzwA+E8ZZ1ZrAEbVAD+rIw1ULL6bbL9T8zO2A1Uicrr/Dc" +
                "pn269B/5o5fsT3YyC/s8z9Ofb5z+Vblvz8/o9Q/CqzrxZufh2V" +
                "fNQJ+h3In41L350ndGIdvTmCa9OmIuOU5kt6fOjFh+H8xqbmP2" +
                "h9LT48KD+yPlKF8Llt/YL1BR6+LaBbEVnML6wvnPtve0X1+u+B" +
                "+kG4GIPrF+z6Argfcvyae+6q1//E4f8Z8GWIf0P9EuKbEL8V4Z" +
                "sM/DbR/s8B/WStP6i+wVm/1D7YmPoZ/3yhfu58f8P49aKLL9IS" +
                "X/TxV3F9SYovh/FbXL+U4c9yfBrVb8N0hn+Ojc+8B/cXoPiHFd" +
                "8w+HPnL0DvkuOEi8pfAD4ftQ9mPHxTGh/K8elx0ufh+Cj4ROdv" +
                "dax+yvQnJj83Sfsnmx/CvxB+huPnMF06fjp+Nye/vk9p+B3C58" +
                "D4jPjQx88WZ/0XsfgZwu9i5x+L/wL7mWqfkX1A+Dm8nwCfEuJP" +
                "LP1EzklL7G8ZzG8RfsPGd9L9sxLhP9L+X1jfDuN/KL4T9/fg+y" +
                "vih/gKL/7VwfuvWUhpb3/jCPhp3v6obPMbKz8ts/Y/MvJzpJ/h" +
                "/rDM+4fxr8u9v7n7i8X5u7B/Orv/kMYvmfs/RugfktHF+T8Tn7" +
                "LJ6xPmp935Fdz8uubXX1D+W5KoPyS2Plxx9p/fvyH1f+nyefGZ" +
                "2H9A+y3MvzjyBfk31g9kH0H+I96/8P2plv7lmXGSbha0cP7tp2" +
                "bSjX+hFh8Ob460/5QI1/8E6+O//xXQH1b/ixrCJwF/5vrNJY+v" +
                "8tdH0fsR//f1p9Sv+P3l7PpLvvg0gX6B8V9UfaEA3y22vKC4vx" +
                "rQkXzcPy3TP1b8xbq/gahC9H5a2D5AfF0on9EfK4of+fFvWn8i" +
                "+/0zWf45GF+I69Mofsnsv3fuX4T432WfH8RPgX4y/v+DvPgCzP" +
                "/D9jm3f4H0zO/3pNm3qPmz9E95nt2Mxi+uTyf1R8XUf6X+V1pf" +
                "ZPgPVB8MzE/cX7fr+knu8fH+S/HTvP1B2ft7wvsD359L0l/i45" +
                "NIvvT8xfqD7j/Cny45PnMR42fV7xT9H39+g/4X0nP4j5j4OHd+" +
                "IJPPjk+Tz1cWv+L4Ni8+JT0f6frg+v8DFnpt9w==");
            
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
            final int rows = 65;
            final int cols = 74;
            final int compressedBytes = 4665;
            final int uncompressedBytes = 19241;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNqVWwuwXVV5PlaYthZLg6ghA80AjvhWWoKihqx7HuaSWJm2Oj" +
                "6IxkSihphIJCYh9ybZe6/zuOfcpNwkl5vwKOSJJKEVRlRGylTo" +
                "FHkXxPAKlAYwSiY8BWWKZvqv/9///v+11j6MvWvOv/7Ht77/2+" +
                "ucffY+597bONI40h1qgLUnNWeMPOo8jMbSXuNI59l0RuNIpZKe" +
                "kGef43ql4mz3ZIqyU1ycrbDvSM8G9EuEsCdmD1E9va61gdc0jj" +
                "RvczYZGD29Uvwwr6A4qlQGXk226IqPrlSSKZxjm80K+Zy1H+JO" +
                "9pDz2rfZ08A/rJG+FqieYKe42QyaQVtD+xkz2H3SeRh9IRs2g8" +
                "1fpQ2Mvphnnyvq5zk7en0Rn28GW+N2up1hq3YnZhr2U81fU7U7" +
                "lj3CSGA9SPPoDfYf7Zfsl+08O1+q9mviQ3S2nTXwcjqC/uw893" +
                "eqfq79e5w/j3YO2rnZwwoxgLZuBts320E7E6ND9hz76c6w/SD4" +
                "h71+n7Wf8+Kv2K+it8gssv+C9gazqLfBeW7Ac3KJWdR6r92Nte" +
                "spC/tU1MWy1zhkv2evdc8IYn9s/7VzK1frkxlpFsE+EdsP7F54" +
                "1d3IK/LsT8R3KyuVzs1pV3fT6KLb96Vif6S72T1c6VxUoPH11D" +
                "3P/i34h8N+OrY/tDehl5kMKmgRk9GAfXrAZCPf4RpnuV4WVxcT" +
                "A6+xr3V/z7jWRsG1nuEsoXXfkNNFIwPpaH8VWiHb6uKyunTis9" +
                "1+VLMxRrPbV+xv0dtutkMFLWK204B9etpsb7/CNc5ynYb9ro6a" +
                "O4mB18A+/YHXtc8UXPs6zhJa9w17uGhkdrq+vwqtkG1zR1ldOh" +
                "X79EnNxhjvCN0+OW+dWQfnnbM3IGYdDTuWPWPWjdwC552Lrqes" +
                "1Mvi6rfcecdZd971bmFc+1zBdd6W94DzDuyNui/EPwk7jBxKd+" +
                "tufte82/eV/6PqtxTfHq7k+7SOPdinJzQbYzQ7nnfO+7b5NlRy" +
                "Sx5F2UGYd0lNz3qNxNULuD9lYN5F1fS6gY8y0rHyGvmRqvYJ05" +
                "1JK2IVeZdAD5x3F4R8aHdpbawv7lcaLzQLbQttFx6XOA+jMTvV" +
                "LBy53V7kIsBS9mqup/ucbe3n2CHs1vxZSihTqYyelK+baKWMNA" +
                "s7J+bZEbse0OvsFcyve+WYJhz1/vSgp2KTRlPWbkC7EW2nlSiG" +
                "y3kt9GoTGrx/QqXPQH2t7sd8UdwyLfDQYq5FA9S9y7R607jGWa" +
                "6Xxe31xCB8o3/NuPYXBNf5OGcJrfuGnC7qrkmf7a9CK2TrtMf1" +
                "bBd34tdT8881G6vx2Smun1E/A67aaN2P8yiqnlY/ozefa5zlel" +
                "ncvp4YYj64V/yY4Lq7OEto3TfkdFH3uezN/VVohWyd9rhO+yTa" +
                "YJ9O02ysxmen2GwxW2DH0OLebaEBr8l3mS2987nGWa6XxZ03EU" +
                "PMB/v0O8F1d3OW0LpvyOmi6mPZUf1VaIVs7all9fz1VGiDfTpd" +
                "s7Eanz2P55q54OWWPIqq7zZze1ZqetZrJB5JivdHzLDn/PYXGW" +
                "nmdu/grHofn+szSwRKHs2mlKugeqiHtMdK831SnZtnlvUrjRcb" +
                "uDtkSx5F1dlmce+/pKZnvUbi7veKo8YMe84feI2RgPsFZ9U+Lf" +
                "aZJapUegeyU8tVUD3UQ9pjpaE22Kd6Wb+yOLkt+Q/bstOSDbab" +
                "7rWXpFOT+9N1yUNwvftI8kjveXtR8pj7ZJg8DZ9vP2ivzuDOM3" +
                "kZ/H0JeK39yf+mlfRoiD+U/pndmrnKh22SvjV9G9xZ/Ka4T5kY" +
                "+C1+Yv2f5KnkQOPl5Jnk15CF612yma53sO7N6VHNz6d/gf6x6T" +
                "vSdyY/TW61zfQDvYPZu4kn+XnyIKzbBN7D8HgiezUZd5+Dk0N2" +
                "gz0j+Z3diN069sPpW5Dn7RBdnvwsuQdQD9D1Dj8Bf4mud82Z6R" +
                "67Nud+idW2JuWZifRPkOUv0+PMZeYy2DG0uHeX0QDOvzGX9V7k" +
                "Gme5TgPux1XEDDEfKLtAcL1TBB/2DXu4aHRyZvqr0ArZ2tPL6t" +
                "KpeD19RbMxJjwm9BqmAfdPznbhcYnzMBprHDaN3gK4f4IIsJS9" +
                "mutw/wQW7p/y2CHU/RNmsAetm3BsPHqn5ll9/9TQTDLc/dPo2d" +
                "k5nopNGk1ZuH9ydiPaju4G90/52vz+CaP89TQP6mt1P9EcxNvM" +
                "NvDQYm4bDTjHTzDbRo/mGme5TgNeTypihpgPXk//LLjOY4IP+4" +
                "Y9XFR9JJvdX4VWyNZpj+vSqXg9rddsjAmPyc34vdlEc2Pyn/J9" +
                "Weti931V9Z38rVW6mr7Bco/kcPK8fPcl35xRNXs5XclZ/f4E+3" +
                "SToHvvo+/peBW8Awwl9A3eePhNGa3O5kHne9NVxTdz+2D9440j" +
                "2augbgV9T8dceIVE7ekkYUuX552W6atH8/LkYP5e9KKszjN/SL" +
                "8s681ysxx2DC3u3XIa8Mo8i3KCkFlQfkT4mA/26aDgekOCD/vG" +
                "nO6RfaO/Cq2Qrf1YWV06Fft0s2ZjTHhM6FVNFbzckkeR/YSzUt" +
                "Mze3DeqVhd5asccbW2m5Gm2mtz1l8R9hBM9s1yFaFCtvbjIZ/o" +
                "052bd5b1K40vNZeChxZzl9KAV5ulnCBkFpQfET7mq1Q6Rwmu93" +
                "PBh31jTvfIVvZXoRWyddrjunQq9ul+zcaY8JjQ65gOeGgx16EB" +
                "z8kCyglCZh7welIRM8R8sE/nCa73oODDvmEPQmQX91ehFbJtds" +
                "vq0qnYp55mY0x4TOgdb44HDy3mjqcB2sYoJwiZ2YN9UrE6iwI+" +
                "+Cx5gJHm+N7vOeuvCHsIJttQriJUyFZ3k0qoDe4o/yo8Jl+LxP" +
                "o3N/pKA95L/G7/00r8GyH9e6DwaqOuhi9xvTNH0KOflevdH/N7" +
                "Kdinrf+/30txX1+prw336bzwmMLfSxXXuzlmDuxYbsmjyH7TWa" +
                "npWa+RWL065nDE1c7XGWnmjN7OWX9F2EMw2bZyFaFCtq25IZ/o" +
                "051bXy3rVxovNUvByy15FGXbnZWanvUaidVRL+WIq/UnGWmWjt" +
                "7FWX9F2EMw2Y5yFaFCtrqbPjpfG7xSzinrVxo/8kYju8aPYc0b" +
                "xGEV1u8pq2TX6hwq6avA1WCfdr9Rn2QK57SN0e57FemJ+783Rv" +
                "eJ1xv4jEUWc+tpwHm3iHKCkFlQfgSdbyzjg1d4U3CjBwQf9o05" +
                "3SP7YX8VWiHbli2rS6fi9TRLszEmPCb0lpgl4OWWPIrscmelpm" +
                "f24HqnYnUWLeGIq/VLGWmWjP6Ss/6KsIdgsn8vVxEqZKu76aPz" +
                "tcF+XlvWrzTeaXaChxZzO2nUJtcmm53ZnVyjrNTLYmaI+eB+fL" +
                "Lg4Lwr8GHfmNM9srv6q9AK2epuUsm/zyy0Ae/dmo3VhMfkZvzc" +
                "++nW7GTD6J7OAn0Ntdfo6z2g/oE+B6vMoH+l5uuujlrn0jxweS" +
                "W4u6D7gmSzvjMY3euqreCvTeB47guv6cXn8nG+L9B3JnaXr13f" +
                "F+if9uLO+fpzcOsz3ifhCXVfsMqsgh1Di3u3ioYdq++lnCBk5u" +
                "HHzBDzwT5dE69itO4bc7pH9mB/FVohW6c9rkunYp8e1WyMCY8J" +
                "vc0GnlGymNtMA3rtMZuza7jGWa6XxcwgfHC9yysDVwgOzrsCH/" +
                "aNOd0je7y/Cq2QbX1PWT0/7wqlwPuEZmM14TGhVzM18HJLnhu1" +
                "SbVJLpaanvUaidW7co0jrjo2f63GC4/mFEy2oFxFqJBt3E306c" +
                "7txWX9SuMrzZXgocXclTTgOdlNOUHILCg/InzMB/fji+JVjNZ9" +
                "Y073yJ7rr0IrZOu0x3XpxPs08FbNxpjwmNBLTAIeWswlNOC98D" +
                "2UE4TMgvIjwsd88A65Pl7FaN035kQt7+2vQitk21pXVpdOxT4d" +
                "o9kYEx4TevPNfPBySx5F1RFnpaZnvUZidRbN54irnYSRms1fEf" +
                "YQjH1fuYpQIVunPVYaagNVy8r6lcarzWrw0GJuNQ3Q9n7KCUJm" +
                "QfkR4WM+OBPG41WM1n1jTtTygf4qtEK2cTe/U7FPmWZjTHhM6M" +
                "0wM8DLLXkU2fuclZqe9RqJi7uXhDK82vnt+YzUbOr1FFU1xn69" +
                "XEWokK29N+QTfbpzp2Vm2LVhv9J4pVkJHlrMraQBd1cvUE4QMg" +
                "vKjwgf88GZcEy8itG6b8xpVlaPsd/or0IrZOu0x3XpVOzTmGZj" +
                "THhM6E2YCfDQYm6CBjwnj1NOEDILyo8IH/OBotvjVYzWfWNO1L" +
                "KwvwqtkK3dX1aXTsU+3aTZGBMeE3oXmgvByy15FDXGnZWanvUa" +
                "idVZdCFHRfVeRmo2f0XYQzD2gnIVoUK2TnusNNQG+3RLWb/SeM" +
                "yMgYcWc2M0asfVjjNj9mdco6zUy2JmiPngzu+4eBWjdd+Y0z3s" +
                "Hf1VaIVs427O5vfjhTbYp9s0G6sJjwm9YTMMHlrMDdOA5+R1M9" +
                "x4nWuc5ToN+10dMUPMB1eg32ic4MO+YQ9COC39VGiFbONufqdi" +
                "n07VbIwJjwm9ITMEHlrMDdGA5/AXZsg+xDXOcr0sZoaYD5S/Eq" +
                "9itO4bc6KWh/ur0ArZxt38TrxPI8doNsaEx4TeMrMMPLSYW0YD" +
                "nsPnKScImQXlR4SP+eAO+c54FaN135iTtfRToRWybd1RVpdOxT" +
                "69R7MxJjwm9OaZeeDlljyK7KPOSk3P7MF5p2L1rjyPI662XmSk" +
                "ZvNXhD0EYx8rVxEqZNt6IeQTfbrzyIKyfmVxujn7ZLqlUpG/V6" +
                "G5Nlgb1HH2ieYQ/71Kml9XmxcV30+c4n8Pln6q+I3rUuZrXhx+" +
                "S6ZXJPTXJH+anQxrhkNU8yPN7yT3KvQ+eDxeRFOwU/6prbnKdY" +
                "N5TTop299cEnZtLm8W908jv3J/r9Jc7b6nw1r+F/jNZc0V7u9V" +
                "1PM408wUSx5F9klnNcKvx3HIyh5WH2CkZvNXhD0EY5eUqwgVsr" +
                "X/HfKJPt155NmyfqXxFcb9NRtazF1Bw47V76acIGTm4cfMEPPB" +
                "O+vd8SpG674xp3tkW/ur0ArZxt38TsU+HdZsjAmPCb2maYKHFn" +
                "NNGrVja8dSThAy8/BjZoj54Ew4Nl7FaN035nSP5q39VWiFbONu" +
                "fqdin17TbIwJjwm9HWYHeGgxt4MGPCf3UE4QMgvKjwgf8xFbuI" +
                "rRum/M6R7Nu/qr0ArZxt38TrxP3ZM0G2PCY0JvgVkAXm7Jo6h+" +
                "l7NS07NeI7F6t1nAkWbz12q88GhOwTTvLlcRKmQbdxN9unN3bl" +
                "m/0nitWQseWsytpQHvhU9TThAyC8qPCB/zwZ3vnfEqRuu+Mad7" +
                "NPf1V6EVsrVPldWlU7FPX9NsjAmPCb0VZgV4aDG3gkalUj2Tco" +
                "KQWVB+RPiYD+Z74lWM1n1jTvdondhfhVbI1mmP69JJ/R5hhT5m" +
                "OWJ9TOitMWvAQ4u5NTTgOfkl5QQhs6D8iPCUHRgWPpjvi1cxWv" +
                "eNOR1T48X+KrRCtgPDZXXpVLyefqzZGBMeE3rnG/c/UbkljyL7" +
                "vMHflQrCrzsL9+MqVu82kBkYYg+r9zNSs/krwh6MGRhqLS1XES" +
                "pkOzAU8ok+3bn7elm/0ngWjF2FJQ8j+wLGmAHsLIWUjB/vKv6b" +
                "FDPo8zphntXKJIvoXQVO98oxmjFUQV2CYwi6qbVFJ8eI/2H0Jp" +
                "9N8YXxuBkHDy3mxmnUTq6dTDlBuCzXaQieqoQnlOYjtnAV1f2+" +
                "ISch0mf9dbEG38bd/E78euq9XR8TY3z2PG4b9z8faDHXplE7q3" +
                "YW5QThslynIXiqEp5Qms9O6JWcpbrfN+QkRHuvvy7W4Nu4m9+p" +
                "2KfJ+pgY47Pn8SazCTy0mNtEw47VapQTBGSv5joNwWN1K+HdSp" +
                "8PlNfiVVT3+4achGj/m78u1uDbuJvfqdin6axWY3x2iuvT6tPg" +
                "vgwt/i/sNBq16bXplBOEy3KdhuCpSnhCaT5QPj1eRXW/b8hJiP" +
                "Yt/rpYg2/jbn6nYp9m6WNijM9OsbnKXAU7hhb37ioatam1qZQT" +
                "hMtynQZc71RUm0p4Qmk+YuPBWar7faUqEezTAX9diAht3M3vVP" +
                "y/0NH6mBjjs+fxiBkBDy3mRmjALq6knCBkFpQfEd4mIR+xhauo" +
                "7veNOd2j/VR/FVoh27ib36l4Pc0GtWv9XvExoVc3dfBySx5F9Y" +
                "tNvX261PSs10is7obqHGk2f63GC4/mFEz79XIVVA/1lHVzNv99" +
                "i+rc21HWryyu/B++mR5U");
            
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
            final int rows = 65;
            final int cols = 74;
            final int compressedBytes = 4319;
            final int uncompressedBytes = 19241;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNqtW32MVdURf6vyIVWrEjShHwJ/NLE0tlqbJo21532hKCBt0t" +
                "akgkQbpMbQxoQqsa33vvd239tFodAvS0qhKGj9p4k2bdL0D9ll" +
                "d/FzEREtRWu1flTRWrCgAktnzty5M+fjPrHp3tw5Z2Z+M/M755" +
                "173r3vvTXXm+tLJZNJ6uFRnVGdgbr4yMp+10696oyS/SMU9XU2" +
                "N5YRGufmFExjwI1z/T4fZuAzlUpcdeB3ekw6X6DPNXOhl0nqkd" +
                "Y8ilJ8utUxoqtRz2WNveV7GamzuRF+Dcb0Hu79cZyFz5Bl7yE/" +
                "n/DTlQeeidWL6jeaG6GXSerhUZ1VnWVubI6Lj6zsd+3Uq87K1t" +
                "MsstgaKpsbywiNc3MKpnncjXP9Ph9m4DNtbBF0Nk+H9Zh0vkBf" +
                "aVZCz0prW0lHqdTqIZsgpBWUqxGerOWVkg/asTCK0bpumBMzzZ" +
                "lUzEIzZFmO+qUSz9Oqj+tsjPHHZHsDZgB6VlrbAB0wTyeZgdZW" +
                "9rGV/TGdM5C19/1SqT07xz0tuPb5gvfrhjnxbH+6mIVmyBKuu4" +
                "g/W085U5inC3Q2ZuOPyfb6TT/0rLS2fjpgnk4mmyCkFZSrET7M" +
                "VyrV7g2jGK3rhjnxbJeLWWiGLMNqbqV8nr6rszHGH5PtpSaFnp" +
                "XWltIB83QK2QQhraBcjfBhvlKp79wwitG6bpgTz/ZlxSw0Q5Z9" +
                "58T8Uknti6kes4xYj8n2lpgl0Msk9UirXIRSfLrVMaKr6ktYy7" +
                "1PMVJncyP8GoJpTYyz8BmyRO4hU58baWG9qL7YLIZeJqlHWv01" +
                "lOLTrY4RXVVfzFru3cVInc2N8GsIBrnEWPgMWRLeZ+pzIy2sF9" +
                "NL3l99nHutya6n92voS/Znd353Nv7cezn7GrN0JOh/ZL33qozP" +
                "7tIJ/TVmQswVvrV9RbeYZLpvaU3yR/NBf8nblu0Ch8tqNZ9nm7" +
                "NZsk5a5XNkE4S0EqN1P6vkg5E+wkidzY3wawimPS/OwmfIErmH" +
                "TH1upIVj8OtjWx/HOSdJrwAd8Jp8imwsBaljWHNfOz8fjPRRju" +
                "SopOyuY9crmn1lN7nVXH8y3eUD96Uv+vmEn2Rl1hrpchHdrDFr" +
                "YMastHO3hg7w3kU2QUgrKFcjfJgP5umxMIrRum6Y00YvKGahGb" +
                "JE7qFfKqn1tEaPWUasx2R78Ae9TFKPtPpBY+x9phG79oe6qm5Y" +
                "Y297zOR/cJ9pBKFxbk7BwH1mlAX5fT7EPWSa3Wcal2lYL6rfZG" +
                "6CXiapR1rr8yjFh0f6hvi1naPz6jexxt7yrxgpUc48BV6NSfe7" +
                "ca7f5wPcL/LzCT+3cqxeVL/SXAm9TFKPtMp5KMWHB8xT7td2js" +
                "6rX8kae8t3MVKinHkKvBoD8+TEuX6fD3EPmfrcSAvrRfWbzc3Q" +
                "yyT1SKt8AqX48IB5yv3aztF59ZtZY295AyMlypmnwKsxME9OnO" +
                "v3+RD3kKnPjbSwXlSfZqZBz0prm0YHrN0vkE0QZlrjm+LXduqp" +
                "6l4+mKdfM1KinHkKvBrTuNqNc/0+H+B+sZ9P+LmVY2Pw69veOr" +
                "MOelZa2zo64DWZQTZBmHWwntbpQ/A6Q5gP5umXYRSjdd0wJ56w" +
                "npy4kIMrkXvol0pqntbpMcuI9Zhs7wZzA/QyST3S6m+iFJ9udY" +
                "zoXLuZkIWjsd/3SUbqbIpv4NWY9vfiLHyGLGu3+vmEn1vZ3NC8" +
                "3a8X1TeZTdCz0to20QFr94tmU9+F7GMr+2M6ZwjzwTzNCKMYre" +
                "uGOW30u8UsNEOWtZUxf3ZfkHPTbKVWOCZsGy/wPWm2Y+Z3yWkl" +
                "9uzn3s02V8jzXWN7t+en9oGYVXLld94zY89k7Yf8+33NOny+65" +
                "t5ok93xRh3pGa9Wc+SddJal5BNENIKytUIH+aDHePxMIrRum6Y" +
                "E8/OwmIW7hiKqrmV9Pj1mGXEeky2t8LAmiBpbSvogFo/IpsgpB" +
                "WUqxE+zAf7+HthFKN13TAnnp2vFrPQDFki99AvldQ8rdBjlhHr" +
                "MWHb5Tn40hN/DtZXbvw5uHzEf479MM/Bna9/uOfg1pf/78/BG8" +
                "wGmDEr7dxtoANqlc2Gxlb2sZX9MZ0zSL7G/ezp+4zgGvcJ3q8b" +
                "5sSzsa+YhWbIsm92zJ/t4zlTzVZqhWOyvWVmGfQyST3SWlWzzD" +
                "4HLxO79oe6qr6MtM497O3MZqRZ1tnMVjfCryGYzt1xFuT3+QD3" +
                "ip8PZTZPy1ymYb2ovtashZ6V1raWDrjGF5q1nQfZx1b2x3TOEO" +
                "aDkVYE13lA8H7dMCeezeXFLDRDlsg99GfzlHPTbKVWOCZsk8Fk" +
                "SL8/puclO+GK3wOvyZzk2Q681yd77ee9L8H5Kn7uCzsLvMenM5" +
                "N37N7wflpKJ9jIyZInPT2d6r7LdqoW/ULyYvL3zlDyj+S1rN4s" +
                "3gHSk9NTQP+ItX40PSc9N3ko2ZbdfyzP9rRdyVOET56B8zlAzo" +
                "B2evK6zX44/9y3nk5BVGqfO5LR5DFAPYnz5N8HsJ68nfw7vC9I" +
                "T7JszkjPNk3ThBmz0s5dkw54TWaTTRDSCsrVCB/mg3m6KoxitK" +
                "4b5rTRTxSz0AxZIvfQL5XUemrqMcuI9Zhsb6PZCD0rrW0jHfCa" +
                "XEM2QUjLR3OF1jhDmA9G2tY4wft1/RqE6IwVs9AMWba+FfNLJT" +
                "VPG/WYZcR6TNji6mr3tztwz9vXbuv30Mol9XHcx+XugL9vEUtz" +
                "xQd9Pp5fd3fIO25nc3hf4L57B/cFd4f3G+qame4zQO7uPQ1K2p" +
                "+Kvm8J78wbLys2x/kA/bjWGhPrx/su1DYfE8b4Vu41JlXGfJyg" +
                "/ax+Tnifv6gbi8YENyfM01gM3XeBrpnNdzCGuI77uNmSbE+GDc" +
                "w29J5IdiZjyR6zpfKl5FmzBSx77Xy/lLycvAra/uSt5EByECzv" +
                "JIdAV/u4yV4vs4X38c6dyY7k0eQRm+dvvI9Da/dxtLJM7D6eTs" +
                "J9PD1N9nHKCdEPJ48DCvbxZDdoTyfPJPuS55LnaT0lr4PtzeQw" +
                "VJ6Ynorc0ynpmelZ6bRkBDywjwPqyazSX5K/YsZsHb0C/n8mb9" +
                "A+nvzH2t5N3kuOJsfSHrWPrzarIcpKy2g1HfCaXEM2QUgrKFcj" +
                "fJiPsvlRjNZ1w5wSHWehGbIMq6GsXCPjyfen1XrMMmI9Jttbap" +
                "ZCL5PUI621xCztPC8+3eoY0VX1payxt7OakTqbG+HXEAzcP0VZ" +
                "kN/nA9yv9fOhzO6flrpMw3pRfZFZBL1MUo+0yjfMos4L4tMt9+" +
                "D9Tumq+iLW2Nv5AyMh6xBb3Qi/hmBgnqIsyO+Ogbm7+VBm87TI" +
                "ZRrWi+pzzBzoZZJ6pFWuRik+3eoY0VX1OazpbG6sxksenVMwre" +
                "vjLHyGLMNqKFvXudxIC+tF9VVmFfSstLZVdMAT/nazyj635Ahp" +
                "BeVqlCHMB+vp4TCK0bpumBNPWE+FLDRDlsg99GfrKeem2UqtcE" +
                "y2VzZwH8OSeqTVDxl7hyMIU0Yb+1HCdaf0+qG8epliGQ/z9Cwj" +
                "JYuznspuZtEkD3tctGbIUnhqD/FzK8fqxfT8PiJ7kkovY0t5RH" +
                "x8v9VqwvPdtsivcfBz3+FWR/Jk10s7v8/c1zklt/YGn7Fu4zj8" +
                "XU9ncvA7nb7OBMFQFdbq28LPfcsjjfcJ0TlVIloNn5/o9LueOK" +
                "ecxc9b65LhbJ7y3yik+Ny9x+6ik8rLbfXleD+evBWfp94p6U8g" +
                "6tb4/S5l0n/NM6Wf3pbYV6L108bMoujk8ebE/O75aTj3ZbG3EL" +
                "uwWnqWqvB9sjYnaFz6s+SVonmC+6dF4RNGfdR/emjdkd+1j6Lk" +
                "34nVR/kzQPydmP/cQnnk+YJ/J1a+2/9UlJ5bOAqzcuXeK4Lnln" +
                "9pjO2P5zxGw+eW1irrG1XPHaPuOPPxj+rnFv07MarH8bVSrdQ5" +
                "AP3hjv1cAfWSPctP1EpkqQ+XcivM03Atx1HLPZtzuKS86kr4jU" +
                "aHUZiVa3QO1ko1hSmV+q/WGOzXSsxDrFKTuBOCqnG/PuzN0zDP" +
                "k8+Y6nG8ucrYV9zw7wOtjmf9qPawFZ+Dqc92jpAMWsurHsX9Sa" +
                "PdurpyZzL5NZfOBMFoPi6X/Hupi9mC+5Pk6fYcHPqdMS40C3E9" +
                "gbTrCXWUsHZ/gZL6JPG087SQcdRq3fWqHeNOH62zu5U7B8mvuN" +
                "ylMZqPy0WqudwZ2XWeAr8zxgVmAX6uAtJ+roI6SuC22SyobKY+" +
                "STztPC1gHLVad73qm8drBV0/wFGcnX1SjQ+yIA/BaD4ul7zaYm" +
                "3hPJylYJ4WBPOkxlgdr9JelX2bSzqelfVV+/lTdRx9bIX9aTv1" +
                "KQ5b7lXWcx62qfW0Hq87Rusoqo5ZuUZnMvklS+teuC/IMYhHVo" +
                "QXq9RE7oTC6w792Lef+3rfWrOevO0zpnr5GI9Uj0DPSms7QgfU" +
                "2lA90r+dfWxlf0yvbKAMYT7K5kcxWtf1cxKiubyYhWbIMqzmVp" +
                "LZ0NkY42Yn3cw38+3+ND/bn0BHCa/hg2Z+/xj1SeJpr7v5jKOW" +
                "9dbvBUle9X63WdD9OzmKs7uVYX+azwdZmss1RvNxueT3BQ9oi+" +
                "Tpet0Ffj3GWk+tB/en+g7an1BHCbX+VOvpfxn79R1kwROuux3U" +
                "R0kt9+xq3cFWb542anQYhVm5Bvsli/2+JcdgH1nVeppTqNW5LP" +
                "5F7BNTysZ94qeurh35fUGP79HxZp6ZZ+dsXjaHVsez9hUzr/9V" +
                "9rBV+mzniJhX/mr7BQ3raZ5fV1dmnFjy9eRV8blINW3RmYv/Qr" +
                "8eY/VY9RhcgVbaa/EYHbCeHiKbIKQVlKsRvpn4+eAd6Lowivxu" +
                "3TAnngOnFbPQDFnWbo35pZLan441b3drhWPC1kw1U2HGrLRzN5" +
                "UO2AsN2QQhrcRoXb0SXj647rYwUmdzI/waghk4Pc7CZ8gSuYdM" +
                "fW6khWPw62Nb3V3dDTNmpZ273XTAetpONkFIKyhXI3yYD+bpnj" +
                "CK0bpumBPPgTOKWWiGLFtDMb9UUutptx6zjFiPCdv8yXLEfw6u" +
                "VPKnwBH9fVl9pOj3PuINv78rb4o/B2dVRzAu/v2dfSYf1hiqkv" +
                "MYiXx/Z7kLU432duuRol/WUT2Or45V4b2fpJ27MTqgVo1sgpBW" +
                "UK5G+DAfvNe/GUYxWtcNczKXIhaaIUvC+36ppNbTmB6zjFiPCd" +
                "v8FRoK1tOcfDaHnPU01HU9DcW/D+7v6bqehjBr8ffBAx/TGOKQ" +
                "8xiKrCfLXZhqtLdqhgrX05COr+6s7oQZs9LO3U46YK3vIJsgpO" +
                "WjuUJrnCHMB/P0WY0TvF/Xr0GIgUuLWWiGLFujMb9UUutppx6z" +
                "jFiPCdv8FRoMPqcby2dz0FlPg13X02B8f+pf2XU9DWJc8f40MF" +
                "9jqErOYzBcT/j/d5qpRnurZrBwPQ3q+Oqu6i6YMSvt3O2iA+bp" +
                "SbIJQlpBuRrhw3wwT7eHUYzWdcOceA4sKGahGbLs+3bML5XUet" +
                "qlxywj1mOyvb3Vvfr+nXQ8W7vIU92LPrYiku2MJ5/2sqY+V/mO" +
                "j9YIfEaQyuwXS2OTxlAV5iFWydi3GPvCVKPjzy06WnOSOcmeb4" +
                "aD/6N+Cvht9X8bC8i3TvwX6437c+Y/UNb7wohkvNsv4Ru/xe8R" +
                "FFp9jxD9nf1t/vcItm7kdz38PULE43yP4P9V78vnCe6xGg+Lrn" +
                "2lUvvy4sjYX+22cJ66R+i8jUc+CFFcrfs8df0firmRcZyf1dya" +
                "z9Me2FU2i659UbZdvbUfSn/gFongut3zNpcX5j0/VllX+9/nye" +
                "H2X+c0zSQ=");
            
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
            final int rows = 65;
            final int cols = 74;
            final int compressedBytes = 3331;
            final int uncompressedBytes = 19241;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNq9W3uMVcUZX63yUORhu1oojSFZkcAa26Rp1aZmz557ErFt0q" +
                "evprYhhCZUqYHSNm1pZ+5yL+y9pvpHEx41fazFGiNV0C6IFbSg" +
                "2IcClocPQIWWKKtCBazSsnYeZ+b75nnP2cWezZkzM9/v+77fzJ" +
                "3H+c45my5Ll7W1pSLlB8/J0uIXlMS9AsosSQuAhlwv8WlJNPbr" +
                "2hRcXgyzUOxxuvh5nxw8tenDx8ZuE9aoTMv1fqdqkoXM391Qxj" +
                "Lf4ZM2fqpyvb2odhFoKL9xu42fhKRc3/XMubtHdVVbqQNzS+9M" +
                "71SpKstS1xolca+AMkvSAqAh13sb4Lhl0Df9ujb52VgTZmG2Ab" +
                "i7ctxiaD9uM+ZstWF5upzlRCrqlss/9pvMVxL3CiizJC0AGnK9" +
                "ywBXOw/rm35dm/xsrA2zUOxxyrm7cvCE+mk5bjPmbLZBj7FL5D" +
                "UbVDW1cXzeQRnLfIdP2mjoeYe4NZaisX1JkRnQ6A3ODaZPJtm1" +
                "tbGnZd55uFWm5v3bp32dL9anPtT3fdF1JCqt3+ZZn/rAb9xuZH" +
                "2a6vNcb56WfopwoxfpfvoI7yf6aSS7Ijam6KjFnzOluFRf5Y4n" +
                "enkRtvTK8HiSHtzxVP9tsX5a/PmifZauTFeqVJVlqXaRkrhXQO" +
                "FSNl1aADTksuk+LYnGfm2bEtHoD7Mw2xDyZnrS/Tzdx8a0bmq4" +
                "Y6A2ha0qfWXWpywyTrGs8W4Re9hrz7wYwh1PfibueMqmFp6DHZ" +
                "UOecVlftamVToW360kqhawKgULWadttfGeZtQJ6MYprGXqYKsK" +
                "w+wMmnaBjzxtW1mnzZGnnn7qxD0R7hv2N6XCxo1MRd0U+cf66d" +
                "LKFNFPGgFXQOFSNllaAHvN0QqXTQZcc6SqlWjs17YpEc1RYRaY" +
                "oUqxN5DIfoK2Shxus+KEtU0NvV5t0fPuCr6O+2XelW5L1hFGY1" +
                "njVFvJozEY9+yMkw6fxDOeOtqGeKRP6n76jNNPT8Y1sxlhNJY1" +
                "R5Zl1RxVjLPtzZR4+mnGkPtps+6nq8Iyv2Z2aRhtykr306JinG" +
                "1vccbDYZVu1f3Uzfc7vyyu6SvXf49avaIsq/h+5/Kqr/ZJyt5n" +
                "Fos+kqmurPbV4nELL9eu13HLfSiquLYsq9p15e4LFHeTU+3LQ+" +
                "+XtD/tV6kqy1I2IOsAkfbzOvwHeGxB1UobOW4j4KBWorFf1ybY" +
                "AbsuBzPFPEGSDUB7oP24zdBi3CZTI9d7Qvf+jc68eyLa40/Eyr" +
                "1/QPPul6d53jm8ajf4JMOdd91HVNp9JF2v6pKPMn59UpZ7XY9L" +
                "oGlL5VVZkjUY2duPa02Zn1vPXSGvyhO0QXE3JbKfIO/LlTlQHH" +
                "yTI4tGjXSUuR5QIyrunjWs/e6+cnGw8kbb296nI31K99PssEzP" +
                "h4VhqVlOXhtWP60uxtn25kqG3C8D6YBKVVmWajenA3x9AgRcAW" +
                "WWpAXXHmOOcM3NgLf9ujb52TMvzMJsg+sNJHJ9Am6YLfhy28Sv" +
                "LMofFLH+oI76B2Wp9p1skPeTlKhakOOyvQfb9hjzw0ozG2xu5S" +
                "lJrKcNg7YPsNIzz/Rmyskkk4/pDSS8n2q3glXFGiNNLlAmfyLs" +
                "npVsIfnOQJ4h28k2sptJT5LnRM0LIj1I/km/yK6vkzfJW+QYyx" +
                "0nb/csJCdpGz1brU96bTqPfjC39xT5K/kLW7v3kpfoF8jL5AB5" +
                "hdX+g7xq3QMN0g/Qs+hIei7THkPH0QvoheQx8njempPkz+Rphn" +
                "qW/J3sZNddZA/ZS/aR/bKfyGGWvkH+zXRH0NGsnx6m59DxdAJt" +
                "JyxyIVvJ31i6I/f0PHlRz8+HySFW8xoZIEfJv1juhEC8Q94l/y" +
                "Wn6Bn0TNGasfT8rsNdzIdKZU6WavN5CjJ8xTpQhlbLGpXj+d59" +
                "ComtmRq2D8DUFvhZ2AxVmjxu2wN+2DPHuf585drZauw5+92PdW" +
                "6ROUbz2tHWPcs5KO/ErZV1BnYMO0fiuaqtjvA9vWtus+c1Zu3u" +
                "d5V1tbNE/HBui919RfhZoTk/HU19D1h5uvUbFbzf4X5yj9795S" +
                "Mn1E/by8UtinvLfvpNmT0vGStPczzVP8XOy11cMnYo+2rvQMhz" +
                "OId+jz1mvWIs61wN2heyZOHuCmPclibj5Ck09RMn6r6NFjiFLH" +
                "fU93m5jIvl0HjaYdYrxrLO1ajvDVmy+umeMMZsKV3LThbVq/2O" +
                "6ucfdI1jlUV89GvkTav2wULj6W0Py9X0BtjvdO03PP20iyHYXK" +
                "L352heZn1Bbwo8LxCrIZ3Qciat4/tdYDafol83a7rekqew/Yb+" +
                "TS62NSVOIcsdyT2+WrDly4WwwFjWuRr1jpilkFVbYsu6jstT9J" +
                "OOCivbHE2BU8iSsfZ5Xi7HYzlTF9crxrLO1ZDcizANY9yWGvcF" +
                "ug8r2307UXy3jPTT2Pje5suZuqXuC7aHdk5r5p8oc1/QdUKeQj" +
                "OXpRPTiWz8TnNxClkyjpzo/c1OxHIhLDCWda6G9NaaKX0vjDFb" +
                "Go7v6t3ZYD0tFt/hEemP7xqT7PisTHxnR5Mt47s1vvjOHSccVy" +
                "y+S4+lLFaTqfgljsm/njvSiqwDBKv9lZLLP8AL6a9zfMW2x64V" +
                "V0vKTb+2TYUw9VwOVlrxycETGnnHAIt9QZ0sh+LgtDPtJM/V5x" +
                "hx8LdZnREHs1TEwfRmHgen+VvotNOOg4W1l1hpiHEwk5eKgxmD" +
                "QnEwKx1izArEwe48UaOvfqtvXTPHZzjSMOcTm3eXFfnKzH1C45" +
                "vJeP7BOm7Mu032bPOvz8kmezbba4CjPVumELdUzzBlgPHHdybW" +
                "PhpfifbS7JCeH8NzwMrVTR6yuZhtAFzYr5+T6snqZH2vNt/eqc" +
                "O/djyKFTv1Q3Gt1nu4uapjVp7vxPb77iRcftWLit4XZLOyWZDK" +
                "nCzVV2SzTIQpd8u2VZXj+ca1ComtmRq2D4zxs7AZqjTZYNsDft" +
                "gqx7n+fGXN41sypboP6yvZ+XGQASb6qwek3TvjWnGr9VdNDM8B" +
                "K1dXesP1PM+f+5rY6sVhv35O2RyZ0ts1t1+YMsCELFdnhKWNb0" +
                "b7aU7Yqg/Dc8DK1U022kzNNgAu7NdnF/YGmPnd5LTud/P+n/sd" +
                "515kvzOtufeZ4RUUrNfvtbm763nPwlbvW/R4+pl9V2/ej9u/gH" +
                "9n9t2/qPtx4xuSR+x2+3s/eSS+A4F+pb3S3rVKpiymETlZqm/g" +
                "qaxh+1U7IOHPLHfJ7/ralQ7rnduVBCPBGq/lqarBUvCgePlZyF" +
                "rVBpkubtocJS/liVvM92FkjZdkHbaelydXJhv7tyiL8yopgVQh" +
                "zXplAduxrbIeuwOjQcvnWclDGJePzSBZa3M0+QEO2/bczWh9Gb" +
                "ewsTVXPR8n29lI3s2kqXp/x2XkIJMtkUginr0Q8XTGfn8n7UDc" +
                "ovvpXoEOxC3Z3Gwuj1uYpnjy78YtEsPjlpzDHnbu0/Xi/RGPW/" +
                "L2r6fncIn8voDHLQyzA7dT4aDM4xaHE4pb6FZ2Pkn76Ta6Cb9H" +
                "oIgjFc8pquhJMN0l0o3epxXPOjWbGzuH83afbohKnTdDyR+9uM" +
                "d8OPpMYRb4+bjupyUj3Ofj9ID7fLyQh4ON3d76gs/H6T75fFyj" +
                "Wz0ff7Tg8/FHyzwfFxpL4Mz7qRNkyS3iegsdwAhffIelReq1h6" +
                "icvm5iMFPFzvWGbQK6qN84JxhP6c894+k/4fecLcbTgcJjzzee" +
                "3in3njOpFXy7USs7M5KlcObjqQvL5H2EGE9LW9kpU6+kIPfdx4" +
                "jxhDCYabLU009LbZ+ADvmNcUK/iVifes5F61O3Zpl/X9Azpjqe" +
                "Id+MjSf6A986Xp3QOBgdQz8i0bv16hi+PtEf2uuT0P1+1XmXk/" +
                "Tb6xP9Xn79rokjh0LruH99cufdktTDd3yReeftpyG9zdL6H4pK" +
                "3X56oOAseqDMfmf4vFDnPtx22o7qxOaktvftqF7gPF94qZhm/e" +
                "XS61MTznw8fQnLxHWmWJ+aESszsTSZadqPeQc51jLWJ4xRbGeK" +
                "+pm+1pg+dRuaIb8xTqx0lMU6x+SpynZOv9ESuK5jlr2jRX6H5n" +
                "QPk6Ngy5dzvStfirHEuhoSF7LkZ+D6i+lXL9M2nP8aSa5u6ffq" +
                "UKlZ+D+TWnuxsa5GsrXgjP1Y6XnXgDOfd9dhmbhmuOR/ioSleI" +
                "ePaXEpyKWXFpicqXi30nA1JBL71G1ohGzG/MnveoyVQH/XU/2E" +
                "o/lZ7+72YAiBS/XDHk3vV+F+L7nG/T6sq1Ev+LV69ZOFx9GRUD" +
                "lx/p8huaaVBROBS83O1r5jXgJsr/FrJEeG1voWT57R076q/tKw" +
                "eqUfN8TvVcaFn/aGcqZume9VkjNbPY9WuCF/xwr3mdeHWzWEfh" +
                "rWd6zxw/P/LQW9VdOyvrIFMoX3LUtu1O+lFpiY1nY8/fRKXCtu" +
                "VbyXQhieA1aurvSG63levJcysNWrw34tu/8DaXjzEQ==");
            
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
            final int rows = 65;
            final int cols = 74;
            final int compressedBytes = 3081;
            final int uncompressedBytes = 19241;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNq9W11sHFcVXhJaIqqE/ChJSSmN2pS3VgEeCAoqu56dhhdAUB" +
                "AgoIiiloSG/JTYDyDw7jjy7HodfsVPqJyYtpFQpUpIPFSAREn6" +
                "UEAB4ZrEUSJECQgKcZsGpYGAcbl3zpw559xz7+w69nZXc+ac83" +
                "3n7+7MeHd2XanIR/29qKWfrCzho/O2xURTV72h1Vd6y9srjx7x" +
                "PpDNrxXrdK/EiBPKkbwvjI6+UF49nNXHsRp1pWNHZ91O5QxFx+" +
                "8P1/X3FN8PMvlAsU67Tb2LhBGndJoAOvBceVR51tGXJMdq1JWO" +
                "hWrcb/XkmNtfck+4rr+nWpu2fJ32cKyIPs8tlfu85Mr84UetXY" +
                "67HOzUVqi1eR1ejedEXdYpq+vHaodoy9dpL8eyfT35CGd41mme" +
                "o/G8zF+yBocIr9U9Z/RHHU7eqa1g/HXfNLJmMcOhUN2ynthMgy" +
                "CTjxfrdEhixKGokUF/niLHV4vr+I7S824wHux6fWIcq1FXOjb5" +
                "mNuLnKHgfSpc199TvB9k8plixm9IjDil0wTQznvKo8qzJvdLjt" +
                "WoKx2b3Of2ImcoeA+E65b31LylWKdvevpd3cvfzuazKm5N54HF" +
                "vC9IdpWiK9U502Ney2v+ttcuatfD5qzThJ+HzAXNuWf0UqhyWG" +
                "Px+6UfOwafjhi9N5Qp3IFGXKx2HWzZOhXX3oEjpt5lzUOm52//" +
                "kXA/foxy+bQQlzoGn46AauFOi/X/UpgTmjT5SrZO+fvMZCQZXt" +
                "L341+o9O3R2KRm+XKlz4+kVZx3k2qFd3Q9ineErM6Bnq8CnirJ" +
                "WBlXRwz0+Kok49e6TnTepY+a8+6t8t3RIo6nL/b8CcpTZfTfCz" +
                "ueBryvin0/7ni+tdA5auO05et0jGP5Kl7gVijPQvyIluPNWcnh" +
                "ndbG9ToBxnMSu9e6fix+CCT9vUu+LTHilB4NAbTz+/Ko8qwux2" +
                "rUlY4dUJ3KGbr3262n5HuFdrj7UTgy2ON14PudP/bxmvpdtU77" +
                "Kn1+sPsqP1zSv3ezi1qJiYVdn0av9mt94gMgkyeKdXpcYsQJ5n" +
                "g5jFqsrHp8oHuHxLEadaVjoRr3yxmK9f9RyTQib213bTdJ0MBK" +
                "f2klZ0hc225W1LJsy5HJs8kItwbn+LtwO0SZLnPzUX+ysq+ez4" +
                "5eicS9T7Dtlp4DhCQyQUc/aW4Wcd69wNm+KKqMuM5CHuTKXopz" +
                "4QbZO+8+9PDhrJu5aM5omcx8c/A0tZ4HHzFoTyxpAV/nq1Tqn9" +
                "VRyOZ1dc5uXXAMpa4mK7GVmOMzUy0+E+6rx+yGz/wO+zH0Ao5e" +
                "bkmdUPByLuXlUVRD4jonSR6Hlr2OU01iulLOh8xwZR8SP5hf2b" +
                "YV17gfu5jPCuVxH+na8qjyrC7Hamj5YtM1bi/E7q1fN290OjqN" +
                "Em2w0svR6fRlzqA9saQFGXQ+k22jjkI2r6tzIi/UhZwhr7bBh/" +
                "OJaX4+M9XiM2XaTDRjtExmvhl4mvWcjGbiScTQi7jPxgw6n7li" +
                "fFBHIZvX1Tmxl1AXvEOUupqsxNZphs+MPcmZMu1MdMZomcx8Z+" +
                "BpenssOhM/hhh6EffZmEHnM6/wTToK2byuzom8UBccQ5lu8uFU" +
                "ia3TGT4z1eIz2X08Lz+fg223NEdIIpM8I4OWDxjP42Y1r/Ae9C" +
                "O7UXPvE1AN4Oks5OH9NDbxTqGa7J2zF3KfguIbJxpPZ/pOvO/b" +
                "+J2pbM7L1sZGtpqNsxZr/NlgdwCz8c/Mn93rbPynWWleZ7A7my" +
                "soT3Nlc51zZX1zxn6ucb7xJ7P/S+N50c/OeGdzefO1JvKGLP4N" +
                "zQ3NjY1fNI5LTuPZxnTew4zZ/lD4/5Fl/1dR7ebm6y3SXJ/5n2" +
                "mcNJwpPidlLT79XHIRE78s62ZVc210KjpljqxMZsfYKXiadXoT" +
                "+IhBe2JJC/g6n3mF9+ooZPO6Ome3LjiGUleTldh5d4rPTLX4TJ" +
                "k2FZl1Bpn5puBp1ulm8BGD9sSSFvB1PvMK36KjkM3r6pzduuAY" +
                "ynSND6dKbJ2m+MxUi8+UadOROZJBZr5peJp1ug18xKA9saQFfJ" +
                "2vUhl4Skchm9fVOaPpgafKuuAYSl1NVmLrNM1npon5TJl2Ljon" +
                "PtFktt1abwGEJDLJMzJo+YBx1M1qOj+O2RF3GVQZcfQMHJcc3Q" +
                "/vFKrJ3jlbXDVv4/U9n+/OyarmqrVLvR//mYs5zJ96vzcPfCPZ" +
                "uVj6t2ZXXPJNZnq7y7EaWr7YdIvbC7F767dbT2ydfr6U97cGTv" +
                "Qv1vM9wonKEj+iw9FhlGiD1dqGCO2jMcQdf47mGcYQZXFjviir" +
                "ybpuTmRwRDOUHPPhfOJcH+Mz8VrkkxFF5KOote6SttQ9K16Kpn" +
                "cuNKJ3lmakdyz58fRw9DBKtMFqfQ4R2o8cRVz6c3QyzzCMKIsf" +
                "9kVZTdZ1cwKj9SBHNEPJYR/OJ871YeqWODJ7bk9EE0bLZOabgK" +
                "fpbS8itI9aiDv+HM0ztBBlcS1flNVkXTcnMjiiGUq2fDhVKtap" +
                "xWfitcgnI/SnnNZ+91NPPB8lZZ+PEPWxyiKX/vcFvVazvN640W" +
                "Q0iRJtsFpDiNA+Ooi448/RPMNBRBEf+Q6P5FXcum5OZHBEM5Q8" +
                "6MP5xLl+kM/Ea5FPRhTf3xXfB7e+ntnbGfbO0m/+VhTffjY8r8" +
                "cnXtXjqcdq6dtNtz39Lic6Gh1FiTZYrQKhfZQi7vhzNM+QIsri" +
                "Ul+U1WRdNycyIl83R3n3QqY+nE+c6ymfidciH0VUr8DmvM9U79" +
                "WqV6LR6hVketZ8VO45on1YOayxd79PSz92DD4dAdXCnRLPzhT4" +
                "HuFKWTxbp2fMcfkOJ2/pr7wR1ayoHbUXc2al2xZ43nmr6d/1WF" +
                "5vnUVHoiMo0QarNYmI3hNLWpCB2KTFf/VFAZvX1Tkpj78LOUOo" +
                "mqzE59fduDPJiHzNTxbab5byWpu+69W8jqfbl/z3F/kd5/qt7l" +
                "3q1g/i+fQueX88vhq6P06o0a6qu+5XMdK9P451y++Pp+8O3R+3" +
                "8fr+uO1A3x/X553tquha3R+v30rx9a31rcaTyQzbCk+zTsfARw" +
                "zaE0tawNf5zKQf0lHI5nV1zm5dcAxleo8Pp0psLbbymakWnynT" +
                "ttSzezUg7d5qdmud5Ah6pWdk0PJlBsoiXpvV6IdMOooqI+7Jso" +
                "X3QJvKtVr2zrsX5+eHeW51PG1h3WyubzZaJjPfZniadZoCHzFo" +
                "TyxpAV/nM/tVOgrZvK7O2a0LjhVylQ+nSmwtNvOZqRafSUbkV+" +
                "/id5QJ+1aito/kNV7HP92/67juqw/3VZ6MnkSJNlitafARg/bE" +
                "khbwiU1aep+OQjavq3N260LOkFfb7sP5xDQ/n5n3LGdQ6/YT1N" +
                "rLw1jxSW4wjEq7flP/jifdF1bTyKKO27WwOe/Hz/p5tbXXUqO+" +
                "KVQ5rIW41DH4dARU66XTMEdOGs1GsyjRBqu9EnzEoD2xpAV8nc" +
                "90/kYdhWxeV+fs1oWcIVRNVuLz85mpFp9JRqjj6Rp+8V27O2Sl" +
                "n+/LFfxuXbU/1/HqbHWWJGhgtVdVZyVD4laODHLbzYqa1TsvIZ" +
                "NnkxFuDc7xd+F2iLK2zs1H/fGslqfree0L1QtGyyVoYLXXWUkY" +
                "36Nm1onZbOoLaCHauYRMnk1GuDU4x9+F2yHK2no3H/XHs1qeru" +
                "ezi086v25fb2Xy9+Lv3esI43u0Ap8XA2jnv+VRENde0Z2DVagr" +
                "XTP5m9uLnKHgzYan8eU13l+BpP+jTi5KjDilEwfQzlx5VHlWl2" +
                "M16krHpvvdXuQMBe+hcF2Zt/pi9UWSoIHVXm8lZ0hc225W1Kze" +
                "+R8yeTYZ4dbgHH8Xbocoa5vcfNQfz2p5up7Pji5Hl81fvkxmfw" +
                "Mvw9Os0wbwEYP2xJIW8HW+SmW8oqOQzevqnN264BjK2o0+nCoV" +
                "63Qjz8Zr8Zl4RHwWJHtfMCcx4vjfj0uu+xh/Tek5dTYU5+dYjb" +
                "rSsbWNbi9yBuKF6/ryxkPxkJW0mRy3+znEiIdgncAKVBuCiPFl" +
                "ZSzKGMoHWRDFrDKK+rO9c4Y/k85GmdDnv6vJf0cbPwKbe7+R33" +
                "UM3QmUUe7dRj/PlQJ9xO2Q9wH3fXl/6XD4V77uL5PlXVbf3dTs" +
                "8X85bDVK");
            
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
            final int rows = 65;
            final int cols = 74;
            final int compressedBytes = 2856;
            final int uncompressedBytes = 19241;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrVW2usXFUVnhpBMT64gRgkNqYxGv8ZfhgTDTpzZk6Vy6XQ9i" +
                "JQfIAQCSaixjQp8Ydn7q4dJ9FyrQ+wNdG2NooGUR7FC6QVyxuB" +
                "RAVSb6P8EWOgoGkUiVo466yzzlpr77XPnLlzbyNnZ+/Za33fWu" +
                "vb557XPG6rBVt2b3ZfMWk5sB6HWefqltqyv7hV+fhc9nw+His8" +
                "/0Kkf1IrumUPZo9kDxezP7dqtux4kel1xfhGA38oe7Sc/SHvT2" +
                "RPZUfy1z8VnjOL8Wj2Yh57cv8U1N4/tT+Ve+8PMh3OFoX1TN7/" +
                "lj2b/b3Vmns5+2fh+3f2Uvbf7H/9VaGO9DCO7jXkGWzVGHPq1l" +
                "uPxqNGx0kOzFhVGDtwvha9hiZ6bU39/Xk/mKMHK961rf+bbduD" +
                "dWj/0WCNK6ad9pN7fVXry6/i/bTi2tPfV8fu91uvkg2vT+q827" +
                "Xs++WK9AoecVZa+/LXfZLh4YHtZ6UZzJMfEVNm0xF+DcmxVSDu" +
                "67GqsT5d2apn2SX7eHocRu6t1nB9fryfJTKWHGbgyK/m3+E4Ri" +
                "R761g6Y8jc9h/MQihlRR8eT1JfslcrxW1uH/NkXcrG9akFR27x" +
                "XODeLJ8L0t3YxXPBW5b+XNDbNclzQbp7vOeC3q6lPBfks+hzQe" +
                "/03uk0ko3W8LPoYwa/MktbyA/z5a/Xh1HElnXDnKNU6DXEqulK" +
                "cv1yzVxLrqmYre6tlnsbbejDUxDhkZjaTxlkHj9r7tkp2RylY4" +
                "ZvkFltDmvgua+gt9PXqPVZm4XL+P4Deb+/v7//ePFccBpxhl+s" +
                "vRc/UYwHTOx3gedQ74aJnlruHO+5wK7W/7XpfWxJit5Rvn+ZH3" +
                "7bxzpXjYrWDGn1vtdUwegqPjeMaF6t6dY92j1KI9loJe9CH43M" +
                "lDFkEU9mlXEcqaNkhF9DcnScxn09qD1U6msj1ZKptcj8tLnV8n" +
                "jKa70n3Ktzp9bvdfdWed65t1dxU5P9Nd0Zdejcm3yPpT16BtWe" +
                "d+5t7kyzZvUMNrglOMpn3JoR58FM3Gp8Ls2Myw0j3DtX7P3KIz" +
                "gme6q/yR7shDEnlgPYMZTz6jlljmflzMyBGasKY7Vumlv64nV1" +
                "3mRNsoZHnKE13Jms0QyNh7aflWYwH9xKTJlNR/g1JMdW4SukcT" +
                "Dl52N9urJVz7KrvfdbHN27q3tGojHm1B6XEXRwe31UfVafAzNW" +
                "FcYObvO16DWM1uvnTbM04xFnaA1vglEyNB7aflaawXywn5gym4" +
                "7wa0iOrcJXSONgys/H+nRlq55lu3Z+1esW17516vnp4sEd4rp4" +
                "iX+/c5d6V84ri/Fs92GX4P3OpW7tqPud2+g2uU+4y92nhU99ju" +
                "A+5KbpfufOLX3nCfx8tx7ud+6iwvp4MX7KvVcwOsXYK8aPuo+U" +
                "3nPcjHW/c7PuQmV/0l2m7hw7uJfP4zcz1imezctxR+0daMd4/q" +
                "rCjpH3NsGRSkldWE3mZHbTujbWmede7qdfSEwza1YzP56f0Hrc" +
                "50ilnfnwczrEZE5mN60bYslL0KmRD2buc4iVkZ+RFkVKSzLAYk" +
                "QzpVfXhSqhNneNzEFxyMU6VFNW88e5fTIHMS1ltub2H6FTy/fO" +
                "F9BmrFzBedIiVFqSARYjmim9XJeqhNp0DopDLtZhtVq7jUjbUm" +
                "Zr7r4POjXy4QyxcgXrpEWotCQDLEY0U3p1XagSatM5KA65WIfV" +
                "au02Im1LWY3mGe7sIV/n85ppzX2Pxro170a6M9DqcGIxnzNqdb" +
                "52PYaq4nV9TH/+xO+Dh79s8P50c8P3mocm/NZszM+fBgvNP39q" +
                "rCG2n25Zzs/pJlHY+cfY++nOlfqcrnsBd/bkR8yX2NfZwH7JES" +
                "vaIOORbzN1bWicw9KGLOajF7i6jo7wR1+HrGup8u6Az0OnRj6c" +
                "IaaZOtLPQ17N00zp1XVDDufT2tiC5ydZU2q3EWnHK2uk/SR0bG" +
                "iV591ty3d9aj+ZnDzJmceqmqF2NYN30lgqFqFTy1e/BW3GyjPi" +
                "UmkRKi3JAIsRzZRerktVQm06B8UhF+uwWq3dRqRtKbM0R6/j9y" +
                "zndTx57USf+1473nXcrjbJdTx9On2aRrLRGh5EHzP4lZrbLC3K" +
                "EOaTmWU2ZNsoW51jdSr0GnDsHLNwuWJev1yzr1TugfwIOwKdWn" +
                "nUlTPEyjPiEmkRKi3JAIsRzZReXReqaA7n09qgAxfrsFqt3Uak" +
                "bSmzNSfPQadGPpwhVq7gAmkRKi3JAIsRzZReXReqhNqK98GeNu" +
                "jAxTqsVmu3EWlbymzNycvQqZEPZ4iVKzhfWoRKSzLAYkQzpVfX" +
                "hSqhNp2D4pCLdVit1m4j0raU2Zq7a6FTIx/OECtXsJE8MlI9Z2" +
                "4Ej8sQQ77N5Bq6LlXR2qQijkMu1mG1WruNSNt9JfKcudbX3P4r" +
                "dGr5OufQZqxcway0CJWWZIDFiGZKL9elKqE2nYPikIt1WK3Wbi" +
                "PStpRZmmPPBd3vLOtzwaoT+lywaiXe37mv5ntl2g3zfl13uvTN" +
                "4wxG8nU2kafal9PeebeJPd1p5NvMosbX3Dfy8evdaWicQ3G2Ui" +
                "xzcAYduFjHfbNgF59ouwFr16PbVq1vu85pnHcBljwLnRr5cIZY" +
                "uYKLpUWotCQDLEY0U3p1XagSatM5KA65WIfVau02Im1LWVxzdw" +
                "N39uhZZ7326zkyZBTybaauDY1zWNqQVR4L3yKlwNV1dIQ/+jpk" +
                "XUuV53k/dGrkwxli5Qo+Ji1CpSUZYFVnz3c1U9bQdaFKqE0q4j" +
                "jkYh1Wq7XbiLQtZZpbeT4InRr5cIZYuYILpUWotCQDrBhT1tB1" +
                "oUqoTSriOORiHVartduItC1lmlvdAQ9Dp5b/9a9Hm7FyBRdJi1" +
                "BpSQZYjLSN/xVBL9elKqE2nYPikIt1WK3WbiPSbkf+w8VH2r+C" +
                "jg2t8jrW4HdIzF4e3tLiQ9TWPrGKZ6BTy4+nG9BmTDN1pJ+HvJ" +
                "qnmdLLdePadA6Kkxr0aPn8HGTHK2sk9pyZnNHgO4gT8j3C+N+3" +
                "2Nonfc5M4JefR6mRD2eIaaaO9POQV/M0U3p1XVubzkFxUoMeLZ" +
                "+fg+x4ZY20F6BjQ6v8bufuBmfsQsMze2HCK8PCeKg7ayVUFHfB" +
                "We7s8We+pZGmUcGqdnVn63DEJEcqtSIposi+U7J8VfG6Pha7Pg" +
                "0OLvX6ZPBO9PfBB5pfnxof0w9Ax4ZWWeveZrFNvJh7Mo3joIND" +
                "k6iNHmFt6NTIhzPEqjPkB9LS8xz9oeRrTFuyhq5ra5OKOE5m1q" +
                "Pl83OQHa9sal7HnT2+j/3h3PfEo8IYaHU4sZjPGa1I1q7HUFW8" +
                "boglL0CnRj6YuT2IaaaO9POQV/M0U3p13ZAD3e2VOShOatCj5S" +
                "t+T/eCH1VX2ULSLb1ZGmHrzfZmGZOvZEW+D9wCsaG/N1sfVZ/V" +
                "58CMVYWxWE369RpCfxNN7qZiLH7vxPe7tPgVsruxGMvftLq7Rn" +
                "wy+mP3E3oed3e4n5Pf/l20u9X9LB+93zE44942+v+A3M2CvT89" +
                "V1g/HXEvrf8/oNudeleYLuLYq/4jmGfpoubU/tVNtLd7xLGyWJ" +
                "8V4iUHZqwqjMV60q/XMEpvXFN6BEc+ntw9GmNO7YpNdO6aEfvp" +
                "SH1W9xvNgRmrCmPdAV+LXsMovXbe9ovYi6Pwuoq3NXjSKHjEXI" +
                "6Nc1mzGJcVoy+MQO1NlMY54Uo7U9j19Wlwnx+JvM4S/ucwdjxx" +
                "LmsW47Ji9IUR7gN1mWJZfcTCOtu54zZ8TGKaWenZbOUxq26vVb" +
                "u9Hvc5UqkViT6J2Oy6uj4W/f3TU7V3iTG/50xPO5Hv7+xqE33+" +
                "9AoikKZH");
            
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
            final int rows = 65;
            final int cols = 74;
            final int compressedBytes = 2313;
            final int uncompressedBytes = 19241;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrVWmmMFEUUHvHgEtjlEEGUCERjjMcfDat/dobuDaKixsgmIp" +
                "KIbkxUlGPxAjIzXbo6EyMKgf8SFTUgRgFFBAyR1R9oJPEMxkQN" +
                "RoPCD5HltqdrXr9X53RP93RiT7q66r3vve/Vm66q7prx9pf6c7" +
                "nS3tLW0lelXX5tcq5+vPhLznKUvgnKnVrdfkWyxzmRS3CUtlu1" +
                "+2SJnq20Wyv9spmInNlhnv7IpXi4w5NYY1TRtMnYrP3Yx0tyP/" +
                "0p6hDT2I/ynf3qjrdb2b2WBkRMrYZRqbacjcrFPjSK1xxT/mU8" +
                "+VEZRHUi0nyYtI2s7HoZQyPVWXIZ1ejRNl5R58x0ZkIJbd6qXM" +
                "BliMArosQWx6O/ymDQuGNVK0BTXtVnoyjEPpjYRCbaf9pn5KJ9" +
                "ohbFzypDa1fvYHg/DdPnufh3M+Pa+92uL54JOC3zSpGMgqK/ih" +
                "QPmLF9Xwejtb1xXMWDRs3p0jxsdfkfKKHNW5X2LgnRRfS6NnhQ" +
                "/fnf8CjVCtA5wU9OYbBHkdOUKpvIRPuf03J1CTF2qTNmfR73Vl" +
                "VGqTkut0VaxevPBd5AaNfuHItiWTH49y62WZVHKCvgseh3uv25" +
                "wPvH+1drFa53lcnpraZ+ns4msY+dp7O5Fh8kT7ekej8NZJqngb" +
                "TvJ6fb6YYS2rzlrne63fUUgVdEiS3uQfXne7tAtQI05VV9Qiym" +
                "KMQ+mNhEJpLRbtpniEnsE7+a3lsqd6b63nIq0/eWU616b8m/gm" +
                "c9T/dRnYi0+4kjB61dL2NopDpLLqMaPdrGq9e5P/GyPB0kbLio" +
                "Q4z1DcSgdQfZrexeZUythlGptpyNysU+NI63UUw47py7Gs/jXm" +
                "+0cVdud89NMu7YhfHm8Ths0cYdm8LaWPDcyqZJ690igpog54ld" +
                "ovU2EvPExrCxJE+WZ2N2Gbs8ynrHNKsem8rzxMYR2aWUjQXPgW" +
                "y0ZDeeTYySJzaJhRnpPM5PKU/zZBuOA2TM/YiROin60tXI8+f9" +
                "ohwi5jLVgrNFidSMUXvaOcDP2lFYA1KsibhO47OJakEi18cyYK" +
                "uZsBgxl6kWrtWTyauskXX5Nn4G91O4l8CuUlaAAJc3PmuWr7bs" +
                "pZ3RrilttpoJixFzmWrB2fIRnorNGLWn+cH8FPNUUFZFjgOk5n" +
                "6yrO2u1gp96WomLEbMZaqFO9jmyeRV1si6zhP8DPqKeVJ2sDiu" +
                "07jTXbDstbnaFQt96WomLEbMZaoFZ+uMsCdvxqg9zd/Iz2BluS" +
                "KcOxcrGQ5wgFSfC9g1ljxp39DQl65mwmLEXKZacDaTJ5NXWSPr" +
                "Ok/yM7gn1oV3xzolwwEOkJr7aV3cVRB96WomLEbMZaoFu9bmye" +
                "RV1si6/A38DPq6Nuz1WiXDAQ6QmjytjZsn9KWrkX5fL8ohYi5T" +
                "Ldh1Jk/mCFQN1RUmFSZhyWu8VVlaKylC1Ktt2SvUanXnJCCpN9" +
                "FC5qAYfRRyhFCqbBifyKzj07Ud//D9BmWwotYPP0/LuQwReIXD" +
                "66Ut8KD682eMCRSHeJlX5gCEOQqqg1JlE5nIE4RD+4xctE+iRf" +
                "2O7QhrN6W3+8dudi9q3d4im67M4y1gM+4/vZTm/pN7Tpb7T3q2" +
                "JPtPbA6bxWaz8PcpFu7O9R2I/b3eZtTcze5JdMfcatXeIUuenx" +
                "bR7+3NRlTeGN5Pr0bYt+6N/B58fi7Do/VsLNwlqPSlmqfzMs1T" +
                "y9jyq/Gs52k11YlIu584ctDa9TKGRqqz5DKq0aNtvPaYyDy+Nt" +
                "VveGiiu7zHqn0wXTYtx8OlfvaYdr3TvIWwR5td75zTWa53ejZ1" +
                "vWOPNPs/sfLcME8bJZ/P1sriISJZ0shbZROr7x6749jTEfIR7G" +
                "Sw5ZpvaHHDb7xYvz5TY/OvK4PWQgW3jD0lSVb4/ToS1B6vS5ay" +
                "JzWzcZAR7z1p3G0LpG8F5eZ6FhvsfHlvehvgfvK2eZvC7Lc7ml" +
                "/pvfe9d/zyA0mquXca/x7svUvQWymb93aD78b+e/AW70Mt54Iw" +
                "T/tSnZ/achkerWfD/czKoTT9OsezzFPr2ci4+yvVb3hIpvfTkA" +
                "zzpJlRmv+/ijsiSVRx/68Sh625/z8VetRa8iOpr7j2acbeaH6q" +
                "Rnj2j/HeMjpJVOVFMcfd6OzGHducntfywoT2MfPENrUiN4U3ai" +
                "d8QAZSrqdI0VL2A1KKRb/UCjlEvS426kOMCtmw1Mn8bL9OfQDS" +
                "zGzSBN/ElnDcDUt13A3LZXi0no1tC/PUlmaenjuSZZ769rdgRj" +
                "Ls+1atc2Hsfd8NWb4H69mS7PvOODrjKJTQ5q3qKC6DEpFY83pp" +
                "W/ZK7fq+Q2u8ihYyB8WIdqKe9qHO9q3sD+MTmeU+ibFQ/+Gasp" +
                "I+F5S96phUR8IPmY6771N/E+pwOgqHeenP8UGNt6pjnQ7vNS7h" +
                "OEDiR2zXPNSQYAP+axKK7PuRSmsl4CgXMvjz4HpzFGIfVDbsgb" +
                "/e1ZlqPQ3WtMPUW02HMYMMLEz3E9uZ+98e5RUtZ3ghnMenJPdW" +
                "nRret3sz3S9Inc2Z48yBEtq8Vb2SyxCBV/h4vbQFHlR//vVjik" +
                "O8zCtzAMIchdgHE5vIRPtP+4xctE9Brcfx3xl5Gch6+Mdb5eS5" +
                "DBF4DVG9tAUeVH/+dTfFIV7mlTkAYY6C6sJyt06PTCRPxBvlon" +
                "0SLcT3O29VNcIvuNGfM50vMh13LWPLr8GzPrfcS3Ui0pyn/Bqz" +
                "fxu7XS9jaKQ6Sy6jGj3axivqnFnOLCihzVvVuVyGCLzCxx93pA" +
                "UeVH/+9VOKQ7zMK3MAwhyF2AcTm8hE+0/7jFy0T0FtvjPfrwVl" +
                "IJvPP/64m8dliMArfMQ2eFD9+dc9qhWgKa/qs1EUVBeWe3R6ZC" +
                "J5It4oF+2TaFF/Mwr/581+TnXG+KR1s1FxYpZsyjz+QKrz+OeZ" +
                "zuMtYDPuFzyk2XX5rdn9AmdXlvsFerYk+wXOEmcJlNDmreoCLk" +
                "MEXuHjz+OkBR5Uf/71I4pDvMwrcwDCHIXYBxObyET7T/uMXLRP" +
                "ooW8P159ItWRsCPTcbcjq3Hnz0+Lku/TVReH464/03HXH23csU" +
                "PN/l+F3E+9qX7D2yO9Ny/Lki3W8R8VXbSS");
            
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
            final int rows = 65;
            final int cols = 74;
            final int compressedBytes = 2242;
            final int uncompressedBytes = 19241;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrVW+mPFEUUb0XjQUBUFIMJHrj6gQSzEI+sCcxOd8fwP6iJ8U" +
                "xM/EZWYyLdOzvs6ERZQzAmxggaiCZIjB/84AfEkw8owioCHgnC" +
                "ikfEWY2RQ1nt7jev33tV1T0zO9WdMJ2pqvfe751TVVvdM+sf84" +
                "85jh+1zRHHgVF81SaaT8QS4CCf5CbacQCPOmTPcbyPdS1Ec7+6" +
                "zU5RcBm2ujfpyUlf3Br3xXOCPtwdvT8N3w2/CN+PRtehheaTTs" +
                "4rPJC0O42ySY3zkfeh08crfC9X+rnKMXsLdxm5e7uMYbeg0jp5" +
                "b806q0mdN3a2nzqN/ZUrPa3V6a3u6jR2qrc6GedT0+p82lnqfN" +
                "rZTZ3qc7qfT97F3sXYIg1UcwPwCEE9oSQFeN1e1H+iayGa+9Vt" +
                "dopC5pDlTXri+fOcyRfPSWq09ebpo3PvNX7Uvs2sdTd+xOa66z" +
                "PGHtfd+pu625/qV3e/Pym20jrVF1n9hH8sdT59XbSH0TvTOi22" +
                "abe6r8w62ffmHnYPY4s0UN524BGCekJJCvC6PW6ZWwO0WUpUfh" +
                "QyB4pdl/OMKX+esxopo6fcKVG3hI7ahdE1NbYtHgMfR5wD+LbG" +
                "lGpF2F2oojXEFPkAOWHGp1S7Mh4eEXhTY4zb0a25M2Yqi6fHKt" +
                "bdkmj+HrS2Eg6WqW3G9xODe9w9ji3SQFWfAR4hqCeUpACv2+OW" +
                "uTVAm6VE5Uchc6DYdTnPmPLnOauRct+Z5/GXbJ4Lxk+WeS4Y/9" +
                "v2/Z27xd2CLdJANV9Bid7jVVvLKbRAaBr52zmOj6Rf1QcisqOQ" +
                "OWR5k554/no0ak5So623I51PGyUtx/Cqrc2T8pe/rbhTgO65CG" +
                "+Z626zzXXnv13mujN76++5SrTrtbCttvDvXbVVj87+tddAJpFZ" +
                "NFIqKuZw3vhpzpUyc2y11/OikDmoFmk8upXGptEsz+M325y1/o" +
                "F+tOu3lOnNuLbfcd/BFmmgmptQovd4Rfs4o9ACoWnkH+I4PpJ+" +
                "VR+IyI5C5pDlTXri+evRqDlJDfU+uPli5yrzfbzDJ3zQKfFVhL" +
                "es577Nl2dbJ9Nz3/F/y3zuO/6P7ee+3nJvOfScjt/+Ji5BruQA" +
                "XlrgVk3ewJKuRZ5RrlshDmLRmrSlxs6jz65Edm102egD6WeS8e" +
                "S/fkcRK6V+W0fErWw8GL1vz5nn3yaoFV34zaxefaWo2YA3AD2n" +
                "k/d+LkGu5NTWxnhpgVvVPqEBtK9rkWeU61aIk0Y5oOfgOI3zZO" +
                "w8+sz5NGCKFvvgw+AjuT8F+xwn+DqSHggOxZzgm6Q9Fr1/Akzw" +
                "Z9Ime0ZwJnTCCxNN9qw9nBdeaYolOBIcDX6I+qngZ233mBNeEL" +
                "Vzk/Fl4dXhomBX8IGiPxl82R5FO3Xwfcr/NWnTe0jvQHhpYueq" +
                "hL87+Cxq9+fXKfjDsKOdn7Tzwyu8QS+aw9AmHgbhikaTwCME9Q" +
                "wlKMDr9rhlbg3QZilR+VFwGbaN801y8sTmzKDIWYmUaH/Gn0nG" +
                "S9s7YEL7M+4CdwFIqIUR5wCe09yKOKct4Oi4DYa5X/JMON0KcR" +
                "Ab6weL1QjU2Hn0mWcJTe4t5frmfdxd466xuY9nWStmHwdvVvfx" +
                "Fd4KbJEGqnoH8AhBPaEkBXjdHrfMrQHaLCUqPwqZA8Wuy3nGlD" +
                "/PWY2U+856XtCYY/N5QZ9n4R6fF9SOFvG8QGjdjaPhIeecfdmP" +
                "3d3j7sEWaaCG7wIeIagnlKQAT+jGXJLoWog2S4nKj0LmQLHrcp" +
                "4x5c9zViPlvjOfq1Rsfh7e72XOpyK8Zd0HN+bP2uKk7Rh7vQ9u" +
                "zLN9H+y23Ba2SCfXUHS1JCLhtvjF8Im0jR9S7UX9kK4FcsWvYh" +
                "MRUk+PQWmHTHKeMct/yOSLeKqGtu48qythutR1Z91b5UTlBLUw" +
                "ii93lbsqpjki5qJc8mHkrmp/RquAg9rxmDRJS41DSiVG6km5Go" +
                "/JG2RHaLLLsWRPt6/Mp1/S53Rbz91zQd3v8tuA32brgZ4/1e+2" +
                "uhJapa67wrwFn7RPsul379XBDOSs/ubW7+ngf6ZjhOzUHUR3A8" +
                "F32ViIPby8i7yPZ0rOhveyk+vK4ZXUwii+3NXu6uGVEhFzUS75" +
                "MHJXt/en1cBB7XhMmqQlTtCaVGKknpSr8Zi8QXaEJrscS/Z0+8" +
                "rnfn8712XuMsMpYlnuGWNZFspsrbhXt97qD3SPVTQfTvfxN8rf" +
                "f5tvZkT1SI9ZPNQl7tGuzwXTlWlqYRRftQnXr0xLRMR9FeWSD6" +
                "Pa5van6QMHteMx8LhuZVqNQ0olRupJuRqPyRtkR2iyy7FkT6Nb" +
                "lehvA7Ywii93wB2otOLfHRIi5qJc8mHktp/EA6odB7MmdSvib5" +
                "Iu5ZjGYqkn5Wo8GIEaKfzuUHrmOXF7uv3M8/hjNldU49oy16/9" +
                "30W7J92T2CINVHMH8AhBPaEkBXjdXlSn63QtRHO/us1OUcgc2t" +
                "6WmOQ8Y8qf50y+eE6k4Y/4I3FL76hOyv9mIYYQ/gh8bw5UxtP5" +
                "EdCoXpKHIotZ9sAKStGq1KL4qpfISM2WdGtkCXmZ53Fad4/bnL" +
                "fVuWWuu+K9+ekzmsb15+793dhFhdfpq/Qs85mF81B6r9G4ocw6" +
                "rb+v8Dvt9Jc6zb027XrPl3ofXLg33J9qE/UnrEY+UWqdJkqs01" +
                "NWI3+h1DoV7q3+dFqndVYjb5Zap8K9hRuwTs0u/ne0+99nekGp" +
                "dSrcG34vFdXp+GzrpH8vNXq5N9pPVLVrcvcK7VuoXrz1+3+vTc" +
                "N3ZqMLZl2ndaXWaV3hdaJ197fVdbeh1HW3ocR1d9JencJjXq2v" +
                "qE71WKda4XWi+XS61zqFG3Mif67U+WTdW/XG6o3Uwgio5pm45Q" +
                "gpj9vaWk6rVnEUj70xRHJrUkP1wTHmKNQIsdW9UXzSs8mfic6Z" +
                "TzM9z8W8+fRsqfPJujfvQe9BbJGOr6hO/wGPENTjJWm0oNuL+o" +
                "auhWjuV7fZKQqZQ5Y36YnnT9a4L55T0v8PQptBxg==");
            
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
            final int rows = 65;
            final int cols = 74;
            final int compressedBytes = 1297;
            final int uncompressedBytes = 19241;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrVW81r1EAUX/ALBEEPgie9WFCpFxUPipiNuwqigih+IOJV8S" +
                "QiogcldtNOsaD01osH0Rb8pgdBD1oQtRcVBBEPXlr1on+AFAU3" +
                "Tt/OvJmXNNnMTCcJmc6befN+v/3lzSTZTWs1vEVroDa0oDbn1n" +
                "ehlnNrXKk53GygRW/bx5voafQheil06rs5tEj3bX3rjPr0v3xB" +
                "RvyotbxqDJXi+Dyz952mE4kWTZCt73Mpf65xDkqwk72t00LeJj" +
                "zE347XBdmCCHq89t8B2U/4q7gqBniks8CfIQ0NI8mfX0STseTP" +
                "hEdo826x0ZnQ73TeWUeT5t0So8wHneo06G59onzjO12vT8NO16" +
                "fhfOtTfDvv+pSeT4bPcOw0n4yjhXvDvaLktWRPdEps2UPux+0w" +
                "GkeFWlJvROApR8MjVAzZh2ahMoRSRxP8MDKFR9laPt2CGltbq+" +
                "zWN2U8n1aGK0XJa9zituyB+3VbjQq1pF7/A55yNDxCxZB9aBYq" +
                "QyjZejWe4IeRKTzKTs8ns1t9xmU+DTxzdV9gXKe/LnViG0xHjB" +
                "8o9v0OVq/tTxPfsxW5tcl0xGZ7EyWvzVojSSl7KP2arUaFmoiD" +
                "o6g8VAzZh2ahMuyUI2o8wQ8jU3iUrZ3jx50VbnV1r3fmuTejZi" +
                "RKXuMW25iUsgfu1201KtREHBxF5aFiyD40C5UhlK0tajzBDyNT" +
                "eJStreOjnXNytcL5ZJ17dKODdbDCOh10pxPbXF2dWqHDfOqtcD" +
                "71OsynLRXOp6Y7nbq4dq7rrs/8lhete1ZldGrty3iS2OZSp/47" +
                "ORnvn5d86umuz0I+9Zj1M/sc3Ey9C46mm6XukKPfpph055d5vd" +
                "tR4eudde6STtsrrNN2dzqxnRW+LzjhMJ+OVjifjjrMp6DC+XTK" +
                "r/vM/O+rsKZLnVjD5/vxTOZ7nOq0251O9cnC7A5o+T9AzorCv9" +
                "aygvfNxbmX0OmryXnndsvDff7mXXjZm+vdZZ91Cr75opN9JqV0" +
                "euSNTo+81mnKG52mXOgUjKfij2eyG7fFqvmnoE7jPudTeNGbdf" +
                "yi1/PunjfzzgIT+v3MOUcR72cGs7+5U+9nluRY8P3M4DHpN0G2" +
                "vnc+77y567TPpIxO7LAvOrFDXq9PD71Znx660ImlfsvFjmSyG/" +
                "VGJ+tMstfxa8sz2U2nrePXVpRj1bcqk9WyNCa5PnHX6zg7nppP" +
                "xzJ1+uJNPn1xMe+6ZjfmjU5jXuv0xBudnviskz9bvd+sX5ZO9d" +
                "cV1um1u3xiJ6urU+u68Zl8PjgvSl7jFrdlD9yv22pUqCX1+kLw" +
                "lKPhESqG7EOzUBlCqaMJfhiZwqNsVbd42s4ZZmecPrecto0Qf7" +
                "d0BXL6/y120HYtTQ7YoS2pxT94H/bEI9U40Ir9sKfcinFpbjgG" +
                "jJM54JJqU2OAnY6c1qM/t4TGsqtcpKKjaf9yHMx9TxeeTXu+c/" +
                "09HTBR/Ca06+KvIs934UxywA5tvMb7sCceqcaBVuwXzlC4MnIW" +
                "NxwDxskccEm1tZ+X78oxwDMdOcz438r4V4mrzDz9jhD/1JhY+Z" +
                "5u19bkgB3aeI33YU88Uo0DrdgPe8qtGJfmhmPAOJkDLqk2NQbY" +
                "6ci4J5hMDr5zazbvMt8q4X4Bei8ERgSTundQ6g2S7NF6L82djp" +
                "KXmbl1vNNHrOPskst1fGBtvnW8yPd0wefk4Du3Zs9J/1yjZG95" +
                "BG6lWwrm0+divTR3OkpObv8AtrzsUQ==");
            
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
            final int rows = 65;
            final int cols = 74;
            final int compressedBytes = 1580;
            final int uncompressedBytes = 19241;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrVWs1vG0UUtwQVBwT0Q0KIVOUCZ/4E9sOGU68IxKEVH1cuwA" +
                "EEtLbjZK0ekECqOJUASkWkthyaFClJnQ+ToDaiSEi9k3Ckolwq" +
                "kCo+PHk7zMzO7O57frtTZ0ZvvO/7t88z41nbrd3WbqPR2h9FE1" +
                "eSa+1GS1InpVLv4mUEO54eWY8G1m6t4hqNaCkfhY5QjmCf1Yu7" +
                "0bHpaFUu+57gtf3DiLbb19o/tddGV8/ICMlMo6C1b++PA6fuZ0" +
                "sybLBae7lQ+2NW0t1z2q07pbfGQdS7o3PRYmMiWjGO3m84e97d" +
                "5M2nKJyc+dR9nzaf3Ni58yn6UpDsUgZXoDMtTc9sHCk17UxLXW" +
                "rmdWMzY0g/HYM5umTZGJLPz2xqgmuCoAOX2r1UXmFlXWaHtRwn" +
                "j611Y2di+E4QdODSffwczheXA2s5Th5bOz3koKXtTyjfAdJumP" +
                "R9ft4lJ/D703jN/Lwr3V/fm4RPQ/vzLkmqzxJ8Lwg6cGmuT3G+" +
                "GCnE5mGkaN3nJyzanCxDQdCBgxa+i/PFSCE2o05DmtaNHYs2J8" +
                "t1QdCBS+fTbZwvRgqxGXW6TtP2HuegzcmyLQg6cOl78izOt0ja" +
                "e0VKdMvey+Q6bdO0buxlaEswrAuCDlya6wTOFyOF2Iz5tE7Tur" +
                "Fj0eZkGQiCDly67v7A+WKkEJtRpwFN23uVg9bX+cn1fJd85vP8" +
                "NPtk9c93nur0+UGvU7ApCDpwhPWwiZNCbMa626Rpkz85aHOyrA" +
                "iCDhwB/wpOCrEZdVqhacM38XZYZMGaIOjAEfCv4aQQm1GnNZo2" +
                "fANvh0UWbAiCDhwB/wZOCrEZddqgacPX8XZYZMGyIOjAEfAv46" +
                "QQm1GnZZo2+Qdp9y/+joNVQdCBI+BfxUkhNqNOqzRteApvh0UW" +
                "bAmCDhwB/xZOCrEZddqiacO38HZYZMENQdCBgxadLPPSrW2dKX" +
                "Fbout0g6Z1Y8eirfKcSf29JZzyec5Mfn+w5/HuF0Xn8e4cvMZn" +
                "7TrZsjrr5M5m10nYYZEFVwVBBw5a60Ocb1GTMSA2Y91dpWnd2H" +
                "kYMk/aNX0/HjG+9y3zdfwejMyWLExaneLXGrU1u07YbAn5aSo+" +
                "qUhJsjIlt6+zknwv20f0YmxgpexVRJenwm6ONqr8vFldsCMIOn" +
                "DpGv+4cE/YUaNbp8eA2Iz9aYemdWMvQ1uC4aYg6MCluT4q89Kt" +
                "bZ0eA2Iz6nSTpnVjL0M7Eeen417PT4Tfg/H/V4lDQbJLGVyBTj" +
                "8/xaHpaZ6flL2pi0NXXj1zETYdkfLTI5ujS5aNIfn8zKbGz/8L" +
                "Wqs+55M7G+//BcGiIOjApbnO4HwLz5lnpF2wyMVI0bqxMzEsCY" +
                "IOHM03r4Uf6Ha0uNlIxd62Vs+NQ4van/ZJdikTVzMPxaletzQ9" +
                "G5Y2tnRxxlKXmnnd2MwYsYEjdoxxiUbn8zPHRWv9//0pfKfK87" +
                "jfhsFO3iNz9vHmrSrPBTOP+NzH3djtfXz6zrj/i25/Us/zXfOo" +
                "z/lUfzZt3T1fXZ06b/NQdYm/kmKwEys/15yTo+QlJ0dpoV6Vlc" +
                "lJr2w8PbIeTeZwaRU3Ol//nY/CvAcYw9MuvX7HaT1P69H0Ozbr" +
                "oDzGW3f41j/kc931H/a3j1f7fNdi/a+HfB4f1PF8596fKn0vfu" +
                "0/yvL/i2Y/e9/fPl5tnZq/+KwTL1v1dcJ/3jX3vJ4L9g7qfOo/" +
                "4XXdHTmw6+6813V3vu46TS/Vse5GJ5XLXp/vLj/Y+dS+P+58Ch" +
                "fqm09nn7bqtHBQ1134rc91x8vGr1PnMCqGdc7sHOmzdtbuU4Wo" +
                "HrPO44cJdzxR58zwitf5dKX2+VTT8114yes+Xku26K4co7uyTi" +
                "DrfgWvpmUeL7mslZDosv5RXWrq3Ni6XxehMO8hG1Fdd+bVtetq" +
                "Is5P97yen+7Vvu5qqdPoueUb1j5O/J6Ol80Z8ULzghwlLzk5Sg" +
                "v1qqxMTnpl441e520vlUOPk41ZhsK8h7xsZib9/vV7VrnMOuge" +
                "Nc+ni17n00Vf6y4+Fh8b7bhRVfuTiOZvf3Jn68zXUKepeGr0tF" +
                "fZ+xKz/u/bf6GKbLw6+fnel/P73exz1O99+y867datp37873f/" +
                "ASvKQHc=");
            
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
            final int rows = 65;
            final int cols = 74;
            final int compressedBytes = 1721;
            final int uncompressedBytes = 19241;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrdW01oJFUQ7s3Fk+CKBw+CCeslBxPEo4hJT3cwetJBSeYUdU" +
                "FFWFDIRbzMTDKzTXB3XfGUoCK6uAdPy0bxsLoIioK7sLB4W5Ek" +
                "IKyiiMieFru7UlNV79X0dE/3vGTSTapfvar6vno1r//eTPyu3/" +
                "U8f19CCzTQycaPPIZ0r7chAou7hp4cTUaYHNxHz8LMsCctNspP" +
                "Mmt8mo7+87/Rn7nJPq61V7N9B/ejNdtu+vBM+2fMLbp3Fq9pa/" +
                "4Q/33f3G5eb34Ttx72cm3Nm6m8otpuWD3fRU97Jbbm15nWn82e" +
                "7iOq37dq77U8GYRb4RZK1FFDiR50xL29yjVEsPE4MkdDDs1Kmn" +
                "81Kws5BpD+Vc3ORwxbFHI0PmJZB4roVVfMJ/9Lr6KtHFLRaN2/" +
                "TA6duSLnXeepYc+7zpMuz7uoXvl5dyu8RRJaoIHOPbhd0xERYz" +
                "A6afsbdhRxcBwTc1AW5hh0NsnEx2+PQdOt6n6YOZ/CYWdE9Izn" +
                "cIsWR83Q/Gc0uOEXLutUPVt4PjxPElqggc49pD2R8f2O6SYqtp" +
                "L2+t/oydFkhMnBffQszAxRRs+beJSfZNb4NN3VfHK7hWer9XP3" +
                "nFlu3J1ni93vwo1897vEL9/9LrgUXEKJOmoo0YOO5CU1jDLxOD" +
                "JHQw7NSlp2FnIMIKNlzc5HvH+9b3A0ziXrQBH6c2Zld4edkvF3" +
                "Cp5356r1U2b4c0fi+vRetX7xE9mKv0ISWqCBzj2k3dZNVGwRjk" +
                "Qx8zA5uI+ehZkhyvBdE4/y46iJn82n6dYMv9s7d1fGdz6dvrdq" +
                "xGAxWESJOmjRy9BHHnQkL6mBv43HkTkaeOtW0rKzkGPYz/0lzc" +
                "5HTOPnYzYz5dzG9emFo3B96tRz+r2YF3Hh2MIxlKiD5t8PfeRB" +
                "R/KSGvjbeByZo4G3biUtOws5BsrdtvMR0/j5mM1MmT6xMBG3Up" +
                "n2TcAecz0AfeRBR/KSGvjbeByZo4G3biUtOwtuQ5nkbtuJidVp" +
                "go/ZzJRzu3h+ah0P/iwT334wE926apdjy/VEN6o63XZap9vu6u" +
                "Q/XmWdysUXrVOVuQ8zn1r35cK4ocynd5zOpwJs+d6Draj9lYba" +
                "bG3W89YuSGvS139Dq+0FaCXWJ08W89fZWp/l9z2o61P0hsv51D" +
                "1e9QiCvWAPJerJXjtROxHsRa9yj6QX7bCTP1gBAbw4HqCZUWCX" +
                "vCYmeLRPyTg7BylttkTCfKLceLbEZaKbEaOcT6636K0RXLn7rG" +
                "fOX1f4m8XWM9e6uJ7ZuYfsa+uFcyz4/Z2Wu7aeufZH7vXMnWAH" +
                "JeqooUQPOpIX18JpjDLxPC+ctqOIg+NIzMFZyDH0Y5NMvXWVaY" +
                "7GuWQd4n0pWIpbqUz7lmCHNvZgP9k1PZzCKBMvzmjKjiIOjiMx" +
                "B2fBbShtNsnUq9MUR+Ncsg7x3ggacSuVaV8DdmhjD/aTXdPDSY" +
                "wy8eKMJu0o4uA4EnNwFtyG0maTTL06TXI0ziXrEO/LwXLcSmXa" +
                "tww7tLEH+8mu6eEMRpl4cUYzdhRxcByJOTgLbkNps0mmXp1mOB" +
                "rnknWgiN6zyEOFnmtWc68vn3R5v6uezZ/1Z0lCCzTQuYe027qJ" +
                "iq3U+gp6cjQZYXJwHz0LM8OetNgoP8ms8Wl6//nkNyr9PBpO51" +
                "PlbMFCsIASddD8ZegjDzqSl9TA38YDNDMKvTmvjTkoCzmGfmyJ" +
                "jN4mJBo/HzNx8THJCPN5fP7HsusF7TsHta6i5T7cekH73/Z/We" +
                "sFMddPyvN4J/Nt4bT1vNvV/Io/jxfdtNzj/CrkLfZ+l/d+5/p7" +
                "8/W/3K3Tzf9SIarjOlWZe/XrT3OfH5Y69c/kUNTp4qGp00V3dY" +
                "reH9/zbm3jMM+nrDoFX7msUzm2A63TttM6bY9tnS47rdPlEYxh" +
                "DH7HWnTdd+5uvnXfxG/I7+9GNJ+iLZfzSf+/snE47/xfXdapHF" +
                "v5Og3/vXn0kcv34O6jVb0HOz/vPnZ63j0xrudd9OkRuz6J//jo" +
                "PFYVrn+zTHTn9yrY9N9hHK755Pa9xX9zbM+7C0f1ucBvFT63Wo" +
                "dmPrUc1qk2xuddreq6+HW/ThJaoKXtXe4h7bZuomIrte6iJ0eT" +
                "ESYHQ9nVs+jlWTekxYaj4bmBZvNpuvXJ9d6M/E3Piz4pVPPN4W" +
                "w5njNPFfz0N6u+3/ln/DMkoQUa6NxD2m3dRMVW0g5eQ0+OJiNM" +
                "Du6jZ2FmiNJmo/wks8an6VbdztmtjM95VY9VVjpe9xxuo2fzP7" +
                "BbxWOVzOtO6zRytubZQteN/L9XueKyTnnZCmT1P0m/NIc=");
            
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
            final int rows = 65;
            final int cols = 74;
            final int compressedBytes = 1883;
            final int uncompressedBytes = 19241;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrVW02IHFUQXiJ4FTEoEmMwBoUgbi4h5KI7r2dAzJ8xJCgLCm" +
                "pU8Ac85CIJOjszvczk4C3oSQnmEF1XUBQR0WAkMZGo+BdPe9BD" +
                "CJgcoqIeDHZPbW39vOre7ukfd3qYN1Wvqr7ve29e93T39k5MyM" +
                "295ltZt7SK/kcTNW6z11fN4F71reStc8CuNXBPFdB0qqqK/ifZ" +
                "Mduno/ep9oftb9qfRda6tNzem0tVPw7bT03E77yek+5EgXk60f" +
                "44dQTn/Aozz+sNn4t6vx5F08xknmy5nlLHerbAPJ2tvqLaecq+" +
                "9U9M1LjNbigbsXWkdYRasMADn2fIuO8vrbKXEYHqgscwk6NJHZ" +
                "qD59gqtEJsfTbSx1H7n7eOdF7SfJbffk/tw/N5Z7r9fqbv44JR" +
                "+U5urnczfvsXSv99W+PWUAsWeODzDLcm6FCc94MVdAATsgg/tq" +
                "mSqrQOGZU5sk7GtR6LDUeDHinluYSn/XzrKTictp4w6mcFh83K" +
                "ytaTxZaUly232Wq2sEUfPWwxo9kKehjHLO4FPciHLI4Xqen5Vc" +
                "TBcSSmpcLP0K3PJpmW5qnHx8S55DxEr+nmdGQN22HfNLzAxh7s" +
                "77yOccziXucNyA/aGi9S1PariIPjSExLhZ+hW59NMi3NUxvVai" +
                "45D81pd8gdivbAxRYs8MCnGP9Eq3OA++xocwg9jAaPYCZHkxWa" +
                "g+fYKrRCbH020sdR+2csPtM/6A5G1mILFnjgU4x/ohXNE/PZqA" +
                "+ih9HgYczkaLJCc/AcW4VWiK3PRvoks8Vn+cFCsCCObEMf+oIF" +
                "N082+rwH8iFGUTevUd08xihbZmhmfEGPm+c5pEf2EmKsQGok9V" +
                "IXxzaO8wuSlf2irKvofPzceJ+Pu11uF7VggQc+z5Bx39eoaMV2" +
                "8ChmcjRZoTl4jq1CK8TWZyN9ktnis/z09eTm1KzOpc75XFKWm0" +
                "uvXPbbnCsj39KVH3txnl6paL/7qdb9rnQ2t9FtpBYs8MDnGTLu" +
                "+xoVrdju/4yZHE1WaA6eY6vQCrGdOa/xSJ9ktvgsv6711Lyuzv" +
                "VUPlt3X/d+4T9Q4B7NnYkse5JjmVRuS4uGR7MrGS0P5qm7s3vf" +
                "aPMk72d2tyfPU/OG6uap6/0aZWXr7si8BibhRX6B9TSZxlLobu" +
                "JkvmhWtuyqWpvgRX6B0WxKYyk0T5vyRbOyZVdV5vEphWVPwfpt" +
                "+fIbq8vNK3p8Ut/O2pTj+No6j09Z2UZXRecFjbuNs8UrqX+r2a" +
                "l7eg9Zeb19VZ8XWNoLnmdudVupBQu8xu1xyzNk3Pc1KlqEI1G0" +
                "Ds3Bc2wVWiG2sXZfqdYGns9n+Snrab2xnv4ck/W03rx2+r08hp" +
                "ndEytw6/+Vc56mys2L1tRet5dasMADn2fIuO9rVLQIR6JoHZqD" +
                "59gqtEJsG07jkT6OGuf5fJY/Husp9353b7l5wyNSwvMqjaeNI8" +
                "vIz6sUG3fe51Us7dbzKr2j2Z9XSZqn/tXxnafZTrZ5yv9cT+MS" +
                "valHW75nYVi5vN9HaFxKRiVkiYaeVQl9FqfWlzYaE/kXeluxJC" +
                "89N3tVelzncKXJinnEzk7j1bGk/a71onGF8Ox47HeWdmu/6z6T" +
                "e7+7TG/q0ZbvWRhWLu/3ERqXk1EJWaKhZ1VCn8Wp9aWNRsbyPJ" +
                "/Jqlb279092dZTnFf09868Mh95vxusrnOe7Oeii+13NT3ve02d" +
                "82SzFTt/UlgV/d284H26J1OjT3jXg39nvG78p/j5+Fgfn4KMx6" +
                "dgxe13q2rd71aVfT4eng9Ph1/gPIU/hGfy6Lefsw+9v1qHX7lr" +
                "i8xT+GVq1PtesrGF3w7b70e6/zS9Aq/+/6i+YlnETqNDLVjgDW" +
                "5qdGSGjPu+RkWLcCSK1qE5eI6tQivEdnCjxiN9ktnis/zk9TS4" +
                "eXzvP82erJoh/HXpGunf8Z2n6rXPPL7EtWuM56l07c2LzYvYog" +
                "9eawf0UQZ9Upb0IN/H48gcDbLtKHnpKuQYSLsf5yOm8fMxa6Wc" +
                "O+l8vIy1O7hlcZU+X3CVv/B/73fJ50+DW0fGNM6fCqrMef40yP" +
                "T38CLnT2w9PTjGx6cKtCfez9w9vtd3tnbjvspvxe8XtLaX+Cta" +
                "9/Fpe9Ur1j3lW2Wi1lNfpvZFxP1uP7VggQc+z+Bxy0dErMHq2B" +
                "7c5lcRB8fRmMup0GOI29kNVpyPmMbvj8Hyh5/H4ze+FuuPYy/E" +
                "sZd70qYo9PJcwuVVxCHjPia1vI6jydbqQyZZlcacFBkenwo8Px" +
                "6mnLUM7qj1+q78/wNqu/bUMWzBAi+OTR2DHsjjcV6JflwTb1iD" +
                "+IQDr9YV6o2zgUsicx2EqFUAixyDZqNaYooRUR/PJTzte7+UH1" +
                "R0RnO81vOn0tncZreZWrDAA59nyLjva1S0Yrv1FmZyNFmhOXiO" +
                "rUIrxNZnI32S2eKzfO/4dLX4fV/zG3671vVUOltzS3MLtuijhy" +
                "1m0CdlSQ+rNF50HL/LryIOjqMxl1MhxwDt7AYrzkdM4+djJi45" +
                "D9Hnf/SgVqk=");
            
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
            final int rows = 65;
            final int cols = 74;
            final int compressedBytes = 2234;
            final int uncompressedBytes = 19241;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrlW01sFVUUHgIh0MToiiZsWNDEVUNIS4Ir3rszrzGhOxeIJt" +
                "g2atzAxpRIiMbyqC1vw96QkBjFBVQrPyZigiQkSmNbURTixh2h" +
                "7todJMY4c8+cd777M8P0vXljq3PDmXvO+c53vnuZ9950XhsdjA" +
                "4GQaRtciSzZIS7w93RwdYg5ygqeZ/PDC5fECRsdhWjsa/Lybgs" +
                "FZhj63YzOwXtA9mwF66JzlM/xP++n/p66qep7+LZnqDQMfWbtr" +
                "e8uV+cyB11P+jimLqZm12yI/5uU7e90eWCGjL2SbXK3KcgqHSf" +
                "WuXvk3mcmTX6fROUdHTHtN5qP74bDeqQOiSWZuQl89YBRJh517" +
                "dZeaazPzMS2cwKuwdi/CpYp6nH102vZtjURp7bz+s3VCOepZZm" +
                "5JEvOTxjjfjQvcEeZxufMxLZzAq7B2L8KmyFbN1uos/s7Ovn8/" +
                "Nfd886mpNFkY0vggqPXnSr5vOu8VWV7+P+bmV+3s0836N9elDp" +
                "Pj0oe5/UMXVMLM3Ii+4nFhFmPrHNSfRtVp4Jj8li67B7IMavwl" +
                "bINtFu8ok+s7Ovn893dvfv9p3qr8GmPcrXHh2ODrNlP409oJgg" +
                "5MyjOYkeM7h8yIxshPZnxctXYa5BtLt5XLGs31izpRT8sWgsnm" +
                "mrY2M04tlDiglCzoAyPMK7fMiMbIT2Z8XLV4G5tn3oy0sn2Kcx" +
                "Y82WUvAnool4pq2OTdAIgtYhiglCzoIyPcK7fMiMbIT2Z8XLV4" +
                "E5trPP+fLSCfZpAtdsKwV/PBqPZ9rq2DiNIKi/RDFByFlQpkd4" +
                "lw+ZkY3Q/qx4+SowxzbR7ualE+zTOK7ZVgr+aDQaz7TVsVEaNO" +
                "cIxyXv85nB5UNmZOMevqx4+Sowx/bcUV9eOvEutWrIhr3MfYhG" +
                "R7aNbAsCssmRzNhjywg5C8r0uMrmixU13CrpgTw257NUYI7tzI" +
                "AvL53kekI27GXuw8i2cDmM77PIJkcy02MwHsutEc61o8s4AK+z" +
                "KcMgZ6Fu0K2ivNXX4iRE84RZ52qw7KAvL51kn3BNjDHZU38pjO" +
                "9myerYUjoG4rHEkXZ8oJ1fMuJpNsUPcBbqBtwqylt9LU5GmHWu" +
                "BssO+PLSCfYJ1oS9JGZXpPeZe3pz56f6qrzP7H238GZnOTdr+q" +
                "pSzaqQ4o34vEDtqPS5745eP/ft5nXX2NtZrgfPn/aWi6vueqr2" +
                "e4T67SLX0/TWBNft9y0lv+662Cl1Z92vuzvFXnczL260far4er" +
                "pWbJ8S3Mbap/C1KvfJ381zPR3o9T6ltbcK4qq+nm4UvJ5udPAN" +
                "5wea63zqNWeizfL08sPdzlrer+77lvq9Tfz+dK/X909n5jpfTe" +
                "vIRrnCZl/pdQfZp/qPm/d7hPK1RzujnWzZJ6/+B8UEIWdBmR7h" +
                "XT5kRjZC+7Pi5asw1yDa3TyuWNaPa7aVYu9q7gtar1f5/nTuhb" +
                "Lfn6Lt0XbXp5htGUlzjstMncxixThXI7vbmUeWWsaaWtr34ydN" +
                "7ag+eyeyYlpJX9QXz7TVsT4aNOcIxyXv89VprrL5kBnZuIcvK1" +
                "6+CsyxVad9eekEO9GHa7aVih8+Ch8Zd7KxP3uEYrZlpBknhsTW" +
                "LiGL90kRoG0EZwSXhXH1oKLkqF2yNWI24ynWo6wY1bvvT7NHO3" +
                "t/qv2+Ue6fWEl570/hSrjCln322DJCzoJCT53iKpsPmZGNe/iy" +
                "4uWrMNdAVp3y5XHFsn5cs60U/Mfh43imrY49pkFzjnBc8j6/do" +
                "WrbD5kRjbu4cuKl68Cc2xrV3x56QT79BjXbCvF3tb19WW2b+ds" +
                "pHrv37mrdHWVryS8Hl5nyz57nHHPgkKvdtmucmdmFaGzsojIVm" +
                "GugWztsi+PK5b1u2pMdrsirZvP9u1cfmWF15PTub614M83W9fx" +
                "WfJffJ65peDzpy2FP++uhdfYss8eZ9yzoEzPrpJZ7S9fFaGxr8" +
                "spPH4V5hqyupmdcP2uGntNZkW65z36Prj1RqXPVd4p/XnBvmgf" +
                "ndHnmFqROWLQ2gzICp9AK4j2VZmdeWA18jKW2UwutWJr5NXk7U" +
                "T23sRjKBqKZ9rq2BCNuNfFaKj1Fuc4ynmfry4Sg8tHbHYVo7Gv" +
                "zUmI5olsFaiQrdvN7AR7MYRrZk1Ynfr9Ub+xh9pP/qkLlBHLSD" +
                "PODFG/umCyGNfTBURLla8z57Mwrh5URN1sjZjNuJ76s2JJfbgY" +
                "LsbvVNrq96xFGjRP/g5IEHIWlOlRlaBlpr51qxiNfV1O4fGrwB" +
                "xbt1tiz3wmTO338UVcM2q21rAQLsQzbXVsgQbN9T61EXIWlOlR" +
                "laBlpm66VYzGvi6n8PhVYI6t2y2x6T61lZpqzV7mPsRjNVyNZ9" +
                "rq2CqNuFeTYoKQs6DQU03Cu3zIjGyE9mfFy1eBObaJdjcvnWCf" +
                "VnHNtlLw18K1eKatjq3RiF+VNYoJQs48mpPoMYPLh8zIRmh/Vr" +
                "x8FZhj2zruy0sn2Kc1XLOtVPz6cH04vi9NLc3IUy/X9d/1CcLM" +
                "uz7c5w6zx1lBIptZYfdAjF+FrZBtot1Vamsjz+3n89f3c8v0px" +
                "1/j/BulT+3zAwU+7ll+pNqfk+s+N8pqrud75K6e/btvPzZN8vs" +
                "9r/6OXhXwZ+Dd3X/+yqtjzfvPk0fLbZPHx3v+PcL/uzR626hi9" +
                "fdQu8rNso+heOVPrkrvVt9f32/WJqRRz4izHxim5Po26w8S+bh" +
                "EUYim1lh90CMX4WtkK3bTfSZnX39fL7ziXq+R9fTq5VeT6V3q6" +
                "3V1sTSjLxkrq4iIvElj3GaJWj97nCVGaROKqXK1mFmEcOMLsJd" +
                "Q2LdbqIPOyc4xAqf7YdPw6fx/mur/yee0qC5muccRdU85xmFnp" +
                "rnKptPzWMlRylv9rU5CcGMWQjbut14NbhWwgmW1Zjsqf8kfBLP" +
                "tNWxJzRoruY4R1E1x3lGoafmuMrmU3NYyVHKm31tTkIwYxbCtm" +
                "63xKbPC9raCCdYVmOypxX/ADMziGY=");
            
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
            final int rows = 65;
            final int cols = 74;
            final int compressedBytes = 2065;
            final int uncompressedBytes = 19241;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrdW02IHFUQnltIRBRCAnpIEELYi9FjDjnMTM+QQw4ikqvgPX" +
                "iRoJ7MZNZMdggoKP4k7kZiEn9CEn+jCGqiqCC6ohLwh8SIh1XQ" +
                "S06CoM6btzVVX1W93p6dnmZjP17Nq6+++qq6t6en52drNdwO7q" +
                "RV7+naDbs1biuXZ7fOwkhjQ5md957jdf/Y1I/Thunothfbi8Hy" +
                "HOzNvM9hRrT8mFJGdoqnrVedoqSKWdxf/wXs1FfSalKJsRXPp/" +
                "U2eqi96vPp+Uqfd+tLP5P2tfexjavoNe4MVjIwbn2tSivWQRXd" +
                "h64hOX4XukOyoXfbqe4terae59fS1/HjN+51fO6vYrz+ydVWEM" +
                "fpxA38end/ubxarbWltYUs+dFr3BExZvAjs9CLfKsnlaVaZPtR" +
                "9vK7wH3g3m1c7jHvv9xn3ams3fl8MD/rXOh83fmoVutuHB3ru3" +
                "PvHy4P7Ydu7FuDfDLZ+dF5Pzf6lTlP3N47F110sWAPcJw6Wwtm" +
                "jXmc2h9UeZz8apMdJ5W1dUpXjNcqvT5Nodo451Pv/GrPp8aZSp" +
                "93Z4qdT72z455PjWd5erGUl88tnpUf1xzZabpjGfHZeXX9WOMZ" +
                "nl4s5eVzi2flxzVHdpruWEZ8dl7d/J6mdX3qX6ry+tTbNg3V5t" +
                "9h0iAsrGZPxxgyMVPrEIo8ZEoU6/q99RalBuXJHtB62OAe+pTU" +
                "IGa6so5kN4VJg7C4ijFkYqbWIRR5yJQo1rUc1sPe2LPWw7QG+e" +
                "nKOlK/FiYNwuIqxpCJmVqHUOQhU6JY13JYD3tjz1oP0xrkpyvr" +
                "SP3NMGkQFlcxhkzM1DqEIg+ZEsW6lsN62Bt71nqY1iA/XVlH6l" +
                "fDpEFYXMUYMjFT6xCKPGRKFOtaDuthb+xZ62Fag/x0ZR2p/xIm" +
                "DcLiKsaQiZlah1DkIVOiWNdyWA97Y89aD9Ma5KcrY6Sa93f9T6" +
                "u8H/fvCyZ9f9f8J0wahMVVjCETM7UOochDpkSxrt8balCe7AGt" +
                "h2kN8tOVvUh2D09GNMa4XWsknWVzwsiLE4v5rOhlcu9obVfpun" +
                "4s28uTEb3SHkaKZumteyzbmxePMcmRnXqZlDFUPypZuqt0XRur" +
                "/xwmDcLiKsaQiZlah1DkIVOiWNdyWA97Y89aD9Ma5KcrY6Si6/" +
                "gXk1zHu4+uhet445EwaRAWVzGGTMzUOoQiD5kSxbp+b6hBebIH" +
                "tB6mNchPV9aRrBkmDcLiKsZGf9fj0sP1IPqi5GMMPVkD61oO62" +
                "Fv7FnrYVqD/HRlHan/HiYNwuIqxpCJmVqHUOQhU6JY13JYD3tj" +
                "z1oP0xrkpyvrSOPhMGkQFlcxhkzM1DqEIg+ZEsW6lsN62Bt71n" +
                "qY1iA/XVlHst1h0iAsrmJMs701I90DNmaZVAPrenrUCfbGnrUe" +
                "pjXI7z6Wquz2vIfn8FrzZFxJjJneWiPpLJsTRl6cWMxnRS+Te0" +
                "dru0rX9WPZvTwZGRyvhyTGuF1rJJ1lc8LIixOL+azoZVKGtrar" +
                "dF0by3aGSYOwuIoxZGKm1iHUxkwnO2XlRLdCD3tjz1oP0xrkpy" +
                "vrSP1KmDQIi6sYQyZmah1CkYdMiWJdy2E97I09az1Ma5Cfrqwj" +
                "9d/CpEFYXMUYMjFT6xCKPGRKFOtaDuthb+xZ62Fag/x0ZS+S3c" +
                "eTEb3SHkaKZtmcMPLixFp+jXlKduplUoa2tqt0XRvLdoVJg7C4" +
                "ijFkYqbWIdTGTCe7ZOVEt0IPe2PPWg/TGuSnK6ciw3eFy++Ds0" +
                "3ZplptrrTfigW16ja/2sFTE37PeT1MGoSF1ezpGEMmZmodQpGH" +
                "TIliXb+33mWpQXmyB7QeNvye87rOyqusI+2jYdIgjNAYl0zM1D" +
                "qEIg+ZuoYfl71JDexKViPrYVQJs/IqpyJha52wq8m3SbXGzW9N" +
                "/belrZeWH8/RqkzVqvLL7D1R4aRdlalaTX6ZvYvP184O7Rvy9W" +
                "4Ue3VoX19+NVS/4OjuV/7L3VdG63e750avor+6dd/qnhnYtxVq" +
                "vqtrXurk/h78wO2DrPNC4UJT/Iqom/MbxP6Kn/l23+m+l3dfUO" +
                "528MHmkYn+kh+P+Qp+pPTn2Xxrniz5YWSbs82t+dnTksGPNNAn" +
                "Bas3uKPZbLOILetazTD7X6a7wH1IVQs23j9xb7JbrmX3CTOmeT" +
                "4NXmXPV3mfWX613pI6Tk+M9XzYX5Q5d1eVx6n1Xfmaqe/v+t87" +
                "R/XPUdaa/n+Ewze7vIsamf1jrf3OvtqtfaRcXt751NyYm7Wmzy" +
                "e/9+mcT/0fyrwvqPZ8mntgWsqNazy9WMrL5xbPyo9rjuw03bGM" +
                "+Oy8uhhrz7fnyZJPHlli8COz0KMsrTc4O3+0WVxD6mjNlbrAfY" +
                "i2t82Lyz3m/Zf7zLXwOLST90/Z9my7/ZwuYDmfjm1PsaLaBPfj" +
                "Yz5v/Wr+53TFOmsvtBfIkh9GtiPbETFmBJTicTA/RiM/sqReVN" +
                "NZMY51tSYxMM/2gNZWw0py/5krazGmM9T5NJPNhM8z1bGfyf0r" +
                "zqRYUW31W//KmOfTzBjnU8HOqvmdWHPdJMfp0J4x7wvWFbsvGK" +
                "Krui84tPf/cJ95+JaCvFtXfUe3MKX7jn+rPE5Fqx1e9bdA/4/3" +
                "Lf2rpb+z3t3aTZb86DW+iRgz+JFZ6EU+6832OMK82cclinWt5k" +
                "pd4D5w7zYu95j3X+6z7lT4S62lwWpoh9hSHINaP0WMGfzILPQi" +
                "n/UGx2kUYd7gOAkU61rNlbqQMbKhdxvnSuI4Lcl91p2K2v8Bo/" +
                "ExmQ==");
            
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
            final int rows = 65;
            final int cols = 74;
            final int compressedBytes = 1668;
            final int uncompressedBytes = 19241;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrlG11oHEV4wZeCVFt/sKaJVzUQaIWGoAG1PuztHai1VSkEob" +
                "nQQqOhUKEPffIlezvXnDmoIIivpajxB61FG6XUVgwxL6aFFKFC" +
                "3vrgD77pQ0QI7u7c3PfNzsxm9nZ2cxtnudn5/r/5duabmb078h" +
                "jZQXZafiGDQe2WrHaxf7E6hTxM7/UdHcxuS1LIPaGOlbB9P3mA" +
                "4es7rZhCHiGPWrHF2xXy7ZLIPu5r3+7fH0S4Ac73e8P6vojcQ6" +
                "SPttwbsb71k5IMj+L0k5WwzP4mWJmR2m5YGZfkvict7jvdy87+" +
                "bvVIaR7JL07ld/2e30wiG0goad/lGSfz1irjlXFWM5hBrGYccA" +
                "cuDNknmVRUH9aMtTEbMipA/vP6Q+0F3wda2ydldNxj6D/uc9RT" +
                "BE9UJvxWWIe4CXr5z+Q8xQEH3IELQ+XzlF/UR7VFpRg3thvVyT" +
                "jUXmAaq0VrvCUUpwncZ7AFuDZcq9T8VliHuBq9aJthGB7o9PLO" +
                "YIhpEPVhzVgbsyGjAhTvBaaxevYvGR0soTjVcJ+jnmLb1XWU//" +
                "ZX1wOY4qrrQX5ibeAETFDzWFZ4yH/CVxk3luYlAEf5MEbkAR3T" +
                "fXJrvI+ixY0LyJMx8iK3Zr/SsUUSZ0+lBEm5/pCDpjzp2gM/Tu" +
                "QweV6Mk07xznC6XlLHqbyUYvVaio8TeVmU0Oz9oXziFBmlJXWc" +
                "1LT040mMk641fa+qfdU+qGmLQhTGHJgug5lGJsOkg7ZzXZQCG1" +
                "hPVOdGXkT7ILfGW+rsjNdkfZDBwk722e7nXVxxTuS5z8zCmuvP" +
                "ZfdHd9696V7nzndPxEr9HNbXpLQV1mq81sYsYHpjLLGPV2Kpy8" +
                "L5Tuq7+70Ue6ObqNU/z2g8jeU6njKzZv8KHxlNBcXz6kvF06M8" +
                "2FO1x5gi546zy9NsYhOoaYtCQbt8CXMEMNAxnrYC7nBVvsQ0gB" +
                "xIghTnlUDFPEyjyCH2IahFa+AfthzwYV7QJ+pX5qc9YowbHxQk" +
                "P+3Ry0+NC/r5SRWnloE4dTALKdeahHFqlfTidPZU+jhtINXTcb" +
                "Kf04tTwNflene0o6NmFbZk4btqPDWP9c54Onsw2XhqHs9i/+SF" +
                "eyYvXA1Qfgq/A/E+Cesv2zr/3UDTnPdxp/2N90XHlztS7q+8z/" +
                "z66whWMsfctTir0/6pwruINMzj/OR9muYJeZe9b6Vv4F+3tkCx" +
                "j5vlk8Tpja0Qp6bmO7nWYOIn8B58ZDQVFM+rLxVPj/JgT9UeY4" +
                "qcO85ulKbcP+0v7r5gZkEvj5M/dfN481REV+f7u9Zwcefd7EDW" +
                "FmA8VY8UN07Z+47idLjAccrAd3dJHqfu3z+J+SltIX/HUv8R1r" +
                "EnNfPTWrfnFhNx2vzSeioLreW54MMuhmNYSsecvGRUD8NiXtCL" +
                "pcAGT5f5hnXwXoE1qGU4ZomXirPMU5T7Aru4+4LmdtP7AsHCW7" +
                "047+qnE867p/N7X2AfKPD7pwNZj6fylNgykAGn8pU36btijb1M" +
                "706/029Zb18wpTfQll+RW6t/aHAWts8tzoDj7/3JR8Y8T3WSaD" +
                "1jwprJODXr3PidNzbz5vOUlvOb642fAX/YHFmzRdeT7j2GX501" +
                "G1ZByrTwa5LqOc3entO1URmtjLKawcHlDDvDldEgjwNHgGV0eg" +
                "E/pVINlAvro9qiUpTO243qpBzem7yc6ANfi9aCmuYn8A17C7ai" +
                "2qMS7Vz3ajsTDjlDkvw4FJs9h1Rccm0ZrndD2fCK52BnxBkR17" +
                "sAF2NxRMVFtaVY7yoJ4zSiv97peqbaj5ePmtyPx2szvR+XW0v3" +
                "/d1W/N68+X5+cbJXCny+WzEfp8hsvSq2jLw5O2RlVkRPmxmc75" +
                "TjabXA42k16/GE4nRLMipeSHiq3qz/Kd7KbzyZzePlxRSnu8XE" +
                "692i3nhqzhnI48sFnnfLenFK9p6uvBp82MVwDEvpmJOXjOphWM" +
                "wLerEU2ODpMt+wDt4rsAa1DMcs8VJxlnmKcjzdLvB4ul3UfWZr" +
                "PM84zQwWNk4nejFOSfKTct5tK/C826YTp8Zd+uPJmXQmWc1gBj" +
                "mT5TsM4u/AxUNUCrihxTTxUpQb2xV1grTcC74PKmu0N9Bj6L/o" +
                "TbRP9K76HYZ998ZR7tXfYch9l+yfLhqYd/s0ZK9p2sh73u3Tz+" +
                "Opz3d7JeeWqWRx2rT/t+zNMU6lAo+nUn5xMrsvcGp5xkluTZKf" +
                "rvzP/wdk68Up4NOM039Lau5P");
            
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
            final int rows = 65;
            final int cols = 74;
            final int compressedBytes = 2178;
            final int uncompressedBytes = 19241;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrdXE2MFFUQnnjZw0KilyUmyiZChN3ExGU5eOHQ/XowxIuJif" +
                "HAxaMhO55gL/Izszv7Q+PvHrx4QvCg0Q2uECRRQRNNSNSECDGA" +
                "m4gGsokhYQ8c8GK/rqmpqvdqemaY7mHHednq+v3qe297errfLJ" +
                "RK8lUd5VZ4q5TTqzekbqv1/N44VH9Kfn6snqv+Wv2Or1P8RmbV" +
                "1VR+q8aueJ4felvl6oXM6M+uZ2GzmnfR9cz+k3h/6W2dym/1vk" +
                "7xkUezTjp3f51Sb4/r1Kaqy/MprvVznea3538+ZV6flp13+HLm" +
                "VWG5VVa4nF3Z9nqznEe+xqtz7JbXp4U8z6e5ocG+PpkT5gRKtN" +
                "FCiRl0pCxpYRXHAC2u+1XUg+O4mO1YyDmAnN+uxfmMaf58zpyz" +
                "M4f75n6ipTL13YcBenjeSvLzuG+z7g5ecnafp2w6ygq3B+XYar" +
                "+eeEo+mO8ytbPh3MDy5+D2T7W75m6ipTL13YUBOpeUyWvcbELh" +
                "eAnzK1gpq3iF24PnyDoZd/lo3Yif7OzOSXIhO9oT7SmVQNqX1d" +
                "BCiRl0xDFzkFuI4ONxZI6GPbQoWdkseAxlPKfFqROtE0fjveQ6" +
                "8Ipaeq9TfbdhzSxcLg3sq3a46A7VF7vJnjm4Mdcpni9gZfpzn3" +
                "l8I95n5nE/Hvw2uM93OveN8twSv71hnoNfyXudgkPBIZKg2WEm" +
                "zIS1eYb1Ylz6QTMTjc/aCfBgNaLJWszgeRKT58g6GXf5IAOXKU" +
                "ciXJ5LeD5+n65P7/TzfFp8vLPzqT7ysM/B0SauhXdy26e7089q" +
                "Pb83Dv05n6In+nk+6d16uT5FQ9GQb4MvGgpvk44a6ugnrTVq8r" +
                "u8zbO1KtkZB6/muJgruVA37kFkRGm1Eq18KZPhaDjRUpn6hmGA" +
                "jh70UxxG8tzCLETw8ZLPwvd5ntuD48oe7VnwGMr4PS1OndhKDP" +
                "M5Uy+5DtGwWTNryedDKtPPqjUYoKMH/RTXbETw8ZLjJb+KenAc" +
                "F7MdCx5ryktanDo172Q+4Gi8l1wHqmg+Gb3QvAe5kedzi7nYz+" +
                "eW/LuZs+YsSrTRwoh/pCxpuVWkxR9qVZDN+/qYhKOzkHMAOb9d" +
                "i/MZ0/x9Nu6cZEWj7oy4+q5w25wJVzJWvBn1s8KVrMoOPuczqy" +
                "Xn1vkar3bYrV4Lfza1W3met/FH/Xzf6c/Beb4W/mpqf+d5fYpX" +
                "+rlO8Ze5X58um8so0QYrGAMfZdCRsqQF+ZTtaxINsvUoWdks5B" +
                "yIux/nM6b58zm7TJm9btYTLZWpbx1G8js5CT7KoCNlSQvyfTyO" +
                "zNEgW4+Slc2Cx1AubNbi1Imt0zqfs8uU7GBXsCtZ/4YEDaz4Yy" +
                "spxo+8hmz2FL4LLYxSJkeTFW4PnqOzcBmijEddPOInO2v9NDvj" +
                "e85Tg7tPF4929nw3N5XDfuZ+P7d+akD2M/d3uK9ystN1qjqfDN" +
                "Uvmr+TTr93f+hPsurnXVd0yOn404XfF/xbzPct8dm+3hd8VXSH" +
                "+usFMT/3/7rPXCwVdD593dfz6Xzh6/RYQcwvDPb5ZB6YByjRTs" +
                "eWZDxYPMkz6IhD2ojg4yXHLX4VZou+HibmtWIh59Cqm5W104RE" +
                "8yc03ovPSVY0rk8FfRMefzPY51N5qjxFEjQ7zJgZK0/Z84kyrB" +
                "fj0g+aGWv8jsbAg/iIJmsxg+dJTMqZqcg6GXf5IAOXKZxPsjOf" +
                "E8fz7Eq5kmgNCRpYwVUrKcaPvIZs1r0C1uw8RimzXJmtk5dXuD" +
                "14js7CZYjScveZSm5o+f00OxqNkjtwkOme+SiMpNd18FEGHSlL" +
                "WpDv43FkjgbZepSsbBY8htJy9+PUiX2PMMrn7DJl9mq0mmipTH" +
                "2rMEBHD/oprtnBAaxy8TgyR8MeWpSsbBY8hjI4oMWpE1unVT5n" +
                "lymzp6PpREtl6puGATp60E9xzQ4rWOXicWSOhj20KFnZLHgMZV" +
                "jR4tSJrdM0n7PLlNlL0VKipTL1LcFIfic7wUcZdKQsaUE+4dVf" +
                "owjl1V/lXtnXx2zHgsdQWu5+nDqxdVric3aZkh1uC7cl69+QoI" +
                "EFNsX4kdeQzfbnE09wFDXCkShiR9+LUo5F0lm4DFEGR1084ic7" +
                "a/0023siH83rjiM4/OiQgtz/PjPcF+4jCRpY0e9W8gwZ920XFT" +
                "XCkSguD7cHz9FZuAxRWu4+U5cbWH4/zc7Yp7umPP09OSD7dNc6" +
                "26dLvb3uZ+5Q1umpAVmnHXmvUzgSjpAEzQ6z1WwNR+xzC2XwuP" +
                "RjtURFzeoWTdbyfMLhmDxHZwFxl4/WzUp4bpGdtX6a3dt1vIv9" +
                "pxulPr7i67nvNznrUv+soHX6Q9k7/bSwdbqZ+35BUA5IgmaHmT" +
                "ST5SDdLwjIb30wpB80M9nYL5gED+IjmqzFDJ4nMSlnpiLrZNzl" +
                "gwxcpo39AtGZz4nj+fjO+fVMY67jZlzZ1RvP3PMbb5WloxX36q" +
                "ZbZ7nu91KLzza1nZ297zr7Xsp8r7znu95N6/R7Ka1bzt+3NK9P" +
                "5ZcL71XY9Sl/7uW95b0kQQMrum4lz5Bx33ZRUSMcieLycHvwHJ" +
                "2FyxCl5e4zdbmB5fdT7Vq5lmgNCRpY0c1y+m/pKUPGfZt1r6GF" +
                "UcrkaLLC7cFzdBYuQ5SWu8/U5QaW30+z8/w7+/DNou7H517q7n" +
                "4cmeR3Px7di+6hRNsOs9vsBh9lWC/GYVA+RCEfsjgeoLlVEJd9" +
                "XUzMkHU+Byn9brITnz/l8l7kowqzyf7gaHxipNrsJxBrfo4IS+" +
                "oUBa/Mk5ncK/v6OfZn8TmOgXWcg5Sar1SqneYYmNm6s4wU9e9b" +
                "gmN5ve+CY10/Bx/r7H3X3f8bEp62PzjQBxrEZKasdHHQK/O0Kv" +
                "DKvjo3iYF1HE1KzYedZFVW51aR9N7v+dx2IY88OqT8ejdf/wEF" +
                "smTa");
            
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
            final int rows = 65;
            final int cols = 74;
            final int compressedBytes = 1931;
            final int uncompressedBytes = 19241;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtWz1vHFUUHbHUIIEEIiki0aUgBUIoVODJbIRAcpAxtFF+AK" +
                "EAU1gUO8my0aaAwkUKLNwAHUJyQUEB4UMYRICQQPiIEF/BQlEK" +
                "RElBmJ271+fcO3eGXduJE3tntHfeuR/n3Pf8dnd2bSdJvpIk+S" +
                "f5O/lX+ftJ0r8/GR7ZD0nDkX9b2vfC2LmK56NkQ0f+bmP0C++J" +
                "e89Ph94vR+3iQGvw0FN9g1H3TYnZTFvpedRr82wme61u3Fv/Ae" +
                "bQOu7B2siXJMfeYA7NrFe2Ebuf8j3qb89u5n7qP3g991Pce3U/" +
                "da+Mvp/q1im7eBM/7y5u/jolSfrX4KGn+gaj/kMSs5m20vOo1+" +
                "bZTPZa3bi340eZQ+u4B2sj3+B5xxyaWa9cFynXHPvpp+SmPa5N" +
                "7+1XBw891adeiXOmrfQ86rV5NtNrxHHujTlsV6ymNvKpkq1qUr" +
                "aRuten/3nFuKFfn2ryNnBfcLB1sKVWsSK1moErsizSKs/HzMym" +
                "GlEUqLkLO4c6NavE8+c5+1rgbClbKp7PpS2f2Utyylg96kc8ws" +
                "pQ5WNmZlONKArU3AXHoBzFoUSvZEs8Z19LeDFbLEalLX2LcspY" +
                "PepHPMLKUOVjZmZTjSgK1NwFx6AcxaFE67TIc/a1rG0PfG7ZzO" +
                "PY0Y3VH/9w699Dd/Lr+Hj3mfH90420n7b+SM+kZ9QqVqRWM3BF" +
                "lkVa5fmYmdlUI4oCNXdh51CnZpV4/jxnX8va12M/bZ9j6mc8ol" +
                "gdas4dvao57nO40/qOORJnN+naWHupvaRWsSK1moErsizSKs/H" +
                "zMymGlEUqLkLO4c6NavE8+c5+1rWnjzvRjl6j4xzX9B7eKd+vu" +
                "sfGmed+tM7dZ0qVa817qf2Tn3etRfaC7AyEiSYM2y8ij2rjsBj" +
                "WXwfXoNz4i58h1D2fOjPKkd6Ec6Ws+Xik15py898y3LKWD3qRz" +
                "zCylDlY2ZmU40oCtTcBcegHMWhRJ+Dl3nOvpa1b4bPd73Ht/71" +
                "abJOI32+O5wehpWRIMGcYeNV7Fl1BB7L4vvwGpwTd+E7hLLnQ3" +
                "9WOdKLcOUn99TknnKkHT5Zp/i+4GT7JKyMBAnmDBuvYs+qI/BY" +
                "Ft+H1+CcuAvfIZQ9H/qzypFehLPVbLV45ytt+R64KqeM1aN+xC" +
                "OsDFU+ZmY21YiiQM1dcAzKURxKdF+wynP2taw9+d53lNrsUnZJ" +
                "rWJFajUDV2RZpFWej5mZTTWiKFBzF3YOdWpWiefPc/a1hFuZ+S" +
                "sfweLzVjNlrH6MPEvwU2kpf7XKKutZrecefC+cyZa7r90xrTrf" +
                "oD7dl+4r7hCGVkaCBCPGV64BpruSfYqYzdZyfhS1OXEXvkMoez" +
                "70Z5UjvQhn89l8sWKlLdduXk4Zq0f9iEdYGap8zMxsqhFFgZq7" +
                "4BiUoziUaMfM85x9LXA6k84UKza0MhIkGDG+cg0w/ZRmFDGbre" +
                "X8KGpz4i58h1D2fOjPKkd6Ec7msrlixUpbrt2cnDJWj/oRj7Ay" +
                "VPmYmdlUI4oCNXfBMShHcSjRfprjOfta1nbvna+srfTC2L8LXN" +
                "jOd+Rjfe97z3rvn6b+vZ73T7Ha9v9e5VrdZ/afnazTKOvUe2a9" +
                "v0dI76uOxq/d6mPzO2mfap+ClZEgwZxh41XsWXUEHsvi+/AanB" +
                "N34TuEsudDf1Y50otwuivdVaz/0MpIkGDE+Mo1wPTz3KWI2Wwt" +
                "50dRmxN34TuEsudDf1Y50ovwgcsHLieJ2MExGClSqxm4Issirf" +
                "J8zMxsqhFFgZq74BiUoziUsE7Mxlp2HQbX9lWz/68OsPi81Uzr" +
                "t17whM/yq5pfjYNJ89hTzWEOn5fN2k65+7Felai+Y96L8j2ds4" +
                "XvwmDc+b60P5b29+JddHhX2vm7fE99rhz/k+t77POG585It/NL" +
                "57fOr+Xoz8q7USu/dW18e35XfnfndOcDV3+uc57Q2n8ddC7X3O" +
                "+9UEZXOmcK+/U63mdvKe1t+R3pdDpdPAOHVkaCBCPGV64Bpmf9" +
                "tCJms7WcH0VtTtyF7xDKng/9WeVIL8R7073FaGhlJEgwYnzlGm" +
                "BS36uI2Wwt50dRmxN34TuEsudDf1Y50otw9+nuY93p7qNr34Q+" +
                "sYF7jHvrIt0n62OjHN3G33N2D43eyfryiszd7d2wMhIkmDM4Hm" +
                "Fl1BqtBo+tgkYUBWruws8hVrNKPP/qHCKcHkmPFDtraGUkSDBi" +
                "fOUaYNrNRxQxm63l/Chqc+IufIdQ9nzozypHeiHen+4vRkMrI0" +
                "GCEeMr1wCT+n5FzGZrOT+K2py4C98hlD0f+rPKkV6IZ9PiXkOt" +
                "jAQJRoyvXANM6rOKmM3WpuZ/eKtRmxN34TuEsudDf1Y50ovw2j" +
                "c2V/CAx4+qyEfqctlfZZi6Us8KZsumKKoUX6Tp+2uaTcj8Bx5R" +
                "rA41545e1Rz3OdxpfcccibObdG2s/+Lk/znH6WRqFY8oVoeac0" +
                "evao77HO60vmOOxNlNuj42zn566fWd+n1m77veSu9jXafeN73P" +
                "Nv7dX+9CxfP5Bhk/bYyeWyfr2dKe3/zvx0+8vb3204m3Jvtpc/" +
                "fTdlin/suTdbpR1in5D4fiPBY=");
            
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
            final int rows = 65;
            final int cols = 74;
            final int compressedBytes = 1288;
            final int uncompressedBytes = 19241;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtWLGOHEUQXcnwHeQnhw6QfLLknbmIbzgJCTlwYIL9g11xSE" +
                "eCReCQjOQyOyDgJEBCgghOQiJBOlkIEg6LX4DZ6Xt6r6qrm9ll" +
                "99DhntFW9+uqeq+qr2e83tnMXievZnu4Tj+Z7fE6+WO29+vh6u" +
                "GKNs0SSlgjrD/HnhUz8lgWX4fX0Ji4Cl8hlT0f67PKkV6E++f9" +
                "89ks2fW1ngHBIoIjoyxCludTZmWDRuQlqlehPipHfipxn5RNte" +
                "w+rMfld8Pn2+Xny4vlV8PsrWnncPnTaL8MfT9mK9/8u1P/4TvV" +
                "Wr6fWPPX4eoP21S0em/Wril/uV/bHrR92t019f204dP8/m3fl/" +
                "nV/AoWGAgWERwZZRGyPJ8yKxs0Ii9RvQrbQ0nNKmn/2rPPFXw5" +
                "vxxmox3XLtOd5ljBOv0RBkPOp8zKBo3IS1SvQn1UjvxUkn261J" +
                "59rmrfxHN3+6/+qr+CBQaCRQRHRlmELM+nzMoGjchLVK/C9lBS" +
                "s0rav/bsc1Xbva8e57MdvAUf32z+LmsX1rP1BzfWsJr8GmkzPQ" +
                "9WNZa8mkUN649qUw5bFdVoozUo2ayacskzvp8+bu+i6Oqedc9g" +
                "gYHgyUdGWeSz8pnNStElr0aUq7A9lNSskvafV+N7shnt37uNzt" +
                "dnZex99czNtbaN2lx5i3f4vfk92jRLKGGNsP4ce1bMyGNZfB1e" +
                "Q2PiKnyFVPZ8rM8qR3oRvh2/Py2/+K9/f5o/mD+gTbOEEtYI68" +
                "+xZ8WMPJbF1+E1NCauwldIZc/H+qxypBfhdp6mnaf+Uf8IFhgI" +
                "FhEcGWURsjyfMisbNCIvUb0K20NJzSpp/9qzzyWeH8+Ph5N1bd" +
                "MsoYTp01FziOU0HwMpm83V+MhrY+IqfIVU9nyszypHehFuz93E" +
                "5+5J/wQWGAgWERwZZRGyPJ8yKxs0Ii9RvQrbQ0nNKmn/2rPPFb" +
                "zoF8NstOPaIt1pjhWs0x9hMOR8yqxs0Ii8RPUq1EflyE8l2aeF" +
                "9uxzibvhHr7Rjnb8brvoFkDw5COjLPJZ+cxmpeiSVyPKVaiPnU" +
                "R+Ksl3+aAa31Ma2/tpYvZrvE8fvGr79M/7dHJn+nnqnnZPYYGB" +
                "YBHBkVEWIUs57MyyQSPyEtWrsD2U1KyS9q89+1zVbu+n9tzdvn" +
                "3q3r3JfYrV2nm6ifPUfh9v56k9d+25a89dO0+v4elq+xT/nnm/" +
                "vw8LDASLCI6MsghZnk+ZlQ0akZeoXoXtoaRmlbR/7dnnCj7sD4" +
                "fZaMe1w3SnOVawTn+EwZDzKbOyQSPyEtWrUB+VIz+VZJ8OtWef" +
                "q9rtPb7Nv3enP7d9Cn9/etm9zHFa8xaRdh0Z6vWsyg6/j7DKuK" +
                "lqq7X1aEVxjdYb11Zai7rpzitM51Wd802id3mVlfZXw+ptzE5/" +
                "ad8GKn+bszLuzjbJ3Fxr26jNlXd6nn5rp6Z9H2/7tKf/t9zt76" +
                "ZRsa7RMkatZ1DWSC0x5VlWGXdcLesBW16BrdF649rKezPcB/3B" +
                "OD+49o1Y12gZo9YzkCWo5QD8eZZVxp3nswZ+ogpsjdYb1xZVW+" +
                "pm9Xt7xqZcp3+2PWi/Z+7u/8FtnyY+d3+1J2zK9dEb/4su7kyM" +
                "e3PrE/5pOyvl6+jF0QvM0qcUA4tYHUtZNroU5229wrwOrU9xmS" +
                "lnIxPrNrkXRxdry09Q6XUMIxAXxzPLRpfivI3U4QWrzWJ9tqcS" +
                "k2dTJq5dX38DhKLi4g==");
            
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
            final int rows = 65;
            final int cols = 74;
            final int compressedBytes = 1460;
            final int uncompressedBytes = 19241;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtWTtvHGUUHQq7zj9IhWhRKpSKeGftgsJu+AVBSJFR4r+wwb" +
                "HBkkVFS4GQqBCuKJB4SBAShHgovJpIaYKUggbxB9idm8M59353" +
                "RmsnuzjLN6O9e899nHO/0czseDy+Mb7RNOPH1jxDhpnTb+0hbv" +
                "7dwMA+Viqb74gaWpNPESekcuTjfF4508tw00zuTD/fTD6Z/Dj5" +
                "YupdbAa2w4/hTX7t7OdZ1eReEfmqeaJt8ulg9vs5Wb4s1vPRNP" +
                "rDnN31OM2xtVfbq7DAQLCo4DerPEJX5FNmZYNGliUansKvoU/N" +
                "K+n6dc2xV/Buuzv1OtvFdm03HxHEmc8wGEo+ZVY2aGRZouEpNE" +
                "flLE8lOU67uubYK3iv3Zt6ne1ie7abjwjizGcYDCWfMisbNLIs" +
                "0fAUmqNylqeSHKc9XXPsJR5vjbemd/TH1jxDhpnTb+0hll+RLS" +
                "Bl871an2V9TT5FnJDKkY/zeeVML8Onu4/LPXEl7uP7f87/e9c0" +
                "L/8x+2BHzDzL+UrfGXkQ9XW+UqNeN5/Nc6BPZ/A2i0UO4H5ln1" +
                "nO+XT0/LN/Pm38PftgR8w8y/lK3xl5EPV1vlKjXjefzXOgT2fw" +
                "NotFDuB+5ZgZPTf7YEds5h29YDlf6TsjD6K+zldq1OuWNbPPm9" +
                "eVA306g7dZzM9B3K/cl+nOzYtN3fquu79mH+yImWc5X+k7Iw+i" +
                "vs5XatTr5rN5DvTpDN5mscgB3K/sM/W54EwT1evuKbwvOHrxvz" +
                "ifNm6f9nzauD3f+fTWh4t5r3J0qV53i7zubl5/1q+68YPxA1rz" +
                "DBnWCs1nGIzoQTd5fBc1sizR8BRxDbmaV9L1l2vIcLzu3r50Hn" +
                "/vDl5ZzHXXRc90f6rHac6JXq1PAOn96XB8SGueIcNa4fMljqzw" +
                "yONZ4hxRQ2vyKeKEVI58nM8rZ3oZLs7C9+q5kz7DXdi4QGueIc" +
                "Na4fMljqzwyONZ4hxRQ2vyKeKEVI58nM8rZ3oZrufTIv5uqX8H" +
                "17+Dh7f2UfsIFhgIFhX8ZpVH6Ip8yqxs0MiyRMNT+DX0qXklXb" +
                "+uOfYSj+6P7ru3nR22WLSo9HF0aDayKjvyscIrY6cqa8p5dKJ8" +
                "Rp/NZ+uLWf9p7k+3Plit+9Ot9xfzXmX/jRW7j+8u5jit2u/d2f" +
                "++O5+/d/uvD2ZfO2/PT//n+1M9n+Z6j/L7wZ2Dr3E+Hfxy8O1T" +
                "4PytiHz3hIx3B7P3zsj6U2d/nqf2yuTKhNY8Q4a1wudLHFnhkc" +
                "ezxDmihtbkU8QJqRz5OJ9XzvQy3J60J9Mnzs52z54ntpuPCOLM" +
                "ZxgMJZ8yKxs0sizR8BSao3KWp5I8j5/ommOvate/7+rz0wKfn9" +
                "6pZ049n85+PrVr7VqJLRYtKs1HnF4/q8bRHbu8Mva+aVHrZ9FK" +
                "tTp9/5Hoi836N9c315vG7GybeUCwqOA3qzxCV+RTZmWDRpYlGp" +
                "5Cc1TO8lTikVA21fLHYXO93W63p0ess92x27bdfEQQZz7DYCj5" +
                "lFnZoJFliYan0ByVszyV5IzZ1jXHXsE77c7U62wX27HdfEQQZz" +
                "7DYCj5lFnZoJFliYan0ByVszyV5Djt6JpjL/Hm2ub0GjTbnWNr" +
                "tpuPCOLMZxgMJZ8yKxs0sizR8BSao3KWp5Jcd2u65tir2uFN52" +
                "eld9635UxanwvqcarHadnHaXRtdA0WGAiZ8ptVHsWu0vNdVt2X" +
                "1Yr+Kfwa+tS8kq6/nCauqfOOR8dTr7Nd7Nh28xFBnPkMg4HVpe" +
                "fZoJFliYan0BxXkuWpJMfpWNcce1V7Odfd6Ooyr7tcbfX/31Lf" +
                "F9Tfu9U6TuOH44ewwECwqOA3qzxCV+RTZmWDRpYlGp7Cr6FPzS" +
                "vp+nXNsVe16/2pXnf1/rT0976X28uwwECwqOA3qzxCV+RTZmWD" +
                "RpYlGp7Cr6FPzSvp+nXNsVe1/XbzJXhH79Z7kmz/AEuyxXc=");
            
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
            final int rows = 65;
            final int cols = 74;
            final int compressedBytes = 1369;
            final int uncompressedBytes = 19241;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtWU2LHVUQfYi/IhtBxM2Qn5CFr7shf8B/EFwFV2HWjk4M7+" +
                "3mJxhUglFHdBQVUUFQN8YguA9odroZhqxEfN03J3VO3bo9/eaL" +
                "mfF209W3qk6dU/emu6dfp3nUPJrR1gx+inkLpMZRwdnGsTI78h" +
                "6hythN1TB5P9xR3KNm495KsWg2zf2y73PjlYdv0yoOR62vfPyt" +
                "+bDs+9x45fpaR0Wtr3z8beuFWd3qOh15a6+2V9OZfY6ZNQxbz8" +
                "CskVpiyqtUGXvcrfUDtrwD7VGzcW/ltVntG+3GMN54mht8jpk1" +
                "DFvPYCxBLxvgz6tUGXtebz3YEXWgPWo27i3qtjSb83nfbb82mr" +
                "1xJk+kn1bHj1tfbP269d1h63T73WdVvw/225DxtyzywzF7/Ho0" +
                "+8tElu+z+dxdRR9cnuf4ebiedFu+U/+2hdfPp87/eG2Gz46s/d" +
                "HaFbvnZd2W9+q1U9fpBNfpfl2DSev0SV2D8tbtdXsYpaOEgQWW" +
                "z6UqRZdw3o53mPfB/bFfZsrZjMn6rr+Dj3Xf7dU1mLROn9c1GH" +
                "k6POwe9taOMsYQwMV4q1J0CedtpI4sWLXK+tM5lZg8GzNZ7Om3" +
                "g812ExY+PFgg7Gwo9VDl+ZiZ2aARZc0b70LnUFJTJZ4/z9nXsv" +
                "Y63wuWX16u7wVDdNL3guVX7kvDGb5n3v7gAj2ZrnXXzKZR8pLP" +
                "CM3nvmfFyHiUxffhNRgTd+E7NGXPZ/2pcqQX+f57weLlZ1faN6" +
                "f9vWDx0sX5XtDNu7nZNEpe8hmh+dz3rBgZj7L4PrwGY+IufIem" +
                "7PmsP1WO9CI/+zd+sb4lTflOZ/fd6X+nu1D33fXuutk0Sl7yGa" +
                "H53PesGBmPsvg+vAZj4i58h6bs+aw/VY70Ir/db/dXb1KDHd6p" +
                "9tOexoggbvnIB0POx8zMBo0oa954F5wz5ShvSvSeuc9z9rWsPZ" +
                "vN/+kP7Kk+jRZ3Uw6s6unYsimqOEVyVHVzTH+89TpzoI57UBvF" +
                "ZrM332MOIMvKmlnnfZyq/mfv43Wdpv8/Z/N8f2BHLI1STpFa6X" +
                "kQVZwiOaq6cW/KgTruQW0U8xzwy8o+Mz/oD+yI9aPV8+kg5Rmp" +
                "lZ4HUcUpkqOqm2P6Y3g+ud7My20UG55PB75qTLmUqd99D7nvnu" +
                "sP7IilUcopUis9D6KKUyRHVTfuTTlQxz2ojWKeA35Z2WfmT/oD" +
                "O2L9aPv9lFOkVnoeRBWnSI6qbo7pj+XPzIE67kFtFBvuuye+ak" +
                "xZM/Xv3ZE6qs+nE3h/uvP35bqetv+q19Ppbm+/Wtcg/K6y6BZm" +
                "0yh5yWeE5nPfs2JkPMri+/AajIm78B2asuez/lQ50ov8et9N29" +
                "pb7S1Y+PBggbCzodRDledjZmaDRpQ1b7wLnUNJTZV4/jxnX8va" +
                "9f2pflc5vffM7t+L9+x448rZab3ypx1RruSNY6dXjec9hjstd8" +
                "yZGD2mq7l2t92FhQ8PFgg7G0o9VHk+ZmY2aERZ88a70DmU1FSJ" +
                "589z9rWs7d4z/6jvAPV3S12nuk7nZ5vf6w/siCGa8ozUSs+DKG" +
                "ONl6tMQ/NRb8yhXZma2SgGJa0aU9ZMfc+c9PvuZnsTFj48WCDs" +
                "bCj1UOX5mJnZoBFlzRvvQudQUlMlnj/P2dea32w2m7NZsv3Wj+" +
                "Ahk58NpZ6vykdaldClLCPKXXDOZhLlTcnWKerGz2kY7TQ7q9Fg" +
                "h9hO2tMYEcQtH/lgMHQ+UjZoRFnzxrvgnM0kypsSrdMOz9nXsn" +
                "Z9PtXvBSe3Tt3j7jEsfHiwQNjZUOqhyvMxM7NBI8qaN96FzqGk" +
                "pko8f56zr2Xtej1NKv4PAURoWQ==");
            
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
            final int rows = 65;
            final int cols = 74;
            final int compressedBytes = 1262;
            final int uncompressedBytes = 19241;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtWz1vFVcQXRlZrlK4oaVKpBTUaWi8+/wT8hdsywWNf8HjYw" +
                "H5X0QkKSIrXYoUAcmAE1kQRALUNg1IKF3KSLzd65OZM3fuat/z" +
                "E0Hx7Gpn78ycmXPuZd++twtU1fSoqqZPpj9Nf58+mI2uVKO26c" +
                "ve/uLmXmSRw+pc2/TnwezTkV0e2sit97Pos5HVF3adbl/qo88W" +
                "UnSlii2uJy866npqtpotWPjwkMnPgmLPVuUjrkroUlYjyip4Di" +
                "U2ZtLzz9XYOaVzXE/xuYvvu4/9fTe5PLmc+ylmLZAcRwfdx3bV" +
                "8dQpRzAz9pJa1uMpYI2c9bWVYqk+7k+LKGrul32bG66cn2tR1P" +
                "zM82+TlclK7qeYtUCmMeIyKnfVcVTbKmbGXlILLGvRSG21+vJK" +
                "lGJdfXPanNKfTe+nmLVAchwVOmu76u7IWwQzYxdWweR6tCJfI2" +
                "d9baVYr+SkOaFM76eYtUByHBU6a7vq7shbBDNjF1bB5Hq0Il8j" +
                "Z31tpZg3m+ag7NvccOWIO8/BclDzM8fz3cf6vrv3V7wZGLO1X8" +
                "cajNn2P481GLVOX8QaxPvMJV5PX8YajFqnq7EG7lPN7mQXFj48" +
                "WCDkLCj2UGX76c66Gzi8rHjDKngOJTZm0vPXc7a14m82m01Vwa" +
                "ZR8pIvOX3WNeILOzpInSB1N66wHBrjq7AKhdn2E33M7PF5fv+7" +
                "/7PuwH72LNCP7n2Tcv8+IZDHY8mmKOMYqaPMm2O64+Z13QN1Wg" +
                "NbL1ZVN77VPYAsM9tM/U93YEcsjVKOkVxp+yDKOEbqKPPmGOnH" +
                "2sTLrRezPeCXmW2mWe0O7Ih1o9n1tJryGsmVtg+ijGOkjjJvju" +
                "mO/noy2sTLrRfrr6dVWzXEbDP1392BHbE0SjlGcqXtgyjjGKmj" +
                "zJtjpB9rEy+3Xsz2gF9mtplmrTuwI9aNbn2XcozkStsHUcYxUk" +
                "eZN8d0x/5XugfqtAa2Xqy/ntZs1RAzZ+Lv7+L90/LWqT6uj2Hh" +
                "w4MFQs6CYg9Vtp/urLuBw8uKN6yC51BiYyY9fz1nW6u557me9q" +
                "/9F9dT/Xje66l+PO56uvt9/Punpb9JiffjsU5LXKd4Pz5m22w3" +
                "W7FplLzkawTnc992xUj6cBerw3JojK/CKhRm20/0MbPH5/n1er" +
                "0++344s2mUvORLTp91jfjq+2Ydnu7GtRrvZRnjq7AKhdn2E33M" +
                "7PF5fvzOnO/+tPFWDi9X8oax46uG8xajlZYV64yPHuL1cxtv5P" +
                "ByJW8YO75qOG8xWmlZsc746CFezrWv26P2ET537Z/tb+f/bmhf" +
                "ZZHjc3b8dTD7YsGuz3v7x/J/P9358f91f7pzMPo5+F39DhY+PF" +
                "gg5Cwo9lBl++nOuhs4vKx4wyp4DiU2ZtLz13O2tZo7nu/iOXi5" +
                "W/1Dd2BHDNGU10iutH0Q1Vjpq6uEg/OeNt2DVQmbWC8GJq4aYu" +
                "ZMPAcvomjjHDPaOPxUPiFjlcyjOK6nWKdYp1inWKdYp4vw/in+" +
                "P+eY2man2YGFDw+Z/Cwo9mxVPuKqhC5lNaKsgudQYmMmPf9cjZ" +
                "0TV8TzXXzu4j4e9/FP7D6+1+zBwoeHTH4WFHu2Kh9xVUKXshpR" +
                "VsFzKLExk55/rsbOqR9tN9uzUW/72Hba0xi+PQuKPVuVj7gqoU" +
                "tZjSir0DmZiZcXJrVOjho7p/78AeUxUc8=");
            
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
            final int rows = 65;
            final int cols = 74;
            final int compressedBytes = 1680;
            final int uncompressedBytes = 19241;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtWb1uHWUQdREhKh4gjRvEYyRR9u4j+A0QRRKSxlYSyUUMCY" +
                "pBsiX7DSLx0yFhxQUFICFBBSiWKFOlpYcKsfvNnZw587NsgoWR" +
                "2P20szNzzsyc79O9Nze+q0urSxvmWrVYct4qU3zNw/Nd4qXsrI" +
                "on64r1VoPXYpnWWvXVleGo7553zy0iseS8VSbntcKivqvtrrhn" +
                "8GRdmApO1GMV5RoZzbVVuabkRfeCkBZLzltlcl4rLOq72u6Kew" +
                "ZP1oWp4EQ9VlGukdFcW5XLdvPR1sZyzbg+/mI5g+Wczu/65P3l" +
                "DGad0+3lDLJr7yuO9995eWK7MzucvO7s/bdfWe2XF3VO/ZX+Cq" +
                "x4EklsGYzH2HdVD324i9fhZ1hOrsIrxGTfD/p4cjYvjVf9avDW" +
                "VjyJJAZmn7YGsZm+0sh241rLz1Dm5Cq8Qkz2/aCPJ2fzsrh9j3" +
                "prvHWtv2GtPcFefu+iiH2gqATCTJvluZGDfqwNUbRZzvfQuJ7M" +
                "yN6Pw/3D3uneL3vfDt7mzE+KX5v9JsXOQub7f/gp+vUk+tPMLt" +
                "+l2Z9nVi/nNPPq3hhvXZoTTzBmcqXvo1nmMdNmeW6ujXtondXA" +
                "Nsv5HhrXkxn5P7+eHv62vO+Wz6eLeD31j/vHsOJJJLFlMB5j31" +
                "U99OEuXoefYTm5Cq8Qk30/6OPJ2bwsXl5Pc993/Z8cjbHkvFUm" +
                "5zmbd7XdbXWcbHk2Ezno8eByPo01Vpy/P5uo4/W6XfT14PIF/P" +
                "1g8794Eg/fm0Tf/VdOZvleMOe7+I3uhlqNNVIkPsHiyFdFj6uE" +
                "XaGWUavgPVTTeJLdf1Tj99S8nW5n8JptuR1Z4mvsn2Bx5Kuix1" +
                "XCrlDLqFVYDDvJcEwy55So8Xtq3p3uzuA123J3ZImvsX+CxZGv" +
                "ih5XCbtCLaNWYTHsJMMxyZxTosbvqXmH3eHgNdtyh7LE14zmgW" +
                "exdgA7etxNZ2QoomkVFsNOMhyTzDkd2j37WhNvd9uD12zLbcsS" +
                "X2P/BIsjXxU9rhJ2hVpGrcJi2EmGY5I5p0SN31PzbnW3Bq/Zlr" +
                "slS3yN/RMsjnxV9LhK2BVqGbUKi2EnGY5J5pwSNX5PzTvoDgav" +
                "2ZY7kCW+ZjQPPIu1A9jR4246I0MRTauwGHaS4ZhkzunA7tnX2t" +
                "nL98zX+p3zs+U3zVnn9PlyBhP/Lz7pT9STu+KoVa59VlXMrnje" +
                "TiuMOqw+G9edYjd0gm6qPe1P1ZM7UXoqGBi2ptz/qfaeYnHHjO" +
                "kVRh1Wn43rTrEbOkE31T7rn40Wd6J0zQFDeTkfVcyueN5m0xXV" +
                "rlwFfbynqpPvZjsht+Zd7a/CiieRxJbBeIx9V/XQh7uQ+oAyJ1" +
                "fhFWKy7wd9PDmbl8Xj8/rv461L6sXbfyKYduWIfaCSZR4zbZbn" +
                "Rs54f3jb9tA6q4FtltvY+OBT20OZ9WRGlr8/LX+nO79z6h/1j2" +
                "DFk0hiy2A8xr6reujDXbwOP8NychVeISb7ftDHk7N5WbzaWm1t" +
                "bIgdr9HTSK0y8ASLI63y/Wxn201nZCiiaRUWw+QMxySck+1mZ/" +
                "E5jM/lfbd8Pp3j7wh3u7tqNdZIkfgEiyNfFT2uEnaFWkatgvdQ" +
                "TeNJdv9Rjd9T8252Nwev2Za7KUt8jf0TLI58VfS4StgVahm1Co" +
                "thJxmOSeacEjV+T83b7XYHr9mW25Ulvsb+CRZHvip6XCXsCrWM" +
                "WoXFsJMMxyRzTokav6fmHXfHg9dsyx3LEl8zmgeexdoB7OhxN5" +
                "2RoYimVVgMO8lwTDLndGz37Gvt7OVzfNbn+P3uvlqNNVIkPsHi" +
                "yFdFj6uEXaGWUavgPVTTeJLdf1Tj99S8o+5o8JptuSNZ4mtG88" +
                "CzWDuAHT3upjMyFNG0CothJxmOSeacjuyefa2dvbzvXuHvvk/7" +
                "p+rJXXHUKtc+qypmVzxvpxVGHVafjetOsRs6QXfxO8LZ8mvBrN" +
                "fm5nIGE6/6s/5stLhrDhjK68+mOzO74nmbTVdUu3IV9PGeqk6+" +
                "m+2E3Jp3rb8GK55EElsG4zH2XdVDH+5C6gPKnFyFV4jJvh/08e" +
                "RsXhYv/94tf1c57+8F3ZvjrUtzo7f/RDBmcqXvo1nmMdNmeW6u" +
                "bfy9xWtDFG2WG39vsT2UWU/2yPU/xluX5sQTjJlc6ftolnnMtF" +
                "meGznox9oQRZvlfA+N68mMLO+7We+5e909tRprpEh8gsWRr4oe" +
                "Vwm7Qi2jVsF7qKbxJLv/qMbvSZ7L62lW8V+Bafv0");
            
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
            final int rows = 25;
            final int cols = 74;
            final int compressedBytes = 679;
            final int uncompressedBytes = 7401;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtV1tq3EAQ1G9O4cMMOo4X7+8ua9B37pNda8EJBJKvJBDIHR" +
                "JyicjTbrqqH4sgIV8aoZnq6aqe6kHG9jBMn4dh+jS9m75N7xd0" +
                "N6wa048+P6e572Hn4/BXY5pvZr+srPIh3f26Ur3d03ZP//CeeL" +
                "z9OWxjzT392u5g1bd5t93Bqu/p93YH1WhvXl59dE+Q5JjJSl9H" +
                "d5nHTNzlc3NvXEN16IHnbM/X0Lg+mTPb77vt74Ltnv73PbWH9q" +
                "CzxhppJq7G4sirImKVsKssMmoX3EN1Gp+E/Uc3vidZt+9p1fd0" +
                "3+511lgjzcTVWBx5VUSsEnaVRUbtgnuoTuOTsP/oxvfU0amdFt" +
                "TnvneSR7DGfjUWR14VEauEXWWRUbvAnHWS5e0kuKfEje+po33b" +
                "L6jPfW8vj2CN/WosjrwqIlYJu8oio3aBOesky9tJcE+JG99TR4" +
                "d2WFCf+95BHsEa+9VYHHlVRKwSdpVFRu0Cc9ZJlreT4J4SN76n" +
                "jnZtt6A+972dPII19quxOPKqiFgl7CqLjNoF5qyTLG8nwT0lbn" +
                "xPphifxidF8sYhHJ2Vi2ulYnbF83N2ujmMPtAfxnWlWM0qmW/S" +
                "nsezInkTp2fJGQM1Zf9nrX2LxRUzpncYfaA/jOtKsZpVMt+v39" +
                "1je9RZY400E1djceRVEbFK2FUWGbUL7qE6jU/C/qMb31NHx3Zc" +
                "UJ/73lEewRr71VgceVVErBJ2lUVG7QJz1kmWt5PgnhI3vidTjN" +
                "fxqkje5Mu/Ss4YqCl/nq5a+xaLK2ZM7zD6QH8Y15ViNatkvkl7" +
                "GS+K5E2cXiRnDNSU/V+09i0WV8yY3mH0gf4wrivFalbJfJN2Hm" +
                "dF8iZOZ8kZAzVl/7PWHm/+j4acjOkdRh/oD+O6Uqxmlcz36/gD" +
                "lz6lRA==");
            
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

    protected static int lookupValue(int row, int col)
    {
        if (row <= 64)
            return value[row][col];
        else if (row >= 65 && row <= 129)
            return value1[row-65][col];
        else if (row >= 130 && row <= 194)
            return value2[row-130][col];
        else if (row >= 195 && row <= 259)
            return value3[row-195][col];
        else if (row >= 260 && row <= 324)
            return value4[row-260][col];
        else if (row >= 325 && row <= 389)
            return value5[row-325][col];
        else if (row >= 390 && row <= 454)
            return value6[row-390][col];
        else if (row >= 455 && row <= 519)
            return value7[row-455][col];
        else if (row >= 520 && row <= 584)
            return value8[row-520][col];
        else if (row >= 585 && row <= 649)
            return value9[row-585][col];
        else if (row >= 650 && row <= 714)
            return value10[row-650][col];
        else if (row >= 715 && row <= 779)
            return value11[row-715][col];
        else if (row >= 780 && row <= 844)
            return value12[row-780][col];
        else if (row >= 845 && row <= 909)
            return value13[row-845][col];
        else if (row >= 910 && row <= 974)
            return value14[row-910][col];
        else if (row >= 975 && row <= 1039)
            return value15[row-975][col];
        else if (row >= 1040 && row <= 1104)
            return value16[row-1040][col];
        else if (row >= 1105 && row <= 1169)
            return value17[row-1105][col];
        else if (row >= 1170 && row <= 1234)
            return value18[row-1170][col];
        else if (row >= 1235 && row <= 1299)
            return value19[row-1235][col];
        else if (row >= 1300 && row <= 1364)
            return value20[row-1300][col];
        else if (row >= 1365)
            return value21[row-1365][col];
        else
            throw new IllegalArgumentException("Unexpected location requested in value21 lookup");
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

        protected static final int[] rowmap = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 2, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 0, 0, 9, 0, 0, 0, 10, 0, 0, 11, 0, 0, 0, 0, 0, 0, 0, 0, 0, 12, 0, 13, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 14, 0, 15, 0, 16, 0, 0, 2, 17, 0, 0, 0, 0, 0, 0, 18, 0, 3, 0, 19, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 20, 0, 0, 0, 4, 0, 0, 21, 5, 0, 22, 23, 0, 0, 1, 0, 24, 0, 25, 0, 26, 0, 6, 27, 2, 0, 28, 0, 0, 0, 29, 30, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0, 8, 0, 9, 0, 0, 0, 0, 0, 10, 31, 32, 0, 0, 0, 0, 0, 0, 0, 33, 0, 1, 11, 0, 0, 0, 12, 13, 0, 0, 0, 2, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 3, 0, 14, 2, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 15, 16, 0, 0, 0, 2, 0, 34, 0, 0, 0, 0, 3, 3, 17, 0, 35, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 36, 18, 0, 0, 0, 0, 2, 0, 3, 0, 0, 0, 0, 0, 37, 0, 19, 0, 4, 0, 0, 5, 1, 0, 0, 0, 38, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 6, 0, 2, 0, 7, 0, 0, 39, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 40, 0, 0, 0, 41, 0, 0, 0, 42, 43, 0, 0, 8, 0, 0, 44, 7, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0, 9, 0, 0, 45, 10, 0, 0, 0, 0, 0, 20, 21, 0, 22, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 23, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 24, 25, 0, 0, 0, 0, 0, 26, 0, 0, 0, 0, 0, 0, 27, 0, 0, 0, 0, 0, 0, 0, 0, 28, 0, 0, 0, 29, 0, 0, 0, 4, 0, 0, 0, 0, 30, 1, 0, 31, 2, 0, 0, 0, 5, 4, 0, 0, 34, 0, 35, 36, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 37, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 11, 0, 0, 0, 0, 0, 1, 4, 0, 38, 0, 1, 39, 0, 0, 0, 6, 40, 0, 0, 0, 0, 0, 41, 0, 0, 0, 0, 0, 0, 9, 42, 43, 0, 0, 44, 0, 5, 6, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 45, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 46, 47, 1, 0, 0, 0, 0, 0, 0, 0, 48, 2, 0, 0, 3, 0, 7, 49, 50, 0, 0, 0, 0, 1, 7, 0, 8, 0, 51, 8, 0, 0, 0, 0, 52, 0, 0, 0, 9, 1, 0, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 53, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 46, 0, 47, 54, 55, 0, 56, 0, 57, 58, 0, 59, 60, 61, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 62, 63, 10, 0, 0, 0, 0, 11, 0, 0, 64, 0, 0, 0, 65, 12, 13, 0, 0, 0, 66, 67, 0, 0, 0, 4, 0, 68, 0, 48, 5, 0, 0, 69, 1, 0, 0, 0, 14, 70, 0, 0, 0, 15, 0, 1, 0, 0, 6, 0, 3, 0, 0, 0, 0, 0, 12, 0, 0, 0, 49, 0, 0, 0, 0, 0, 0, 50, 0, 16, 0, 0, 0, 0, 17, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 18, 0, 0, 19, 0, 0, 0, 1, 0, 0, 0, 11, 0, 71, 72, 12, 0, 51, 73, 0, 0, 0, 0, 0, 13, 0, 0, 0, 14, 0, 74, 75, 0, 76, 77, 78, 79, 0, 1, 0, 2, 0, 0, 0, 80, 0, 0, 0, 0, 1, 15, 16, 17, 18, 19, 20, 21, 81, 22, 52, 23, 24, 25, 26, 27, 28, 29, 30, 31, 0, 32, 0, 33, 36, 37, 0, 38, 39, 82, 40, 41, 42, 43, 83, 44, 45, 46, 47, 48, 49, 0, 5, 0, 0, 0, 0, 0, 0, 84, 85, 9, 0, 0, 2, 0, 86, 0, 0, 87, 1, 88, 0, 3, 0, 0, 0, 0, 0, 89, 2, 0, 0, 0, 0, 0, 0, 0, 90, 91, 0, 0, 0, 0, 0, 0, 0, 0, 92, 93, 0, 3, 4, 0, 0, 0, 94, 1, 95, 0, 0, 0, 96, 97, 50, 98, 0, 51, 99, 100, 101, 102, 0, 103, 53, 104, 1, 105, 0, 54, 106, 107, 108, 55, 52, 2, 53, 0, 0, 109, 110, 0, 0, 0, 111, 0, 0, 112, 0, 113, 114, 0, 0, 10, 0, 1, 0, 0, 0, 4, 115, 5, 0, 1, 116, 117, 0, 0, 3, 1, 0, 2, 118, 0, 6, 119, 0, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 120, 121, 122, 0, 123, 0, 54, 3, 56, 0, 124, 7, 0, 0, 125, 126, 0, 0, 0, 0, 0, 6, 0, 1, 0, 2, 0, 0, 127, 0, 55, 128, 129, 130, 131, 132, 57, 133, 0, 134, 135, 136, 137, 138, 139, 140, 141, 56, 0, 142, 143, 144, 145, 0, 0, 5, 0, 0, 0, 0, 0, 57, 0, 0, 146, 1, 2, 0, 2, 0, 3, 0, 0, 0, 0, 0, 0, 13, 0, 0, 7, 0, 147, 0, 148, 58, 0, 59, 1, 1, 0, 2, 0, 0, 0, 3, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0, 0, 60, 0, 0, 61, 1, 0, 2, 149, 150, 0, 0, 151, 0, 152, 8, 0, 0, 0, 153, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 154, 155, 0, 156, 157, 0, 7, 4, 0, 2, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 11, 0, 0, 12, 0, 13, 0, 0, 158, 9, 0, 159, 160, 0, 14, 0, 0, 0, 15, 161, 0, 0, 0, 62, 0, 2, 0, 0, 0, 9, 0, 0, 6, 0, 0, 0, 0, 0, 162, 163, 2, 0, 1, 0, 1, 0, 3, 164, 165, 0, 0, 0, 0, 7, 0, 0, 0, 0, 58, 0, 0, 0, 0, 0, 59, 0, 0, 166, 0, 0, 0, 10, 0, 0, 0, 167, 168, 169, 0, 11, 0, 170, 0, 16, 12, 0, 0, 2, 0, 171, 0, 2, 4, 172, 0, 0, 17, 173, 0, 0, 0, 18, 13, 0, 0, 0, 0, 63, 0, 0, 1, 0, 2, 0, 174, 2, 0, 3, 0, 0, 0, 14, 0, 175, 0, 0, 0, 0, 0, 176, 0, 0, 0, 15, 0, 0, 0, 0, 0, 0, 177, 0, 178, 19, 0, 0, 0, 4, 0, 0, 5, 6, 0, 0, 1, 7, 0, 0, 0, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 179, 0, 180, 181, 182, 0, 2, 0, 3, 0, 0, 0, 9, 0, 0, 0, 0, 0, 11, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 20, 4, 0, 5, 0, 0, 0, 0, 0, 21, 0, 0, 0, 22, 0, 0, 183, 0, 184, 185, 0, 20, 0, 21, 0, 6, 0, 0, 0, 0, 0, 8, 186, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 17, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 18, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 187, 22, 19, 0, 0, 0, 0, 0, 0, 188, 0, 0, 1, 0, 0, 20, 189, 0, 3, 0, 0, 7, 10, 1, 0, 0, 0, 1, 0, 190, 23, 0, 0, 0, 0, 24, 0, 0, 0, 21, 11, 12, 0, 13, 0, 14, 0, 0, 0, 0, 0, 15, 0, 16, 0, 0, 0, 0, 0, 191, 0, 0, 192, 0, 0, 0, 193, 25, 0, 64, 0, 0, 194, 0, 0, 195, 196, 0, 197, 22, 0, 0, 198, 0, 0, 23, 0, 0, 0, 60, 0, 26, 0, 199, 0, 0, 0, 0, 0, 0, 0, 200, 24, 0, 0, 0, 0, 0, 0, 12, 0, 0, 0, 0, 1, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 17, 201, 27, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 18, 19, 20, 21, 22, 0, 23, 202, 0, 24, 25, 25, 26, 27, 0, 28, 29, 0, 30, 31, 32, 33, 0, 203, 0, 65, 66, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 61, 0, 0, 0, 0, 5, 0, 6, 0, 7, 3, 0, 0, 0, 0, 204, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 26, 0, 0, 0, 205, 206, 1, 0, 1, 27, 0, 0, 0, 0, 0, 0, 0, 207, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 4, 0, 0, 1, 208, 209, 13, 0, 0, 0, 0, 0, 0, 0, 0, 210, 67, 0, 0, 211, 0, 0, 212, 213, 0, 0, 0, 214, 0, 0, 0, 215, 68, 0, 216, 0, 3, 0, 0, 0, 69, 0, 0, 62, 0, 0, 28, 29, 0, 0, 3, 0, 0, 30, 0, 0, 217, 0, 218, 0, 0, 64, 219, 0, 28, 220, 0, 221, 222, 0, 0, 31, 29, 0, 223, 224, 0, 32, 225, 0, 226, 227, 228, 0, 229, 30, 230, 33, 231, 232, 233, 34, 234, 0, 235, 236, 6, 237, 238, 31, 0, 239, 240, 0, 0, 0, 0, 0, 70, 0, 2, 0, 0, 241, 0, 242, 0, 243, 35, 0, 0, 0, 244, 0, 245, 36, 0, 0, 37, 0, 0, 23, 0, 0, 0, 32, 33, 34, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 35, 0, 0, 246, 14, 0, 247, 0, 0, 1, 38, 0, 0, 0, 0, 0, 0, 0, 15, 0, 0, 4, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 39, 0, 0, 0, 0, 40, 0, 0, 0, 0, 0, 36, 0, 0, 248, 0, 0, 0, 249, 250, 0, 0, 0, 251, 1, 0, 0, 0, 5, 2, 0, 0, 252, 0, 0, 0, 0, 0, 0, 37, 253, 0, 41, 0, 254, 0, 38, 255, 256, 39, 257, 0, 258, 0, 0, 0, 0, 0, 0, 0, 259, 40, 260, 41, 0, 261, 0, 262, 42, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 263, 264, 0, 0, 265, 0, 7, 0, 0, 0, 43, 0, 266, 267, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 3, 0, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 42, 268, 43, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 71, 269, 270, 271, 0, 0, 0, 0, 0, 0, 0, 272, 0, 0, 0, 0, 8, 0, 0, 0, 0, 44, 0, 0, 0, 0, 0, 0, 0, 0, 0, 273, 0, 0, 0, 0, 2, 0, 274, 11, 3, 0, 275, 45, 12, 0, 0, 13, 0, 14, 5, 0, 0, 0, 0, 0, 0, 0, 276, 0, 0, 0, 10, 0, 0, 1, 0, 0, 2, 0, 277, 44, 0, 0, 0, 278, 0, 0, 0, 0, 0, 0, 45, 0, 0, 0, 0, 0, 0, 72, 0, 0, 0, 279, 0, 0, 0, 280, 0, 0, 0, 0, 281, 0, 0, 0, 46, 0, 0, 0, 47, 0, 282, 0, 0, 0, 46, 48, 0, 0, 0, 0, 0, 283, 284, 285, 0, 49, 286, 0, 287, 50, 51, 0, 0, 8, 288, 0, 2, 289, 290, 0, 0, 0, 0, 8, 52, 291, 292, 53, 293, 0, 0, 54, 0, 4, 294, 295, 0, 296, 0, 0, 0, 0, 0, 0, 0, 297, 298, 55, 0, 0, 0, 56, 0, 0, 57, 0, 24, 0, 0, 25, 5, 299, 6, 300, 0, 0, 0, 0, 0, 0, 4, 0, 0, 2, 0, 301, 3, 302, 0, 0, 0, 0, 0, 0, 0, 0, 17, 0, 0, 0, 0, 0, 0, 18, 0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 303, 0, 304, 0, 0, 0, 0, 58, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 305, 0, 0, 0, 0, 0, 0, 306, 0, 0, 0, 7, 307, 0, 0, 0, 59, 0, 308, 0, 0, 309, 0, 0, 310, 311, 0, 47, 312, 0, 0, 0, 60, 65, 0, 0, 0, 313, 314, 61, 0, 62, 0, 2, 19, 0, 0, 0, 0, 0, 4, 0, 9, 0, 10, 315, 0, 8, 316, 0, 0, 0, 0, 0, 63, 0, 0, 0, 0, 66, 0, 0, 0, 3, 48, 0, 0, 317, 318, 319, 64, 0, 0, 0, 320, 0, 0, 0, 321, 322, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 49, 0, 0, 50, 51, 9, 323, 0, 52, 324, 53, 73, 0, 325, 54, 65, 0, 0, 0, 0, 0, 0, 0, 66, 0, 0, 326, 327, 0, 67, 0, 0, 328, 68, 69, 0, 55, 0, 329, 70, 330, 0, 71, 56, 331, 332, 72, 73, 0, 57, 0, 333, 334, 0, 74, 58, 335, 0, 59, 0, 0, 75, 0, 0, 0, 0, 0, 26, 0, 0, 0, 0, 0, 336, 60, 337, 61, 0, 0, 0, 0, 0, 0, 0, 0, 0, 338, 339, 0, 340, 0, 0, 20, 0, 0, 6, 0, 1, 0, 0, 0, 0, 0, 21, 0, 0, 0, 0, 0, 0, 0, 0, 341, 0, 0, 0, 0, 0, 0, 0, 0, 342, 3, 0, 7, 0, 0, 34, 1, 8, 0, 0, 0, 62, 343, 344, 0, 0, 63, 345, 0, 64, 346, 0, 65, 347, 66, 0, 0, 76, 0, 0, 348, 349, 0, 0, 77, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 0, 67, 350, 0, 68, 0, 0, 0, 0, 351, 352, 67, 0, 0, 0, 78, 0, 4, 5, 0, 0, 6, 0, 0, 0, 0, 3, 0, 0, 0, 353, 0, 354, 355, 0, 0, 0, 79, 0, 0, 80, 356, 0, 0, 0, 0, 0, 69, 0, 81, 0, 357, 0, 82, 70, 358, 0, 359, 360, 361, 83, 84, 0, 362, 71, 85, 363, 0, 364, 365, 366, 86, 0, 0, 0, 367, 0, 0, 0, 0, 0, 0, 0, 0, 72, 73, 0, 368, 1, 0, 4, 0, 5, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 369, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 74, 0, 87, 88, 75, 0, 76, 370, 89, 77, 78, 371, 0, 372, 373, 0, 0, 374, 375, 0, 0, 0, 7, 0, 0, 79, 0, 80, 376, 68, 90, 0, 0, 0, 0, 0, 0, 7, 0, 16, 0, 377, 0, 0, 0, 378, 0, 379, 0, 0, 380, 0, 91, 0, 381, 382, 383, 0, 92, 384, 385, 386, 387, 93, 94, 0, 0, 0, 388, 0, 389, 390, 391, 0, 95, 96, 0, 0, 0, 0, 0, 0, 0, 0, 97, 0, 0, 6, 0, 0, 0, 8, 2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9, 0, 0, 0, 0, 0, 392, 393, 0, 394, 0, 395, 396, 0, 0, 0, 0, 98, 99, 0, 0, 0, 397, 0, 0, 69, 70, 398, 0, 0, 0, 0, 0, 0, 100, 0, 101, 102, 399, 0, 103, 104, 0, 0, 0, 0, 81, 0, 0, 105, 0, 0, 0, 0, 82, 0, 0, 0, 400, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 106, 107, 0, 83, 108, 0, 84, 401, 402, 0, 0, 85, 0, 8, 0, 0, 0, 403, 0, 404, 0, 109, 0, 0, 86, 0, 405, 0, 0, 87, 0, 406, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 407, 0, 0, 0, 0, 408, 0, 409, 0, 88, 0, 410, 0, 89, 110, 111, 90, 0, 0, 112, 113, 0, 411, 0, 114, 412, 413, 0, 115, 414, 0, 0, 0, 0, 0, 415, 0, 0, 0, 0, 37, 116, 117, 0, 118, 416, 0, 417, 0, 0, 0, 119, 418, 0, 120, 121, 419, 0, 122, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 123, 124, 0, 125, 0, 0, 126, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    protected static final int[] columnmap = { 0, 1, 2, 0, 0, 0, 3, 4, 0, 5, 6, 1, 1, 7, 6, 8, 9, 1, 2, 0, 1, 0, 0, 10, 1, 5, 0, 5, 0, 1, 7, 2, 11, 0, 12, 0, 13, 1, 1, 1, 6, 7, 0, 14, 15, 12, 3, 7, 0, 16, 2, 6, 12, 17, 8, 2, 7, 18, 16, 19, 3, 0, 19, 20, 21, 1, 10, 22, 23, 2, 24, 25, 1, 26, 1, 7, 7, 27, 16, 1, 28, 2, 16, 29, 30, 0, 4, 0, 0, 31, 32, 8, 3, 33, 34, 2, 1, 0, 1, 3, 11, 35, 36, 18, 37, 38, 0, 39, 40, 8, 41, 1, 42, 0, 1, 43, 44, 9, 6, 45, 4, 46, 47, 48, 4, 16, 1, 1, 49, 50, 51, 38, 6, 10, 0, 52, 0, 53, 54, 10, 8, 55, 56, 0, 57, 1, 19, 0, 58, 59, 60, 6, 61, 23, 62, 3, 63, 4, 64, 1, 65, 66, 67, 0, 0, 0, 22, 68, 69, 70, 71, 72, 0, 3, 73, 19, 0, 0, 74, 0, 75, 76, 7, 11, 0, 2, 77, 3, 0, 78, 0, 79, 1, 80, 1, 81, 82, 83, 84, 0, 85, 86, 87, 88, 3, 89, 12, 0, 12, 90, 14, 4, 91, 92, 93, 94, 22, 95, 96, 0, 0, 97, 98, 3, 99, 0, 100, 26, 6, 16, 2, 24, 16, 101, 1, 4, 102, 2, 1, 1, 103, 0, 8, 104, 105, 1, 106, 107, 108, 109, 110, 111, 11, 0, 112, 22, 14, 0, 0, 9, 5, 1, 113, 27, 2, 27, 8, 4, 5, 114, 5, 2, 10, 115, 29, 116, 117, 0, 0, 18, 29, 1, 118, 6, 1, 0, 7, 20, 0, 4, 119, 2, 31, 1, 0, 120, 121, 49, 16, 7, 3, 24, 122, 1, 9, 123, 124, 26, 125, 8, 126, 0, 6, 127, 128, 129, 130, 131, 132, 31, 32, 133, 134, 10, 11, 135, 35, 12, 10, 136, 137, 13, 0, 5, 13, 138, 139, 140, 8, 141, 6, 142, 143, 144, 38, 27, 145, 146, 147, 26, 148, 2, 7, 4, 149, 150, 0, 39, 151, 152, 0, 153, 0, 154, 40, 18, 41, 155, 156, 2, 157, 49, 7, 13, 158, 159, 14, 42, 160, 161, 162, 0, 163, 164, 31, 0, 165, 166, 47, 4, 0, 34, 167, 168, 169, 18, 170, 171, 15, 1, 172, 173, 174, 34, 0, 0, 26, 0, 0, 9, 175, 2, 27, 35, 18, 3, 0, 45, 1, 176, 15, 177, 178, 9, 8, 0, 179, 180, 181, 1, 182, 183, 24, 184, 27, 185, 41, 2, 0, 186, 187, 188, 38, 0, 14, 0, 2, 2, 189, 9, 42, 7, 190, 191, 2, 192, 193, 51, 194, 32, 195, 196, 197, 2, 0, 198, 199, 4, 200, 52, 3, 20, 201, 34, 18, 202, 203, 3, 204, 0, 20, 205, 53, 206, 207, 208, 1, 6, 209, 210, 211, 212, 213, 3 };

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
            final int compressedBytes = 1353;
            final int uncompressedBytes = 38977;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtXUuO3DYQLbJrDKYxC9rwwt4xcCPrQU7AALPMoo8wwFyECR" +
                "AgMLzIESb7LHKEAZID5Ag5Qo4Q/U1JpES1fpRUD3CPe1pqiaxX" +
                "j1WlkuaZ/QS/foD09Q03H6/6Dagf9OUi/4ZHA7/9x/EKjJ/vUf" +
                "/1/bvry0d1AfUe3ppf/nj6/OOfQCAQ2pDJP/R9yMr/qO0P9BMY" +
                "EAIg0Q/k5oOERD+Y/i4d2SPAg+b4D/sGUv04KS7hLVykekx+3H" +
                "15MvekH6Px3K3f/5J+b8N/fk/851z5zyXxn5fEf2TiP1fGMPOf" +
                "p9x/4EL+QyAQGhBF3FGGIGX8wSHRmPwDTN+pYjMl1I5Gj70bCK" +
                "IIgUAgEJoLp5W3t1cUkWftvMja89X0AaZYP5/vqP5GiAAsI7+w" +
                "nCHjvyzeC9n0FxZF/cquf5yO6j+8eEEsRIu1I2Lu2C8C+1X1Q2" +
                "nXD9P6B3ytf2T1w09F/RBmqB/aU8V54PRIEo09QTdt25tRTuE/" +
                "7vp5yf+HjP9UPyesUTxRNDHB/rtW/d7Wj7Nr/byU62d1fDXH8d" +
                "vjV83xq3ivX0zCf+HJH+v1V9hp/XX7CVBH/bxJi3yz6eyHMUzA" +
                "dq4fxBh54yZsPH6O6SpK15QE1Q9hwvrhRsgfLXrqr4H9MxHUD3" +
                "z1O4fb+up3cqhRt81fOaXzY3vi7jxqMM/89dl/pvptT/11of6z" +
                "2/1Ps2/ByHQyRLIf02kVSCOgfAU0YF6yr05Lq/D67qpfMslOdj" +
                "RGwVl3HSA9C2P1erZJM53/lyNGTNIvD+P47vwXcvuxtv2gbj9Z" +
                "2u+1bb/u+ttS9Weq3xBymOql0Gg362fgz6AYTnTGBNyrUIvFj5" +
                "yo1CCVO+jB7FeYGFRVBuTKYl++yLGfk2/Bs0W8ZJFUwUZFuMH+" +
                "9xX7GVang36nmHedmTIHVguxWq85SQFk7LY/suorsb4l+jmwCD" +
                "nUfvLfaaQWnQLDSmsrxzTpyfaPJBDtZ4yajX/xgK1CQOUTGdFl" +
                "LLNcuLEYW51J5evURY/NkIN1OJCcLiLF8X6qhsTZGgi3Jik8TK" +
                "y7rLcIj4NjPe0ihe468TWTo/ioOzr+YPDeSTZ00MjIUCHeZkzJ" +
                "4jkVlNZcsi4SmvYb0VQRBocA5U/2SaHn/PTgVWfbK6cO0qnBVq" +
                "QwZifiQE0ht6K3f7O6/nTquP5E138OEdwwMsHMi56nf7fpVrvs" +
                "/9w//24+//0GKuH9D6eI+x8Ggu7cI2xK//TBx3+TW8/dv0oqsm" +
                "/weuy3E3Nrd+rucJZl+5fXjPpDxr/3BJvuH9jk+ufsXwd3/7oM" +
                "6l/noVcHtyiIKiLKHCB+oLrwqOjDLHT/yCj9Cb//gvQnPrT7v5" +
                "+q/u9TZuDsTU//N67sKbX+DWNrr7FVH62AftqUOpyLmjh3hFIJ" +
                "gUAgLz9GpBrQP6nKMEGQBQl7BzuWXGE901f2JChPOQBLSZju/t" +
                "OZ+trVYfI/3jP/Qouu+Y/W/5aF2Yv9xM0fLh7OUTForH5/ZYOD" +
                "PymnU/0F2Ib/D5SA+vhd61fq07Xxw9zjj6l2TsH67fkPb9w/ps" +
                "KnVCzn/61jWX+k1FXd5XJjFN6G/zC3bsnI/RE7bK3cZy4nO/Ko" +
                "/GHG59/MlHSQFhNm8F9PBKVdW4Ze7iP9J6xF3sEpI2WQceXfYi" +
                "H9kFb8bqJ4/pQZ4wLSGcKP85Rdx9/x2f9g4h1n/E2YHu6S3OTP" +
                "D5SB+/OYvGBFcL8njX1+3lLP3yNdJhAIBIq/CHHFMsoOwYqAbd" +
                "EShKZYgXAgnxPD9vTrPwvSf133pvz+Le1JAKN+hARdrxiO/wFP" +
                "HRUS");
            
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
            final int compressedBytes = 1229;
            final int uncompressedBytes = 38977;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtXUtuGzEMlVQlUIMsVCCLLhU0B+gRVCCLLHOEHkUpuugiix" +
                "6lR+gReqTOOHY8tqUZjSTqMyKBILZhDSmSIh8pzZgQwgjh5J3k" +
                "8SUd/sz4Qu0/4Ic3wxBNrCQMIw2S9P6mBpKArRHvXAjuFFUcLe" +
                "O69sT+xsP+wHrYBtG0lwNWNveQ2bz5oQGQh44CCJdY6lQMuXrF" +
                "tuasIiczvtMkH5ge9KwIUxMn1mpUHv0xqJ7fTBSp+ZlltkcyyJ" +
                "9ElfLTdtcDEmZEJNB8wwMwKRLSponP1FdQK0O6izpLAH2HbDwa" +
                "/z258R8Bxn9F448F/975zv9+of5O1HGIxe+L9rsjV2Ja5Z14HB" +
                "uuLy3gUfML/qaM/yAhha8fObN+uK//8jzCIobenP5SxO/GQVa+" +
                "/hPF9bcIdQPwSwx+aJ3kWjSn89kvR3z4MphcDNZ/Ib84M58luS" +
                "aK6oeR9eOgFs34M6Xk5pbrD4pJ8ok8EPU4/Lt6/W5u/9Siv1Tx" +
                "S6WL35IgecbfnuNPxfmLnVbwG3FoDXJNta7pYTBmFAU/G9K2cC" +
                "rhanGGAABDoT/GYlF/v86tbOxfoP6QOvJLhv6P63f7OZ37fksC" +
                "FRFrzm9fiMAix1cw/yWl8CADUuI4ac0JEiSZpYCptz31vPOPOn" +
                "+xjfMjfO2iFseR2rZ/pcdv7Ob/vPv8ZZz/z8P8B/vqi/3jSoKK" +
                "ygHkFvizgvwrBhBJ4efGNcrSq9+NX3B82fGx+A0Gv/rjT7Q/ji" +
                "9ZP7XOv3f9R4xPcv6s7/VXvn+C40uuXzleT05A51v5/tF2K6ix" +
                "lJBILdZfMj5+3nszi/VXNTeFQi2FlOtvqn9i0z/Z658A9J9iPW" +
                "+N/wTK77b/Ov5Cl9CfwgAF7oXT9UOm9t+/GRPXif+RNP6H1I3/" +
                "zMSvmfjtNR6pY+SWIP/7xT8SHP9gMqDMNr55+wfgJ7FKirr9p+" +
                "r66bf//fc6sH5aqqcszx+jR2PaZsdkdvXB9e+iz78kGC9Won8J" +
                "77+R539KnT8SiebPj/z5OX+aN6iA6i++f1vafxL3P03G+FVz/q" +
                "Kub/rEr5z5owb9NT3/WPv3hp9SljY+6aqyeNHZ1o1aVX+lP/9Z" +
                "V52k06xfi/6Ut/54Vf0vdfoGeLGavKZd4H/m/+zCft9c/n8Nbz" +
                "+xwguL5r901PAtVorgVhdSLH6UM2uBZxhPQsZHUYL6PQt8h6u/" +
                "o/snwP6nQsQQDeHsjuu3zuufKGgm1sUjUHCgl1nXCU4q+P2i0v" +
                "evdD8+Nv+Vvv+7nP42sH9Ywf5NlfefNeF/FccP7F8gwhNzYMlU" +
                "ILkzfu+fP8D/HeP39PkDT2P8FtMYGoEOY9ElNP21pgcYNFqYP1" +
                "K2eCGbkra2/kcO/vETF9nCmcrHn7Vly4veQ+8/S4c/yxe3vFB/" +
                "rXu/bkJSESi/ypW2Q/nTabSOe/5ZovqlvvprFHrXgDjKD/H8u/" +
                "7OX23LfkhISEjtxs9Uzz9dLHrzx3+P/Fkavxht0dn9gYuzdaBG" +
                "qb7a8elseQSwP6H9OxwguyPaxjIjfyQkkDXkKD7rklSlnF65Yr" +
                "lu+evul9AV93+/JzUJlf8CUUQ6/HGX/vlxrftv3evX5b/c5r/y" +
                "1H87TldQG034YM0QF68mFyDlrd8j+8f/ARgZxzw=");
            
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
            final int compressedBytes = 884;
            final int uncompressedBytes = 38977;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtnVtu3CAUhjGiEaqiiEpZAJG6kJO3PmYJXQqpsqAupUuqPR" +
                "fHE2MPtrnzfw/RTCQC58rhYkcwphgTTDLNzmjGLx+74QedvnR/" +
                "GDPiOyN2hcTYYh05+WP5ICdDO4zJSTKzSf/ioP1v2+sA/tMWHV" +
                "TgC2H9eIZDPdD/yvjABF23k3qof3b4j5r3b4LUX3nbXwSvP31A" +
                "DcaPLCj+jqMCVuDl+3/o+JnK/xkw3eTLoNNe/ps27awfWsw/6U" +
                "sDNfG1zqM96q5oZWw5veVPETcsZ72m6b/t9Ucb82/t6y/4P8jW" +
                "fym8/+7ff8BE02BA6hbk57A/xAUtQFMXxdEeAPHqX7LVvzRsA5" +
                "3q37fT79+H+vfjWv+aPmQD1r9yW0uP918m7eW8/Un+7iz/r6F9" +
                "+K0yvrPg99V3yv7rqzuBQzhzGAjUicvlQO3WyGc21rdfCIYq2H" +
                "8gf+wJ60v9+Dvq+YeH8xe5uP8cY6Z1UgLV278FU3aUFj78bQvR" +
                "pfjTy/GnxvjDtWUA/KRoAAAAoKD6EQCAIhaiAwAAAFvJ+/485r" +
                "+sPafS55fqOSzWzvbjd+wnI9qv3MW/SaWCZm92UeL+8fwZ/KPC" +
                "/YOjKTh1Cm+9/7y8CfYHMedf0Y2Zf/qOET1/nU0O45fL8WaTjq" +
                "vq7Vdo/dQ5DJZy0vx5/WfG9fuz6/r9pWT/Wd6/SCD/evx/9Ry9" +
                "0f91jfGPiqkgo6a2L8F/Qdj5c2n/+3Vp/nhwzgsaas4cvvAZq8" +
                "KN0+QeV7+8UQDBchwDFVSjf7kh0+D8AoDskGhfgP75Tf5UlsJE" +
                "W9IsRTiuz0T/Gv6D8derv7Tvf1iPMLlWj5ndGnRuDwAIyMu4cj" +
                "SMG6nYA9Md/bxUG6pPQW/9x6dHRt1wZvzjmirEB5nHv9DfHsKm" +
                "dXE3w6e45uneP85CNlrb1/357TdCCPoHCavTKuL32Puf770/Wf" +
                "z7jP9p+/n7kzO4ECbhPyDZ/AdAHqlPY/wRIVs1ay9ta3imU6/l" +
                "T77v/pG/+/MSr5cHx+b/APev871/v7ZnQqyUy2LH9nye2bermG" +
                "aWrXlvPGUxHom6ogDxvz/+hW1yJ2seMJ7jP4/nR2KswmuCcg06" +
                "tSkpFFKn68TtE8lbzfZE/oKku39R8f6V0/NbS/KbrOSnI/Yj6/" +
                "hfo/3/Gcly/v8zReSfgOu9/1H5h2k=");
            
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
            final int compressedBytes = 895;
            final int uncompressedBytes = 38977;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtXMuN2zAQHRHEQtgTDy6A20FKmNxy3JK0QQpIaSkpsfyJDF" +
                "Myxe+Qeu+wsLWgOeT8qeFoIkOkaSRLF1hS14/D+Q/PX4afRJN+" +
                "J6YbWM8j9PN44zv+g8rAbvxPR64/Fjr9/p/27P9y/FRh/WecSI" +
                "+u5/P86h995uEx3+bfwECOIVtcGJerWa7/+mXem/fHMTaB/KWZ" +
                "32+8dowvIf+S9e9ORTr9PaWxf2Mt+5HCfu/Uv6r8++3PP67iv4" +
                "4KsypQXHby/WpZDA3ELyw5fkrjv9uFNm3Hv0fgXzfyNxzVk3Hk" +
                "ThUfDwCCLIVc+V31X+Oz//p19l/DJRn/cfZfo2t5qjD97KKf7/" +
                "R/zs+/7vRfDxM4af6dOWaH/wW8oXyjlHac6pRdRaBrAAAAyMsB" +
                "ADEF9g9IBtGpxrj6/lX1z5nQ9xfM++svxJ+/xOxi6TBHmGyqyI" +
                "HKK/tPmP8rkfqnnvTnu1v/iN/g1zrwf/nqr2rHTxwi/wv/wQXr" +
                "5+rxb1/9mnf91u760VpxnIz6SaC6FxzXZM06pVMZGforI//RG3" +
                "V9K1piEs+P/B04sv1K4P9sg/5Pgv+2MBbV7Gc4/98i7+8g/wWS" +
                "q4kVrGobUCufnRjBaqAsiudfx6gfhyYDQRJy0YeJEr//GzzHT7" +
                "R1f9d/POS/6/wZ8X2eHFBhO1oI6oFjWD68/2o4Hn51f07/+c+/" +
                "Zf3T7f7cxA7SP+5RzlroZs+/+k3w+u26/Jq7/Hoem2/J+cb+0e" +
                "79q4BQ+ikR/WXmd3PQENCZ/0L92itw09SrxndVZdOC0PoL27/Q" +
                "hOb/QLNA/5b+nVhHSntc+4Pzh5T7Z7F/O/N3rB/6Kz3pgp2TK7" +
                "899I+Pkb/W+/+X0L/W9Hdq3/4PZekPiN9x+PwC6V4m5bRfMuxP" +
                "3/ljBft5MPuB80/EvynsX57zB+TvoL+sVReKIddKk/uf2vMD9a" +
                "Ho4X13QYaZQGmpIFOx9x9i6Uf/NCCx2vfX//1Q6QBuZ8gKltFc" +
                "GsD5Q0z9frbzDxybHeL8AgAAoBH/Ka5/LwD+7QTOjxuJv/qQ33" +
                "blJ0P9ipHHP369C5733xj6H6AU/Sa64f3/X/UP5WT9Q22b8lPQ" +
                "oO7qPwnkE5NaojpVVJYhZn6h8RPib/h/pOYHiH+eMWb4DRbMZH" +
                "bFb3zrn0Wf8/OvtfqXzfxroEbuD3SkqtRYW+K/zu3lig==");
            
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
            final int compressedBytes = 753;
            final int uncompressedBytes = 38977;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtnW1u2zAMhmnDGLyiGDxgB1CAHmBH0BF6JG3ogXbExe3qek" +
                "DSxB8SSet5fhSNUcUSTVEvJVkV2UYncRh/9hL+XQnSBonnC9KM" +
                "H59fr//6LZJeHs7XR5JIjN1UYiw/p5dypHjhnqf3Wl7g7e/DWP" +
                "efAgBwdGLl9V9cvlk6inq137nml8b/DyPE1w/NOP53D7Mv+m/8" +
                "L+1/cTeje2h/EKgZy8//jv7Tf9Z/tpbH/vnjV/Bnv6aU/rFh/x" +
                "VD4eApfuXTL/QfC/nDev8to9+O6//kn+hvNzSYwFr83af/a98f" +
                "YL/+0y8qucf6m1797dEeLyikHdO2pFwebnTEdjcHB/QbwA3PWT" +
                "V/sEZ/NDmiWHvz69LqtKEMwyhFhlkj3ir89ZIQS9dUWpvPfmLc" +
                "fgVmGuLu/afk/LNl+5XJ/7eS7/lr17+M/aPrYGC4n334X5r878" +
                "e9/neqRD8GgTvNhKmgqvGb/C+7fo0L9M9w3amaagN7sWb2yuW1" +
                "6YkHmQPsQeeuO+f3Z/74yPlDyDj+gwX97df/2b+p2/4M+n/R/N" +
                "O8fMrU/mDY/8uMvzFb+/Ff+/7rZ/4hoD/AXPxz9/6Y1iK99uYA" +
                "35sTgrD+AqCjQgAAZtKrhP4zOn9hqv0L589W7H/KnX9H1+3Pn7" +
                "/4HX/Z/0v+Q/y8Jaq7zY4BDlDYP6N9/u+1+B2u959hit+HSTu7" +
                "yu+vrZ9s5x8b27/1/ED3+isY1p+s/xO/jeVP5vzP4fwPgD//Ga" +
                "p/TPkfVVIuDwB28H/+infl7jr/9b5+pV1/9Gfd+Y93/ztNg0SS" +
                "NvWDfJHQxKepaa08n3/99iixkXaQ7+/N7F5ievzzaQbQ3e8YRz" +
                "r/FKz1/5zxq4b1g4ALL8XY/gOeP88/swpi/AYAQL/r64ddx199" +
                "/ROz5R+cf7wjFQof7+dPkD/p9h/iD/abYHESqoT339EfjD8a+Z" +
                "PR/Jf1g0L6mfdP6m4/oF/0s57t/3/Oa/z13v+JP8eP31n7z185" +
                "cOl2");
            
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
            final int rows = 35;
            final int cols = 16;
            final int compressedBytes = 72;
            final int uncompressedBytes = 2241;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNpjYWAQYGBgYeBgUGCAAAUGJiiTEUQ4gDmMTQwMDSxcDA4MMO" +
                "DAAtdBa0Ave0bBKBiM6VdhiLufYdT9o2AUjAJaAAC+swM5");
            
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
            final int rows = 45;
            final int cols = 107;
            final int compressedBytes = 3982;
            final int uncompressedBytes = 19261;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtWwm8TdUa/9ba87lUuIknUub5GqPoFjIk0+2mElJkliHSIL" +
                "qZigZl6BlSSqQQkggRUVGhnlc00auXl/ekiId6531r2NM5+5x7" +
                "zj3uvd3fr/37nb32Xmvttfde//3/vv/61jr0NSCgggUm7UEf1U" +
                "bB+VACysDFdC1cAlWhmjKTlNG/o/UhA+ors8hWqxI0peXgachU" +
                "jkAr2ku/CTpBFl0GN8Et5LiWrU5URpml4Q64HQbAID0bFPI0FN" +
                "PuIL8rVchzkA4X0sn0OTUbfjKWQkV1LZxVcqAyzYbq5AmoaXa3" +
                "jpFnoQ5dAg3gcmimjKPdoTWdRq8hL8F10EHZR9pDR6u+soDeQ7" +
                "sp8+FWeiP0oT9Cf5JOjilTIQ0uUCztHjIdSkFJKA0X0cNQDi6F" +
                "y6ASnaS2V8ZAbbIS6tJF2n+gETShdWl56ydoDi3I8+Sw2hVaQj" +
                "vjPToFukBXegRmQDbcCN2hJ8yC22gF6AcDteVAQaPNQAeDHKW9" +
                "FQVCUFz/Hs6DsmRNaAntC+VJO6hATiifG19BFagBtdQs8iO5mv" +
                "xAe9IsMhPqQUNorDSBK/TBcCVcBVfDtdAG2moroD1cr0yDzhCG" +
                "G6Ab3KwdhR7QC3rTFdAX7tSH0Oq0Jq0BQ9VbaA+jmFVM7Q5Dwm" +
                "FaOyw3hlU4rM+i1fD4GNka2oyl5UircNi8LhxWh5slcD9e1KWN" +
                "zSPm9YhVGftqPRtzG2Ld32hltTZ5TuSqm/TFag4vT1dHK2cwXS" +
                "1KzFvTSoc9G8OKpUYHbaXI0Zrz/AWYN1CZj61zrMJhko65m8JR" +
                "Gz3sa28M5jTA3/vacV5a1y7RFru11B287Ahp7b1Wv4vtrYq8TD" +
                "4v7S1Saw4/uzy0lB6Vtdvpzcxe8vhFCvQTvSGvU8dt0Qh529dW" +
                "2Eekh8yRbak3w2BsZYj6FL3W5pU6A+6CYcirUbQNGQ2XhMNQjd" +
                "xNyljzlMtgDNQ36rMrGa/ISMgMXQkj4D6rB/LqbqML45Xe3vrZ" +
                "epjch3UYr+5hWIGiP8Of8X58Bs4rfMaOarY6C2tVNAD7D7FivM" +
                "LzmvwJlwC+D+MV9sF2/YC/70l7/pb1xJngFc9nWG3HnAvsmlBS" +
                "YIW8Gsl4ZVj6EsYrjtUi7TTjlT5bH4Y1Oa+wT7rCcGiHpVP4PR" +
                "7EktGMVxKXCng+kLes0fUuVoA9znjFsEobrFSE8uZHjFe8ZhX8" +
                "Ia9oRYna67Qt45XzjJxXmCKvrFqMV3jcmYy1sXJ4dS/caQ00un" +
                "ps4PVae9sG6q2FDdQ6kuahI+ocZgPpVXpbrSti1ZG2gExstZX+" +
                "Be4dG6j/1Tph8fe0baDVExTaEIrhezyNT9qGYWUUD3XWF2tdGF" +
                "bmeIYVVDanulhhKrFCGziM2UCZizaQpw8IXrlYGeOgP2fNIWYD" +
                "ZQm3gRIraQMZrxhWUFcZooUZVrIuYiWPWgqshA3Ec2kD8eg2/K" +
                "ENDL3FbeAmZgNtrGwbiFgNYVhhXgXZHreB+st0psPzLsIGYtkV" +
                "NlZeG8iwoplBNhCxy1B22lgpHxizzRaI1YPwEIxDrMYDWj7SyZ" +
                "ilNWK84ncfC02VXZDDsRph9ghdA530z+FhhlU4nHZR6DsHqwmC" +
                "V/KLysIv9RnBK87rHIYVpqONBvyKAKyQV3tp93DApvxsY4W/Pt" +
                "a/Ze4v8XglbCDUNk9julCHaKz0pYJXsleP+HnFa9q8esfPK9wj" +
                "r0y0EJFYCV4ZfZwn/zAWr3BvY/Wtn1doAydyrJqqzF89DpO0Q1" +
                "YVmIZ1H4MnaW3k1VTtADyifUvKwHStEfNX2pfGXKuGfR+rGtuH" +
                "9nn7MK1s6HvtoMfGZ8MUeTSV8crz1j/J9Kz2VTjmZm0Pzte+5v" +
                "tveAuODdQ+D+eyMX/F0x1aOJWN7gh82poMK28OTOZeuLHR37my" +
                "DtSL17K2H68KO1g9Ktt5gu+fxbZKwvPArYpZSraI2gLmSw/BlY" +
                "JaK7jttHK+s/6hH3z+OBvmST+KnWOm2/nm5S5WzF/FxGpvjD5f" +
                "4OkP118dSgSr0EuY/h5UajaJzoM5vrMXZP/sD7o+hPpEa+OrP5" +
                "f3XZYxIDGsGK9crPBIqjFYyHtzm+2v9J3Iqxdtf2V1Ef5Knc91" +
                "4Hbmr/SPUbPXgKYq9j9qiw2AajCtJfNXeM781Ydpu9JQHeofeT" +
                "U7lqG/MtL13cJfubxiNlBodtsG6vv8/irEciP9VcdIG4i//hyr" +
                "XyL9FSzCI+mvbBuIOXVVxCvaBprNhL/CHPRXPI3yV7hn/upr4a" +
                "94HfRXzAZCWcYrfUe0v0KsBvltYJC/cm2grdn1vcIGYt5LzAaS" +
                "ubBSfRUWw3J4LbQFtcUKeFldBsvUpbCEzIelqC3mhUys/Yr80r" +
                "kNVFTpdV5OGxTBs81+XmGdVxyN+oyH612c3Aaxv7NQ20R5JbRF" +
                "ojZQXRXI4nsTtoFnY9lA/YfofDXLc7w899ZtHeh5x1ejeRVqaf" +
                "OKaXbGK9Tsi2C15NVxyauNTLOnPcB4JXSgzSt+p/tcXmG+5BXe" +
                "abeZ7vLKkNqCtxCbV/2CeeW8g8OrYB0Iq4J5pf0YjqMDc+eVGk" +
                "qGVz6UJ+bOKzI2ilevc15t0OuFWsEGeJPs0TPMUrAe1sJbsE7d" +
                "DGt4z39I0GOTT/nx+2QreQPvl0PW4dm7+NtJdvGSz/h+O/k7eZ" +
                "OsJxujvpS/kU3kA9dfebWFr97e5P28h1dTE+WVXiclaRHWysb0" +
                "sLNzYeTEBN4oOucNwSvlsM0rPQM2wiYowZ9mkhgLG9MoNaYir1" +
                "CzWxPJVjYW1h5h/oppdsErvnd45firt3m+ok2WubvxzOXVYy6v" +
                "XM3u51UCb3VrpA2M1OycVyP9vBJYBfLKo9n5eQzNrpX35Dmanb" +
                "e8I1qz+1CekrtmV/4l23J5tZnxSt+m9SR/IRfj0U79CC1p6oh9" +
                "GsMKUzYWD9FS1NKldiZbKfoubZppBPWcjZU1zfd0iBU9X2Dl0V" +
                "yhwBb2pcSrQwXGq0qxSvQduVw5JffWXawce1NO8ArflvPKetLs" +
                "Sy9kMSaGFeZeQs+DajZWYizMsIKmeNV8wSvC4i6dSHk/r9wYk/" +
                "BXLlaesfCsSF5pM84dr8j0XHh1NEVeVckrr0TsNpexcKb85h93" +
                "eHWv4BVtKbF6Sj9l/hPzedyCVLLjFrQ11+wSKxa3wDo5fhso4h" +
                "YRWE2QNnBWkA0UMSYvVizOXmBYpWoDM/MTK8f3uVhNFFiZvzr+" +
                "6gwdRCozXpEqzF9F8irUXfBKW0WqerEyTwT6qwmuv4rmlXmySP" +
                "urVvnprxx76vgrUk3yarSNFb2bDjd1xitSg2HFeEWH0FF0qMAq" +
                "rQPqwOqI1WqrF2TSkTCCDqbDGK9IzWisSK142oL5q8LDyqieIl" +
                "aDChYrwStk2hns6/qSoRksHkgaMG1BZIt0NkyP8I+vR8RmTsTp" +
                "zSk2VhHXnEzFt9txiwht8Uui2sJokqK2GJqg7pkcrS3ixwMjrv" +
                "fFAwn/euFx0oifNXbanCQjB0DKRN1vTRJ3i4EV0+znZks2Hsjv" +
                "3iJFrMYULFbE822xODtPp4Waszg7H1/hyJLgO5HmUbx6I3Wsgj" +
                "V73rFKTrMzG5gSVvcXCq82KOlKCdKBXEf26Kc8bU4ScfaguIW2" +
                "1h+3CL3gj1soJT13m8ewEnGL3G1ginGLfBhfBcfZjRkJXj03Oa" +
                "xEnD0cI86OqYwH4hucIZ3teCDXFlVZ3EK5MLQMa2WA9GmoLd6U" +
                "cYuIeKDnHp54oDZZxAN5fmDcwo0HRs4L87OAeeFE44FSW0TFA2" +
                "PqwATjgcaCoHigPI8bDxTaIn48MCzj7Jj64uxKOViJ33MW8Piv" +
                "UgFWkBtQs98o4hawVGh2N87O4xbrk7IXUrNHfeMXFV0baCzMs/" +
                "WckperSDZ/+m78uJt8Fz5TER1j8sQeGVYbzgVWf1R/pWTlfk/z" +
                "QAFjdRPf9yb9yJ3kDtKH9PW1Ocmp55kZl/6Kx9GZvwp40xFBWG" +
                "HtCH8V45kKyF8pC1P7PqyDBYzV7UJbcBvYH5aTPfzNVxBmk5fh" +
                "+GoApkttbcFsoKMtNsWfE1FGRmIVrS0KF6tUbaD1j8KwgYiV1B" +
                "Zkj/Kgcn+ktmBYkUGIFWoLgRVqi7fJOsgk70IrhhXTFuQzvu7W" +
                "xuoBW1soY4W2sLFytQXv5ShtIbDKf23BeJWKtgjNLGBtIecagZ" +
                "ChZAQZZmOFJRwrTKu6mh0yPLzaLHglsMJ6WRG84r3JsZrAeOVi" +
                "FYMZlYN5FYxVEK9crJzcUtjaXfL4Uj+voG4sXkFLmUqs5JmDFT" +
                "+jfO9ghcfF+T5w/hFqBPHKxkrWuTbqKomV0y/D+f4hXuaoBVhP" +
                "xsNbfHzlmReWV9hYbVHm2P5K2EBZLrGK9ld/NBtIF4ULacujDR" +
                "zH9xORV/j1kxz55oG8YuMrj7Z4R2gLNr4SvHL64lkXKzG+EryK" +
                "ry3+5FWuvHqYP/1qe66Rn20kk8RcI/NXynp7TgTryHW3fE5ka/" +
                "Acvn8sTCb750T8Y2Gvv3Lj7P6xMGrjNfkTZzdXpRZn99Xxxdm9" +
                "/kpZllqc3eOv+By+skXZrDjqW9lGDkRqdjoqisfbeD5Xe3RYIu" +
                "OrvG7m9gTsw/7kbaC5tWjZQPmm35CvyUHyJZHvSb6LGl9976v/" +
                "BZa9m+xYOD+3vGCVqmYvJKxOk5PkFKZypTL5LQGsthcUVuaafO" +
                "LVV0URK8VZXaThmyp76EPS8qE3C72Nud+6NlD7UpblxLOBWpJj" +
                "+nj/PTAT+O+Bi1Xi/z0wvyksrHJbH6jtj/NVbqDy3wEibkHnR5" +
                "QHzIngUfy4RZTyzqtmT45XCXyZNlarCg2rBSnYwA22DhRYsXVM" +
                "2OI6ey2nMxYe44lb7JBxixEibsGw8sUtPo1cbxE/buHqQH/cIo" +
                "GxlU8H8pyEdGDMuMU51YGprrdwdaBYG6McJCfZegtyhtlAusUb" +
                "u007ymyg66+YDeT+6r24LPbZwFjzwonZwJjXRP3/KjkbWFDaIv" +
                "l5Yb8N9M/hK8d946vTjFfO+OpUjPHVzvjjK+VXxiu6O/XxVX7x" +
                "KtV1TAXNK7pHrGPiCPgUlD1/xd/ubMD81a6C0oHWFcnEmBL3V1" +
                "bTIhZj4vNXKtjzV/RnwSsXK+VUjLnGj3LpkV8jsSqYucbEsSpq" +
                "4yt6TGgLgRU9aWsL//gqcG3Mx/F1oKoVTuw2CV5dUxSxwnddic" +
                "enxHoLtTisoP/Ndb3F7mRt4J+8StEG8rlGtbSKHp6eFnlqFa8N" +
                "jLneYk9BYWUNyCde9SuK/oqtOaPyX+l6RvScCOLHv0ExJyLv94" +
                "m8Uq7Ah8DVJP45kbi9XTlGfpJzIpj65kTo/+Rx1JxIrPWBf+y5" +
                "Rvg/2DmfYQ==");
            
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
            final int rows = 45;
            final int cols = 107;
            final int compressedBytes = 1620;
            final int uncompressedBytes = 19261;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtWmuM1UQUPjN35t5uL+4iC8kaQBAfiywgDwVlVVQUUFFRg/" +
                "hWIDxcoiKCaKI/QBajIYs/jA8Qlg0axcQQfxgWif4gaFZZdQkI" +
                "GMQoBhWNCugCPkI9nc7tbW/vo/fe7rYlnWSmZ6Yz03a+fmfOOS" +
                "3ZCiR2hoaJdOglVGGugX68EY8XYGs72YflLhiBZRvZRt7XNL6L" +
                "tGJtO0wgn5Ed2O82slfM8DHZQzaTLewqMdM8aNCPfAX0ILvJh+" +
                "RTLUeC81IS2Wlrv6xiA5bXytoNcGPW0bMwz5Vy0myt1jR6Ssrn" +
                "pFpjS0R9eEVLjju5Rh6nwq2W1vswP4B5jqhRUcYhYZ4X6wdnZZ" +
                "3xQnudPydaL8E8zuxznWPU7XAH3Jmuk2V6yWZj3ptuZfvknI1m" +
                "y9zMmfhurYjEV2hdmsjX7vsaWOGxRfMpGViV+KRbWYOUOmxzSq" +
                "xSvBJyildfGbzCLHiFRzuvHrJgf0DHKj+vLHezs/gn0HlVLFbq" +
                "aLcj2DdBwIp9K8pV+LQMKkARtRexdiZN6ljBAFoJtVSl1bQCW5" +
                "fASL0HVeBSvgfr4zE/JtbqZlFOh7ss6zcTZtAvhRTTsaJVsr03" +
                "9LH0GmiRB2MeIuVh9C0YBWNd4HRPJlbQ05R6yWNfWAiD4FwDKx" +
                "gqnuINbL8Yxsgel8MVUloAk22zLYZpcLelLt5C4LY+qigrzXp/" +
                "zGeL1WyC81Gus2MFF4H5pkA9XAm4Y8BEmITl9TAFy1ssc98L98" +
                "ODeL8doOu/dWQOmU1m0uNsLWt28gp7rHe8G3uDpAO7llfB0IH0" +
                "iEBCaG4utVbsb3FG7tCxEyhLXpmjFBcr0um4VlXAsJrhF1Zxta" +
                "Sdarpcx8WWNV3Am+y8YpvoIvpwxvV6iL4LMc+nj4bRtlBnhYtX" +
                "pm2xxWpb8A8K2xbx/gVsi9ZMrLrStigJq3lhxIp9ztptnOnl2K" +
                "++cPB4QNhtdrUhlFh9l4FDjQOr7x1YzQg7VrQtpDrwB6sOjPdz" +
                "oQMfL6ADD3qlA9Unu4hXT4SSV4cydOAsFzqwMey8SgwOJ69w7Z" +
                "vYYfYr6WC/ofyU0BFDzfOSV7Q2zSuU8vPqF8dVgmZbLPMLKzqs" +
                "vPEJSyzBiAfGLf4vXZSXl79rPqdw8YpOKodX8ZZETXq/MrCiEz" +
                "N5Zd2vCvLqj8DzamUYsWLH2Al2lHUyjf3L/mTHEyfZqQwNC7b+" +
                "R5B1m4q+yn/sn4hXAqup5fCKU2CaBgrpiL+nacqIjPMmr2Bkil" +
                "f0JtIK4+280mO3KV5xgr2lXQ8xK68KxW4NXinDUR7l9gmUkbaI" +
                "U09HDKovZjN2m8YqW+wW5ckZo6fZai5jt26xgnrMxtc+EbsVUp" +
                "bYrTJKj90iVn14dfxU5jeRcnQg721ZyWfL0YHqtsJjSooH+maz" +
                "x0sapSzPamfIb5QJc8fiNY53Y2l3PZn6yenmX9Hl5dnsKV848S" +
                "Mi+FJhXsV2FODVwKDbFv7tV7H2svarQUpt2g5Ux3rgX7UG3g70" +
                "j1fDPNQ6U+xYpfwrVpfDDx8S+VfdhZVus/M61smHGjY7thS02Z" +
                "VHipj/QDk2u7rfb9vC6/8tStOBxv8Wwsp4Jt2alDYF22i+tzV5" +
                "5njbxXXe8frdVJ528or0Dgev2Ltl7Vf1wJSlun8l1sGFf6XvV3" +
                "n9q3Fe+VeqCzaW4l/pvPLHv7Lb7MX5V0IHXp2OW6R0oLIjtw4s" +
                "xmY3/KuujFuUogN93K9KstmVnKNo0rAt+PPpf2NStoWbf2OyzF" +
                "ilBSKF27YwUoXcd/gLBlZy3srS/mOKsPI6biHRWUVQ76nzSU4d" +
                "RQ7Zavu1EKeQY/UyK2BFOr8Ld19Kju4arPzzhcvyJLfyV1Ixpq" +
                "znS/l+ddCruEVyTISVhVevBplXp58OLItXq/Pg+Jroseb0wyq0" +
                "OrCFr+Ov8zWkgzfzDXw1X1+uDuRrvdKBEa9s3Hmz4Pr5yKvkhI" +
                "hXRaG5MdqvAoJEq+5fIXsC6V8lV0e8su5XpuTd/xbtntnszRFW" +
                "OVkm1io4Mabk5girotY573+3/KfIZvdDB/LDHunAnz3TgR9FWG" +
                "Vdpzz7lZQdWGEpsOJHrVhlmaVUrLZHWBVrW/BjxfDKQ6zaIqxy" +
                "YcX/8kQHdnqG1b4IqyKwtOhArEmshGzRgaalcTI7r7BfoGJM4c" +
                "QK/gc+yFoh");
            
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
            final int rows = 45;
            final int cols = 107;
            final int compressedBytes = 545;
            final int uncompressedBytes = 19261;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrt2jtLw1AUB3BP0lzaBlulOKibgqK7i4rgKiruKriIbqLUVg" +
                "Q/lqiICFrB11BRfCC4OSg4WqlWY0ltm0BMX0nNqf8DTZsWAjc/" +
                "zrkn91bT7IPO6dZwdkCbhc8J/XhW/FX5oGvaoh2Lq1zRHp1oJY" +
                "MuzOfqg+ZoyPHcezCmNWAYrZSMvRUdOW3ldOSteIbyWbhPyVJW" +
                "dJy3om2zFd3AygOWX+XXQP3c0ir7Paxqr3G7teSVIOSVd0JIyC" +
                "s+IUVtLX2cxsK9Bgq/UHxPQlDS92JVA0Uge7y0roF0alUDfc+o" +
                "ga7UOFXelxOFsRyWn1fSin5cQl55q7eoLK9EM/LKlbwKUUq/S+" +
                "+/3r9H09k959EytwrDiqlcC+YrD+u0Iq/YWEVgxaYPTFNKtBWt" +
                "KMPNiu7+TV61I6+YynXo/YL60z2Esq+gFJECho7CX811pTCsHN" +
                "DpRF4x0uqCFROpbqwHsrHqgRUjrV7UwEaJEnuNfbCq25Nkfq+x" +
                "H3uN3rcqfHJu/2oAVn80nw02zljY18ARMWyXV2K04rwaQl65ZD" +
                "UmJsWEozVwHFYu1bgp9OyM9aZNPbsDa7ewcs1qBlZMn4UjYhZW" +
                "HsyoOd0D+1d8Mkk16M2jBjKSs18PXIBVHZ+vcuuBi1gP9PwMtW" +
                "waS9pYA+W36uYr+dWr8xXzbiL6n/5vwdxqFVZsrGKwYmMVhxUb" +
                "qzVYsbFahxUbqw1YcYmmb+W9ixs=");
            
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
            final int rows = 45;
            final int cols = 107;
            final int compressedBytes = 534;
            final int uncompressedBytes = 19261;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtmrtLA0EQxp3ldhMVomBlpxbWFjZa29sJVtqJqCiStL7xUf" +
                "msfEQh/4IxivgkghqLiKKCtYWCjWAKEeLlDNFgolHvcjfhG9hl" +
                "lyzHMb/7ZubmogYoFteNXuJZjO7SdrdxmE1G26lVNOPvZ3Sjzx" +
                "fG+pgOKWisNvUR1scpRYz9tTEf0RWFaEsNfrnKJe3QSQ53cw4i" +
                "2U0NQVdsWA2DFVNyI4lZlL7vhEcfJaJCFH+cEO6/XFeUwbcm0B" +
                "mFrtiwGgMrNqzGwYoNqwmwYsNqEqzYsJoCKz59C7WgZtScmk30" +
                "LWRATcu1//Yt5Ar6FjnpZP5Xp7tzOOPHE+0Yuqs/vMc2wUeOiY" +
                "Hr0i+X5RJF5aoMyEXEQAezCiJf8chXOquNpJ9M+yaidYKVNbpK" +
                "rcz7fhUCK4ti4BZ0xYbVLmoLJvlt76cTWge8VMD5ah+6sigGHq" +
                "Bm59JjUk+fehR9GfoW3iz9jH59dIlePO8Oov8MHzgpX2n3KkZR" +
                "7dGkmv0BMdAaE77vYqB6FV7RgxhYqHWgywVdsWHlBis2rDxgZQ" +
                "0rV7nZrGQjWNljrkr4oIBjYBV0ZVNN7/1WddXwUB7zVY3p+Yqg" +
                "KzYxsBas2LCqAys2rOrByhaWBqvULsnKWIeNOZJWaTQkWGW8Dl" +
                "g5U1fN0BUbVi1gxYZVK1hZ9H7VZjYrbROsHNm3aIeH8qgrH/7L" +
                "ycOK3gAEcoti");
            
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
            final int rows = 45;
            final int cols = 107;
            final int compressedBytes = 654;
            final int uncompressedBytes = 19261;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtnFtrE0EUxzPZuWlBoT71Vei38AMoqA9+AUEffAkotmpNwa" +
                "/Qiyj0FgsVW1CkfZC2FF+0gpcqKwlqESyl+FDtJVqa0lpcx21s" +
                "DLG51JntDP0f2M2Z7LBJzm//c87MLhFXgwpGLwQwK4xMbHv+P4" +
                "9PkQ9qnw795+QJeRR6Y2qbVNtL8ipsvw/3z8g7MkrGxbWSs2TI" +
                "Y/Kiim/zFkTKsRLXg0Akt1ixAdHC+v+XFesDq2pMtNbSmycUq7" +
                "u6deXVg5UzY+A9sDKiw0E6VaG2eIMoWZOvHooHYnhnXYmRmnV1" +
                "H7raG4s3l9Xla0TI6XzlQ1emWYm0l9RSB7aClZVjYAYRikxX6y" +
                "QnZtTrRr69WdLjc1Hro3W/YNraGvuT9jPOepN/fO9p9bqKXw73" +
                "F3G9OzMGziFCEc6vFtR8d158JT5d1FQHfkFtsUej7hJiYFPNrn" +
                "S1rE9XFLoyo5qs+FZoeT5qC6dpriAGzrDaQAzsyVcyxk9pXQ8M" +
                "kK9snF/JBkQoOpNHCz5tDOnU5SkdUtvB+JH4gb/IyV3xPow4ay" +
                "DViPvCbs2vQk9bvpLHkK/MsJLHWYr1sh7isztsgHXjmTOLWZ3g" +
                "7byTd2w9H8jbwCoq4zdr6p0wMgaeAStDujrLT+udX/GTYGWE1T" +
                "rJyfO4L2ykxj5nog6k8zKBdXbbjaa2r4JLO/ToR5ScmS03IQaW" +
                "6Oo7XZPNdFVeoT/oCs2pd34W92Cxov7ZXX3KJsVqfRS6akEMbK" +
                "nZ87VFErWFM6xugJUDY1xbhVyDdXardBV6+taYOqArM6zkbe2s" +
                "boGVIVZdsrscK9lTc74aAytDrPq066oXrIxUFincw9/XtQX+M8" +
                "EgKzn0mxXmV3Zb7Bd3CLuc");
            
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
            final int rows = 45;
            final int cols = 107;
            final int compressedBytes = 570;
            final int uncompressedBytes = 19261;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtmjlLA0EUx504szvTC4qCn0BsRCw8ULTzqDw+gZ0giIXoN/" +
                "EGG/HCJhjx1ohHgkQNia2FoIIgihYqrOt6gJpEo/twx/wf7DBh" +
                "Nwm8X37z3uyGLVgvwSJWgmBhdmSPh858m60zvzML2EfQPnZZyH" +
                "kdd8ZNFmNzbF5OfPqUKFtiO9aXwfYtRPLsULCaBCs6VvxMTrEI" +
                "v3CHFT8HKxpWctptrwQDK4rgV3LGHm/s455f81vLkrPvrxBZ76" +
                "6//NG3PPA75Jo6pP/Jq7ff/YtXzjzojKEPZtheJTQGXtGzmgMr" +
                "bepZOB1WMgBWmvXs8+gttGG1CFZ/VLGWkQOP7YVXsBfOyDVwFa" +
                "zoWMm1J1bwSvt6tYUceMUrQxrCrlfbybwyFLz6x/UqAlY0rOSB" +
                "26x4AKy08SoKVkRexdxmJXLAShuv4mBF5NWJZZm9z6zEmNkjRn" +
                "/t1RBYfSfMvnSuNjpsVqdiWAyKARYRI2JM9IOVV0Pl8HDqK/ge" +
                "suTVeqXyfuuVyoVX9KxUgcrHGqjxKlmIHHjFK1VkNBmNbvbsRg" +
                "O8olsD+ZkqxjORTNwLm8T3blVJhntVCq90YKXK5Hjy58J4fuWp" +
                "Tq885dkKZOj/1itVCa9owjzODr7Oszc+n/d1J36fr8sZO/9gHa" +
                "jK8N6iGr2FZpWrVgevEEn51SEH2rCqRw68Uq9UQ+o+0KhJ+35g" +
                "OeqVNj17I1jRsVItrrJqBitCVm2usmoFK9L9VTv2V/rVK/zfwq" +
                "uR9Qhbf/Vp");
            
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
            final int rows = 45;
            final int cols = 107;
            final int compressedBytes = 522;
            final int uncompressedBytes = 19261;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtm00vA0EYxw27s505IiFxcXARicRF6GfADZ9BUi8RCRcnNx" +
                "dvcaxIJCKIEBGqqYN3bQ+lXuMuKvEZ1naJoBqtznR3tv8n2cls" +
                "O6f/b//P8+zuLAmzAdMOkkiPLGB+CxIn99aYtOfn5JDs2LM96z" +
                "i2jiiJ2ed39nhCbskuCbF+80eQaxIhF+afQS5NRHZ1wp+zd1ZD" +
                "YOXOYH05rBlV4pqrLj1fZfz/H1+Nw1dyWLEJOk1n6Uyalb5Ep/" +
                "TFQlnpQbDKJehcXqsDFqtJY93YzO4rYytfVsYaWEny1Tztop0i" +
                "cyDtAKsi9BrBzN/KR35fWz5sj4NQzUX8FqCBMqxWoYGHe3b0Fr" +
                "J6iw3RrHQ/WCnjq22wUoZVCKzksdJSbJ8ktFcxrLQXsJJUr8LC" +
                "6xUBK2VyYASslGF1AFbKsDoCK2VYnYKVM5Ht2e3HE6gzKKS0r6" +
                "LwlTKsYmDlTLA4NPCwr67gK1f2Fkko5KIceAMNPJwDH5AD5bBi" +
                "j6JZ4V2jS+vVExRShtUzFHKuXrHagutVDXKgfFaszhjDfnY3s+" +
                "KV2M+ujq94lcg+EKxkstJSvB57Y5Rh1QBWXu/ZeSMUKrqvmuAr" +
                "z/uqGQo5dy9cuK98y/CVPFa+lTQrUTmQt4CVjOCtX88qErnnQH" +
                "yD6oyvWLfIHMjb5PqKt5c0q16h7xp7JLPylyarsjce+zKL");
            
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
            final int rows = 45;
            final int cols = 107;
            final int compressedBytes = 437;
            final int uncompressedBytes = 19261;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtm79Kw1AUxnskl9rrA+gDObiqCPoEIgg66KCTgk6+gH9qW/" +
                "ARRKwUF61orUOkog6CrioiCk4OMQZxqRiwSZsTfx8k3JCQ4fvx" +
                "nXtuwpWK58u5t/3iOk9ek+RMrv1zIxifyIHsBKOyf1T941Tqwf" +
                "VVcD6SS9mVPeeh6S0Xsi81L1Ry7qEW1DXz2107gEPtklTsoCmY" +
                "vNkQ1xTNllk3pVZzZTbJVUyshr5G7o/3/1IDJ2AVD6vvUWSs7D" +
                "Cs4pAdCXvCGcelxNTAUeYrJbkaI1cpogmrNPcW9IF6WE3CKqbe" +
                "YipqVt15WMXEahpW/7gGzsJKDas5WHVGId/Z53FIda4WyJUaVo" +
                "uwSmQNXMIh1blaJldqWK3AKpE1cBWH1LBaw6F2yRbCnuD/VXLm" +
                "K1vkv7CSXJXIVYpobuNBUnv2XF+rNTDXSw1Us746hJUaVlVYJX" +
                "J9dYxDqnNVI1eJzFUdh9q4FnY/98plH6PbK5dlr1yn1lf4l5Ya" +
                "eINDqnuLW2pgTPPVXdSsnDKsOjRfPeOBGlYveKCG1SsepLi3eG" +
                "O+UsPqHVZaWPVkYKWGlcAqDmU+AN7Bxys=");
            
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
            final int rows = 45;
            final int cols = 107;
            final int compressedBytes = 413;
            final int uncompressedBytes = 19261;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtmk8rBVEYxr0Mow7KwnfwEURiYyd/y2VzsfIxEBtlaWljY8" +
                "OChdBQ3Cj/FleEr0D5DOM0yea63WhOZ96Z31tnOtNMs3h/Ped5" +
                "zmkkim0F76ZZqsFnXFPyIG/2+pTMb6QiR8nsxI4rO+7kPrl/Ta" +
                "7X8iLHchp81HzlWc7lNm5Y8hhT9bsTmZbvWfXX5/9g1Sqw8lOm" +
                "jR5kRVc/s9R0ZQy6UsOqA1buWNls0Um2UJAtusgWalh1p86qH1" +
                "Zq/KoHVp4yey89yFi26CNbqGE1AKsi+lX7Lqw8+dUgPchxDhxC" +
                "V2pYDcPKabYYIVsoOLcYDffDg/q6Cg//yircg5WnbDFGD9SwGq" +
                "cHOc4WE6yBjvxqMvW98Das1OhqClZqWE3DSg2rGVipYVWCldNz" +
                "i1nOLRTkwLm0dcW/Md72wmV6kGO/mkdXTv1qAb9S4FeL+FWB18" +
                "BlWKlhtQIrp361il8VUldrsHKqq3V0pYbVBqwyfyqx2eiNYIku" +
                "5divttCVE13toKtC6ypCV2pYncFKDasLWKlhdQkrF9X0BUTNz4" +
                "o=");
            
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
            final int rows = 15;
            final int cols = 107;
            final int compressedBytes = 165;
            final int uncompressedBytes = 6421;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrt17EKg0AQBFCn9v8/JUSRNCFCNCkUg8ZOiCBYKoF053GkS8" +
                "RKZGQG9thrrtjHFofIfIPM/AnueNqzcP0VZxxcF9i62Epxc/fK" +
                "nTFKHBH68c8rD5yQmMUgN8r8dNawSmS1TfxCM9jxXpXaKxqrSl" +
                "Y0VrWsaKwaWdFYvWRFY9XKisaqk9VG/6teM9jxXg3aKxqrUVY0" +
                "Vm9Z0Vh9ZLVGvAnjL6JA");
            
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
        if (row <= 44)
            return value[row][col];
        else if (row >= 45 && row <= 89)
            return value1[row-45][col];
        else if (row >= 90 && row <= 134)
            return value2[row-90][col];
        else if (row >= 135 && row <= 179)
            return value3[row-135][col];
        else if (row >= 180 && row <= 224)
            return value4[row-180][col];
        else if (row >= 225 && row <= 269)
            return value5[row-225][col];
        else if (row >= 270 && row <= 314)
            return value6[row-270][col];
        else if (row >= 315 && row <= 359)
            return value7[row-315][col];
        else if (row >= 360 && row <= 404)
            return value8[row-360][col];
        else if (row >= 405)
            return value9[row-405][col];
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

        protected static final int[] rowmap = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 9, 0, 0, 0, 0, 0, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 11, 0, 0, 12, 0, 0, 0, 0, 0, 0, 0, 0, 13, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 14, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 17, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 18, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 19, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 21, 0, 0, 0, 0, 0, 22, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 23, 0, 24, 25, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 26, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 27, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 28, 0, 0, 0, 29, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 30, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
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
            final int compressedBytes = 108;
            final int uncompressedBytes = 38977;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrt2MERABAMBECl6/w0YPxPdivARWIkT3tBsahvAMw35AMA4H" +
                "2EfAAwX/gm/+n11b5//UF/0B/cP+crP/mD+gEAAABgDv9fAAAA" +
                "AEAr/5sA+jMAgPeR9QMAAAAAAAAAAAAAAADA1QEl6NHW");
            
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
            final int compressedBytes = 85;
            final int uncompressedBytes = 38977;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrt3KERADAIBEFKT+ekgxgEhNn1IF9eBADALvl2qvfV/wAAAA" +
                "AAAAAAAAAAAAAAAADb6TMA2HcAAAAAAAAAAAAAAAAAAAAAgJ90" +
                "91X0XYa7/Q3bqA==");
            
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
            final int rows = 644;
            final int cols = 8;
            final int compressedBytes = 50;
            final int uncompressedBytes = 20609;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrt0LENAAAIA6Ce7ue6OrmbwAkkAD/1rQwBAAAAAAAAAAAAAA" +
                "AAAAAAAADAMhArH2I=");
            
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
            final int rows = 31;
            final int cols = 125;
            final int compressedBytes = 162;
            final int uncompressedBytes = 15501;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrt2UsOgjAARdFOVRRF/KCuh3V16S5BGbbv5E0Jg3sCTaCW+n" +
                "PrrhbrZeWvq5gzN+bG3Jhbg+Z3peLM5213Xc/KNm/+UirOfK9U" +
                "nPlBqTjzk1Jx5kel4swXpeLMr0rFmV+Ucp4bc+vOfFAqzvypVJ" +
                "z5pFSc+VupOPOPUmnmW+f/eQfP+ahUnPlNKd9krHvzh1Jx5rNS" +
                "3u3G3FoyL18ovw8m");
            
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
