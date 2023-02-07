import math
import os
import lizard
import collections
import numpy as np
import keyword

testcommands='/mnt/autoRun/test_command.txt'
bugs_txt_dir = "/mnt/autoRun/bugs.txt"  # the path of the 20 manual bugs, groundtruth        

class Bug:
    def __init__(self, bug_line: str):
        line = bug_line.split(",")
        if len(line) != 7:
            print(f"Error line {bug_line}")
            return
        self.bug_id = line[0]
        self.bug_type = line[1]
        self.bug_src = line[2]
        self.bug_line_no = line[3]
        self.bug_test = line[4]
        self.bug_fail_test = line[5]
        self.test_bin = line[6].replace("\n", "")

    def __str__(self):
        return self.bug_id + ". type:" + self.bug_type + ", src:" + self.bug_src \
               + ":" + self.bug_line_no + ", test:" + self.bug_test + ", fail test:" + self.bug_fail_test


def read_bugs_from_txt(file_path):
    bugs = []
    with open(file_path, "r") as f:
        for line in f:
            bugs.append(Bug(line))
    return bugs


def cal_ochiai(num_fail_exec, num_pass_exec, num_total_fail):
    return 1.0 * num_fail_exec / math.sqrt(num_fail_exec * (num_pass_exec + num_fail_exec))


def output_file(file,content):
    with open(file, 'w') as w:
        for s in content.items():
            w.write(f'{s[0]},{s[1]}\n')
        w.close()


def merge_dict(x,y):
    z={}
    for k,v in x.items():
        if k in y.keys():
            z[k] = dict(list(y[k].items()) + list(v.items()))
            #z[k] = y[k] + v
        else:
            if len(v) != 0:
                z[k] = v
    return z


def record_cov_info(project_path, gcov_root_path):
    gcov_map = {}
    find_all_gcov_cmd = f'find {gcov_root_path} -name *.gcov'
    gcov_file_list = os.popen(find_all_gcov_cmd).readlines()
    for cov in gcov_file_list:
        if gcov_root_path.endswith('/'):
            gcov_root_path = gcov_root_path[:-1]
        cov = cov.strip()
        cov_relative = cov.replace(f'{gcov_root_path}', '')
        cov_relative = cov_relative.replace('CMakeFiles/', '')
        cov_relative = cov_relative.replace('.dir', '')
        is_in_proj_folder, is_in_src_path = False, True
        src_file = ''
        cov_by_line = {}
        with open(cov, 'r', errors='ignore') as f:
            for line in f:
                line = line.strip()
                if len(line.split(':'))>=4 and line.split(':')[-2] == 'Source' and project_path in line.split(':')[-1]:
                    is_in_proj_folder = True
                    gcov_src_file = line.split(':')[-1]
                    src_file = gcov_src_file[gcov_src_file.find(project_path)+len(project_path)+2:]
                    src_relative = src_file.split('/')
                    for re in cov_relative.split('/'):
                        if re.replace('.gcov', '') not in src_relative:
                            is_in_src_path = False
                            break
                    if not is_in_src_path:
                        break
                elif len(line.split(':'))>=4 and line.split(':')[-2] == 'Source' and not line.split(':')[-1].startswith(project_path):
                    break
                else:
                    if len(line.split(':')) >= 3:
                        exec_count = line.split(':')[0].strip()
                        lineno = int(line.split(':')[1].strip())
                        if exec_count == '-':
                            continue
                        elif exec_count == '#####':
                            continue
                        elif exec_count.isnumeric():
                            count = int(exec_count)
                            cov_by_line[lineno] = 1
                        elif '*' in exec_count and exec_count[:exec_count.index('*')].isnumeric():
                            count = int(exec_count[:exec_count.index('*')])
                            cov_by_line[lineno] = 1
        if is_in_proj_folder and is_in_src_path:
            if src_file in gcov_map.keys():
                for key, value in cov_by_line.items():
                    if key in gcov_map[src_file].keys():
                        #gcov_map[src_file][key] += value
                        gcov_map[src_file][key] = value
                    else:
                        gcov_map[src_file][key] = value
            else:
                gcov_map[src_file] = cov_by_line
            print(f'Update gcov info for {src_file} : {len(cov_by_line)} items')
    return gcov_map


