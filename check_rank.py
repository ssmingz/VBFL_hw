import os
import lizard
import var2line
import collections
import sys

groundtruth_path='/mnt/autoRun/bugs.txt'
trees_root='/mnt/values/trees'
values_root='/mnt/values'

gt_list = {}
with open(groundtruth_path, 'r') as f:
    for line in f:
        i = line.split(',')[0]
        gt_list[int(i)] = line.strip()

logBuf = ''
available_bugs = [2,3,4,5,6,7,8,9,10,12,14,16,17,20,22,23,24,25,26,28,29,30,31,35,36,37,38,39,40,41,42,44,45,46,48,49,50,51,52,53,58,59,60,61,62]
ignore_bugs = [3,6,7,9,11,14,15,16,18,19,22,23,24,27,28,29,32,33,34,35,37,39,40,43,44,45,47,49,50,52,53,54,55,56,57,58,59,60]
available_bugs = [4,8,10,25,30,31,41,42,46,61]
#for i in available_bugs:
#for i in range(1,63):
#    if i in ignore_bugs:
#        continue
#    if i not in gt_list.keys():
#        continue
i = int(sys.argv[1])
gt_file = gt_list[i].split(',')[2]
gt_lines = gt_list[i].split(',')[3].split(':')
gt_method = ''
gt_m_startline, gt_m_endline = 0, 0
src_file = f'/mnt/out_put/{i}_llvm/mysql-server-source/{gt_file}'

value_file = []
if os.path.exists(f'{values_root}/{i}/values.txt'):
    value_file.append(f'{values_root}/{i}/values.txt')
if os.path.exists(f'{values_root}/{i}/values-large.txt'):
    value_file.append(f'{values_root}/{i}/values-large.txt')
if os.path.exists(f'{values_root}/{i}/values-small.txt'):
    value_file.append(f'{values_root}/{i}/values-small.txt')
if len(value_file) == 0:
    print(f'bug {i} : no value file for bug {i}')
    exit
graph_map = f'{values_root}/{i}/graph_map.txt'
if not os.path.exists(graph_map):
    print(f'bug {i} : no graph map file for bug {i}')
    exit
sbfl_result = f'{values_root}/{i}/ochiai.txt'
if not os.path.exists(sbfl_result):
    print(f'bug {i} : no sbfl result file for bug {i}')
    exit

methods = lizard.analyze_file(src_file)
flag = False
for method in methods.function_list:
    if not flag:
        for gt_line in gt_lines:
            if method.start_line <= int(gt_line) <= method.end_line:
                gt_method = method.unqualified_name
                gt_m_startline = method.start_line
                gt_m_endline = method.end_line
                flag = True
                break

method_id_path = f'{trees_root}/bug_{i}/instrumented_method_id.txt'
if not os.path.exists(method_id_path):
    print(f'bug {i} : no instrumented_method_id.txt')
    exit
flag = False
line_mapping_all_methods = collections.OrderedDict()
with open(method_id_path, 'r') as f:
    for line in f:
        mid = line.split(':')[0]
        fname = line.split(':')[1].split('#')[0]
        mname = line.split(':')[1].split('#')[2]
        mstartline, mendline = 0, 0
        
        tree_file = f'{trees_root}/bug_{i}/{mid}/tree'
        if not os.path.exists(tree_file):
            print(f'bug {i} : no tree file for groundtruth method {i}')
            continue

        # mapping var to line
        var_line_mapping = var2line.Var2Line(line.strip(), tree_file, value_file)
        ori_mapping = var_line_mapping.direct_var_to_line()
        line_mapping_without_args = var_line_mapping.map_args(ori_mapping)
        var_line_mapping.print(line_mapping_without_args, f'{trees_root}/bug_{i}/{mid}/line_rank.txt')
        line_mapping_methods = var_line_mapping.aggre_methods(line_mapping_without_args, sbfl_result, fname, mname)
        for k,v in line_mapping_methods.items():
            line_mapping_all_methods[k] = v
        
        if fname.endswith(gt_file) and mname == f'{gt_method}&{gt_m_startline}&{gt_m_endline}':
            flag = True
            # rank in method
            counter, gt_rank = 0, 999999999
            binary_exp_counter = 0
            num = 0
            for lineNo,v in line_mapping_without_args.items():
                counter += 1
                if lineNo in gt_lines and counter < gt_rank:
                    #gt_rank = counter
                    start = 0
                    for k2,v2 in line_mapping_without_args.items():
                        start += 1
                        if v2['reorder_score'] == v['reorder_score']:
                            break
                    tnum = sum([1 for j in line_mapping_without_args.values() if j['reorder_score'] == v['reorder_score']])
                    temp = 0
                    for j in range(start, start+tnum):
                        temp += j
                    new_rank = 1.0 * temp / tnum
                    if new_rank < gt_rank:
                        gt_rank = new_rank
                        num = tnum
            if counter == 0:
                print(f'bug {i} : no tree generated for groundtruth method {mid}')
            else:
                #print(f'bug {i} : {gt_rank}/{counter} method {mid}')
                print(f'bug {i} : {gt_rank}({num}) method {mid}')

final_score = dict(sorted(line_mapping_all_methods.items(), key=lambda x: x[1]['score_with_method'], reverse=True))        
with open(f'{trees_root}/bug_{i}/line_rank.txt', 'w') as f:
    for lineNo,info in final_score.items():
        score = info['score_with_method']
        f.write(f'{lineNo}:{score}\n')
# rank in all methods
counter, gt_rank = 0, 999999999
num = 0
for k,v in final_score.items():
    counter += 1
    fname = k.split('#')[0]
    mname = k.split('#')[1]
    lineNo = k.split('#')[2]
    if fname.endswith(gt_file) and mname == f'{gt_method}&{gt_m_startline}&{gt_m_endline}':
        if lineNo in gt_lines:
            # calculate rank
            start = 0
            for k2,v2 in final_score.items():
                start += 1
                if v2['score_with_method'] == v['score_with_method']:
                    break
            tnum = sum([1 for j in final_score.values() if j['score_with_method'] == v['score_with_method']])
            temp = 0
            for j in range(start, start+tnum):
                temp += j
            new_rank = 1.0 * temp / tnum
            if new_rank < gt_rank:
                gt_rank = new_rank
                num = tnum
if counter == 0:
    print(f'bug {i} : not find')
else:
    #print(f'bug {i} : {gt_rank}/{counter}')
    print(f'bug {i} : {gt_rank}({num})')
if not flag:
    print(f'bug {i} : not find groundtruth method')
    