import os
import lizard
import var2line

groundtruth_path='/mnt/autoRun/bugs.txt'
trees_root='/mnt/values/trees'
values_root='/mnt/values'

gt_list = {}
with open(groundtruth_path, 'r') as f:
    for line in f:
        i = line.split(',')[0]
        gt_list[int(i)] = line.strip()

logBuf = ''
available_bugs = [3,4,6,7,8,17,22,23,24,25,26,28,29,31,35,36,37,38,39,40,41,42]
for i in available_bugs:
#for i in range(1,43):
    if i not in gt_list.keys():
        continue
    gt_file = gt_list[i].split(',')[2]
    gt_lines = gt_list[i].split(',')[3].split(':')
    gt_method = ''
    src_file = f'/mnt/out_put/{i}_llvm/mysql-server-source/{gt_file}'

    value_file = f'{values_root}/{i}/values.txt'
    if not os.path.exists(value_file):
        print(f'bug {i} : no value file for bug {i}')
        continue

    methods = lizard.analyze_file(src_file)
    flag = False
    for method in methods.function_list:
        if not flag:
            for gt_line in gt_lines:
                if method.start_line <= int(gt_line) <= method.end_line:
                    gt_method = method.unqualified_name
                    flag = True
                    break

    method_id_path = f'{trees_root}/bug_{i}/instrumented_method_id.txt'
    if not os.path.exists(method_id_path):
        print(f'bug {i} : no instrumented_method_id.txt')
        continue
    with open(method_id_path, 'r') as f:
        for line in f:
            mid = line.split(':')[0]
            fname = line.split(':')[1].split('#')[0]
            mname = line.split(':')[1].split('#')[2]

            if not fname.endswith(gt_file) or not mname == gt_method:
                continue

            tree_file = f'{trees_root}/bug_{i}/{mid}/tree'
            if not os.path.exists(tree_file):
                print(f'bug {i} : no tree file for groundtruth method {i}')
                continue
            
            # mapping var to line
            var_line_mapping = var2line.Var2Line(line.strip(), tree_file, value_file)
            ori_mapping = var_line_mapping.direct_var_to_line()
            line_mapping_without_args = var_line_mapping.map_args(ori_mapping)
            var_line_mapping.print(line_mapping_without_args, f'{trees_root}/bug_{i}/{mid}/line_rank.txt')

            # rank
            counter, gt_rank = 0, 999999999
            for lineNo in line_mapping_without_args.keys():
                counter += 1
                if lineNo in gt_lines and counter < gt_rank:
                    gt_rank = counter
            if counter == 0:
                print(f'bug {i} : no tree generated for groundtruth method {mid}')
            else:
                print(f'bug {i} : {gt_rank}/{counter} method {mid}')

            