def compute_test_size(all_test_path):
    if not os.path.exists(all_test_path):
        print(f'all_test.txt NOT EXIST : {all_test_path}')
        exit(1)
    num_pass_test, num_fail_test = 0, 0
    with open(all_test_path, 'r', errors='ignore') as f:
        for line in f:
            if '[ pass ]' in line:
                num_pass_test += 1
            elif '[ fail ]' in line:
                num_fail_test += 1
    print(f'-------------- calculate test size : {num_fail_test} failing, {num_pass_test} passing --------------')
    return num_pass_test, num_fail_test


def run_single_bug(id, project_dir, src_file):
    testcmds = []
    get_test_cmd = f'python3 find_all_covered_tests.py {id}'
    test_cmds = os.popen(get_test_cmd).read()
    for test_cmd in test_cmds.split('\n'):
        if test_cmd.startswith('timeout '):
            testcmds.append(test_cmd.split('|')[0].strip())
    for tcmd in testcmds:
        test_name = tcmd.split('=')[-1]
        # clean all .gcda and .gcov
        os.system(f"find {project_dir}/build -name '*.gcda' | xargs rm -rf")
        os.system(f"find {project_dir}/build -name '*.gcov' | xargs rm -rf")
        os.system('rm -f /mnt/autoRun/test_result.txt')
        # run a single test
        os.system(f'{tcmd} | tee /mnt/autoRun/test_result.txt')
        print(tcmd)
        # find all .gcda
        cmd_find_gcda = f"find {project_dir}/build -name '*{src_file}.gcda'"
        all_gcdas = os.popen(cmd_find_gcda).read().split('\n')
        print(cmd_find_gcda)
        # get .gcov
        counter = 0
        for gcda in all_gcdas:
            os.chdir(f"{project_dir}/build")
            counter += 1
            if gcda == '':
                continue
            cmd_gcov = f'llvm-cov-12 gcov {gcda}'
            gcov_result = os.popen(cmd_gcov).read()
            src_file_name, exe_percent, gcov_name = '', '', ''
            flag = False
            gcov_results = gcov_result.split('\n')
            for l in gcov_results:
                if l.startswith('File '):
                    src_file_name = l.strip().split()[-1].replace('\'','')
                elif l.startswith('Lines '):
                    exe_percent = l[l.find(':')+1:l.find('%')]
                elif l.startswith('Creating '):
                    gcov_name = l.strip().split()[-1].replace('\'','')
                elif l == '':
                    if gcov_name == f'{src_file}.gcov' and exe_percent != '0.00':
                        flag = True
                        break
                    src_file_name, exe_percent, gcov_name = '', '', ''
            if flag:
                # replace / in test name for path -> |
                test_name_for_path = test_name.replace('/','|')
                output_path = f'/mnt/AllTestGcov_BugVersion/{id}/{test_name_for_path}/'
                if not os.path.exists(output_path):
                    os.makedirs(output_path)
                cmd_cp_gcov = f'cp {project_dir}/build/{src_file}.gcov \'{output_path}\''
                os.system(cmd_cp_gcov)
                cmd_cp_result = f'cp /mnt/autoRun/test_result.txt \'{output_path}\''
                os.system(cmd_cp_result)


def collect_coverage(tar):
    ignore_bugs = [11,15,18,27,47,54]
    bugs = read_bugs_from_txt(bugs_txt_dir)
    for bug in bugs:
        id = bug.bug_id
        if int(id) not in tar:
            continue
        project_dir = f'/mnt/out_put/{id}_llvm/mysql-server-source'
        buggy_file = bug.bug_src.split('/')[-1]
        run_single_bug(id, project_dir, buggy_file)
        print(f'{id} : ok')


def parse_test_result(path):
    # 1 for pass, 0 for fail
    with open(path, 'r') as f:
        for l in f:
            if '[  PASSED  ] 1 test.' in l:
                return 1
            if '[  FAILED  ]' in l:
                return 0
    return 0


def Z_Score(arr):
    x_mean = sum(arr) / len(arr) # 平均值
    x_std = np.std(arr)   # 标准差
    arr_ = []
    for x in arr:
        score = 0
        if x_std != 0:
            score = (x-x_mean)/x_std
        arr_.append(score)
    return arr_


def Min_Max(arr):
    arr_ = []
    x_max = max(arr)  # 最大值
    x_min = min(arr)  # 最小值
    distance = x_max - x_min
    for x in arr:
        score = 1
        if distance != 0:
            score = (x-x_min)/distance
        arr_.append(score)
    return arr_


def norm_in_method(ori, src_path):
    methods = lizard.analyze_file(src_path)
    order_by_methods = collections.OrderedDict()
    for lineNo,score in ori.items():
        for method in methods.function_list:
            if method.start_line == method.end_line:
                continue
            if method.start_line <= int(lineNo) <= method.end_line:
                k = f'{method.start_line}_{method.end_line}'
                if k not in order_by_methods.keys():
                    order_by_methods[k] = collections.OrderedDict()
                order_by_methods[k][lineNo] = int(score)
                break
    final_dict = dict()
    for m,lines in order_by_methods.items():
        orilist = order_by_methods[m].values()
        #newlist = Z_Score(list(orilist))
        newlist = Min_Max(list(orilist))
        index = 0
        for line,score in lines.items():
            final_dict[line] = newlist[index]
            index += 1
    return final_dict


def load_decision_tree_score(score_path):
    treeScore = collections.OrderedDict()
    if os.path.exists(score_path):
        with open(score_path, 'r') as f:
            for l in f:
                # /mnt/out_put/2_llvm/mysql-server-source/sql/range_optimizer/tree.cc#key_or&1054&1648#1423:4.16227766016838
                cut = l.strip().split('#')[-1]
                lineNo = cut.split(':')[0]
                score = float(cut.split(':')[-1])
                treeScore[lineNo] = score
    if len(treeScore) >0:
        newlist = Min_Max(list(treeScore.values()))
        index = 0
        for line,score in treeScore.items():
            treeScore[line] = newlist[index]
            index += 1
    return treeScore


def split_for_var(vlist:list):
    result = set()
    for v in vlist:  # no blank space in v
        flag = False
        for r in [',', '++', '--', '+','-','*','/','=','+=','-=','*=','/=','==','!=','&&','||','&','|','(',')']:
            if r in v:
                flag = True
                result.update(split_for_var(v.split(r)))
                break
        if not flag:
            result.add(v)
    return result


def check_changed_var_in(obug):
    tlines = obug.bug_line_no.split(':')
    src_name = obug.bug_src.split('/')[-1]
    src_path = f'/mnt/autoRun/mysql_bug/{obug.bug_id}/{src_name}'
    tvars = set()
    for tlineNo in tlines:
        tline = os.popen(f"sed -n {int(tlineNo)}p \'{src_path}\'").read()
        if tline != "":
            words = split_for_var(tline.split())
            for word in words:
                if word.isidentifier() and not keyword.iskeyword(word) and word not in ['nullptr']:
                    tvars.add(word)
    appearScore = collections.OrderedDict()
    methods = lizard.analyze_file(src_path)
    for tlineNo in tlines:
        for method in methods.function_list:
            if method.start_line == method.end_line:
                continue
            if method.start_line <= int(tlineNo) <= method.end_line:
                for i in range(method.start_line, method.end_line+1):
                    cline = os.popen(f"sed -n {int(i)}p \'{src_path}\'").read()
                    for tvar in tvars:
                        if tvar in split_for_var(cline.split()):
                            appearScore[str(i)] = 1
                            break
    return appearScore


def sort_by_tree(bugids):
    bugs = read_bugs_from_txt(bugs_txt_dir)
    totalsize = len(bugids)
    top1, top5 = 0, 0
    for id in bugids:
        # check changed variable apperance for each statement
        varAppear = check_changed_var_in(bugs[int(id)-1])
        # load decision tree score
        treeScore = load_decision_tree_score(f'/mnt/values/trees/bug_{id}/line_rank.txt')
        root = f'/mnt/AllTestGcov_BugVersion/{id}'
        bugcovByLine = dict() 
        for t in os.listdir(root):
            tname = t.replace('|','/')
            tresult = parse_test_result(f'{root}/{t}/test_result.txt')  # 1 for pass, 0 for fail
            bug_src = bugs[int(id)-1].bug_src.split('/')[-1]
            buggy_gcov = f'{root}/{t}/{bug_src}.gcov'
            prior_gcov = f'/mnt/AllTestGcov/{t}/{bug_src}.gcov'
            precovByLine = dict()
            # parsing correct version
            with open(prior_gcov, 'r') as f:
                for l in f:
                    lineNo = int(l.split(':')[1].strip())
                    if lineNo > 0:
                        precov = l.split(':')[0].strip()
                        if precov.isdigit():
                            precov = int(precov)
                        else:
                            precov = 0
                        precovByLine[lineNo] = precov
            # parsing buggy version        
            with open(buggy_gcov, 'r') as f:
                for l in f:
                    lineNo = int(l.split(':')[1].strip())
                    if lineNo > 0:
                        bugcov = l.split(':')[0].strip()
                        if bugcov.isdigit():
                            bugcov = int(bugcov)
                        else:
                            bugcov = 0
                        diff = abs(precovByLine[lineNo] - bugcov)
                        if lineNo in bugcovByLine:
                            bugcovByLine[lineNo].append(diff)
                        else:
                            bugcovByLine[lineNo] = [diff]
        totalByLine = {lineNo:sum([j for j in cov]) for lineNo,cov in bugcovByLine.items()}
        # normalization inside method
        totalByLine = norm_in_method(totalByLine, f'/mnt/autoRun/mysql_bug/{id}/{bug_src}')
        totalByLine_cov = dict(sorted(totalByLine.items(), key=lambda x: x[1], reverse=True))  
        # combine all three
        for lineNo,cov_score in totalByLine_cov.items():
            tree_score, appear_score = 0, 0
            if str(lineNo) in treeScore.keys():
                tree_score = treeScore[str(lineNo)]
            if str(lineNo) in varAppear.keys():
                appear_score = varAppear[str(lineNo)]
            final_score = tree_score * 0.9 + cov_score * 0.1 + appear_score
            temp = collections.OrderedDict()
            temp['final'] = final_score
            temp['tree'] = tree_score
            temp['coverage'] = cov_score
            temp['appearance'] = appear_score
            totalByLine[lineNo] = temp
        totalByLine = dict(sorted(totalByLine.items(), key=lambda x: x[1]['final'], reverse=True))
        # output
        if not os.path.exists(f'/mnt/values/trees//bug_{id}/'):
            os.makedirs(f'/mnt/values/trees//bug_{id}/')
        with open(f'/mnt/values/trees//bug_{id}/combine3-line_rank.txt', 'w') as f:
            for lineNo,info in totalByLine.items():
                f.write(f'{lineNo}:{info}\n')
        # check gt
        gt_lines = [int(i) for i in bugs[int(id)-1].bug_line_no.split(':')]
        # rank in all methods
        counter, gt_rank = 0, 999999999
        num = 0
        prelist = []
        for lineNo,times in totalByLine.items():
            counter += 1
            if lineNo in gt_lines:
                # calculate rank
                start = 0
                for k2,v2 in totalByLine.items():
                    start += 1
                    prelist.append(str(k2))
                    if v2 == times:
                        break
                tnum = sum([1 for j in totalByLine.values() if j == times])
                prelist.extend([str(i) for i,j in totalByLine.items() if j == times])
                temp = 0
                for j in range(start, start+tnum):
                    temp += j
                new_rank = 1.0 * temp / tnum
                if new_rank < gt_rank:
                    gt_rank = new_rank
                    num = tnum
        if counter == 0:
            print(f'bug {id} : not find')
        else:
            #print(f'bug {i} : {gt_rank}/{counter}')
            prestr = ','.join(prelist)
            #print(f'bug {id} : {gt_rank}({num}) [{prestr}]')
            print(f'bug {id} : {gt_rank}({num})')
            if gt_rank <= 1:
                top1 += 1
            if gt_rank <= 5:
                top5 += 1

        #print(f'{id} ok')
    print(f'top1:{top1}/{totalsize}={1.0*top1/totalsize}')
    print(f'top5:{top5}/{totalsize}={1.0*top5/totalsize}')



if __name__ == '__main__':
    avail = [i for i in range(63, 64)]
    collect_coverage(avail)
    sort_by_tree(avail)  # decision tree, changed_coverage, changed variable appearance
    print('finish')
