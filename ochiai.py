import math
import os
import GetSliceCriterion
import collections
import sys

# configuration settings
FAIL_GCOV_PATH = '/build/gcov'
PASS_GCOV_PATH = []
build_dest_dir = "/mnt/out_put"  # all project will be placed here
bugs_txt_dir = "/mnt/autoRun/bugs.txt"  # the path of the 20 manual bugs, groundtruth        
# output path
SCORE_BASE_ROOT = f'/mnt/values/'


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


def find_file(ipath):
    if not os.path.exists(ipath):
        return ''
    with open(ipath, 'r', errors='ignore') as f:
        for l in f:
            if '0:Source:' in l:
                return l.strip().split(':')[-1]


def record_cov_info(project_path, gcov_root_path):
    gcov_map = {}
    find_all_gcov_cmd = f'find {gcov_root_path} -name \'*.gcov\''
    gcov_file_list = os.popen(find_all_gcov_cmd).readlines()
    for cov in gcov_file_list:
        if gcov_root_path.endswith('/'):
            gcov_root_path = gcov_root_path[:-1]
        cov = cov.strip()
        cov_relative = cov.replace(f'{gcov_root_path}', '')
        cov_relative = cov_relative.replace('CMakeFiles/', '')
        cov_relative = cov_relative.replace('.dir', '')
        is_in_proj_folder = False
        src_file = ''
        cov_by_line = {}
        with open(cov, 'r', errors='ignore') as f:
            for line in f:
                line = line.strip()
                if len(line.split(':'))>=4 and line.split(':')[-2] == 'Source' and project_path in line.split(':')[-1]:
                    is_in_proj_folder = True
                    gcov_src_file = line.split(':')[-1]
                    src_file = gcov_src_file[gcov_src_file.find(project_path)+len(project_path)+1:]
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
        if is_in_proj_folder:
            if src_file in gcov_map.keys():
                for key, value in cov_by_line.items():
                    if key in gcov_map[src_file].keys():
                        gcov_map[src_file][key] += value
                        #gcov_map[src_file][key] = value
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
    pass_test_names = []
    with open(all_test_path, 'r', errors='ignore') as f:
        for line in f:
            if line.startswith('[  PASSED  ]'):
                num_pass_test = int(line.split(' ')[-2])
            elif line.startswith('[  FAILED  ]'):
                num_fail_test += 1
            elif line.startswith('[       OK ]'):
                pass_test_names.append(line.replace('[       OK ] ','').split()[0])
    num_fail_test = (num_fail_test - 1 ) / 2
    print(f'-------------- calculate test size : {num_fail_test} failing, {num_pass_test} passing --------------')
    return num_pass_test, num_fail_test, pass_test_names


def cal_rank(ochiai_score_by_line, bug_src, bug_line_no):
    rank = 9999999999
    for loc,score in ochiai_score_by_line.items():
        for line in bug_line_no.split(':'):
            if loc.endswith(f'{bug_src}#{line}'):
                num = sum([1 for i in ochiai_score_by_line.values() if i == score])
                start = 0
                for loc2,score2 in ochiai_score_by_line.items():
                    start += 1
                    if score2 == score:
                        break
                temp = 0
                for i in range(start, start+num):
                    temp += i
                rank = 1.0 * temp / num
                return rank


def run(project_path, bugid):
    # run all tests
    os.chdir("/mnt/autoRun/")
    test_all_cmd = f"python3 build.py test {bugid} 0 0"
    print(test_all_cmd)
    os.system(test_all_cmd)
    # all-test result
    all_test_path = os.popen(f'find {project_path} -name test_result.txt').read().strip()
    # compute test size
    num_pass_test, num_fail_test, pass_test_names = compute_test_size(all_test_path)

    GetSliceCriterion.read_bugs_from_txt(bugs_txt_dir)
    GetSliceCriterion.clean_all_gcov(f"{build_dest_dir}/{i}/mysql-server-source/")
    # running fail gcov
    os.chdir("/mnt/autoRun/")
    test_fail_cmd = f"python3 build.py test {i} 0 1"
    print(test_fail_cmd)
    os.system(test_fail_cmd)
    # get gcov files and copy them to the copy them to the {i}/mysql-server-source/gcov dir
    GetSliceCriterion.get_gcov(f"{i}", f"{build_dest_dir}/{i}/mysql-server-source/", 0)
    FAIL_GCOV_PATH = f'{build_dest_dir}/{i}/mysql-server-source/gcov'
    # read .gcov files to record coverage info
    fail_gcov_root = f'{FAIL_GCOV_PATH}'
    fail_gcov_map = record_cov_info(project_path, fail_gcov_root)
    print(f'-------------- collect failing tests coverage info --------------')
    GetSliceCriterion.clean_all_gcov(f"{build_dest_dir}/{i}/mysql-server-source/")

    # running each pass gcov
    test_bin = GetSliceCriterion.bugs[i-1].test_bin
    for pt in pass_test_names:
        bug_test_filter = f"--gtest_filter={pt}"
        ptest_cmd = f"{build_dest_dir}/{i}/mysql-server-source/build/bin/{test_bin} {bug_test_filter}"
        print(ptest_cmd)
        os.system(ptest_cmd)
        # get gcov files and copy them to the copy them to the {i}/mysql-server-source/pass_gcov/pt dir
        GetSliceCriterion.get_pass_gcov(f"{i}", f"{build_dest_dir}/{i}/mysql-server-source/", pt)
        PASS_GCOV_PATH.append(f'{build_dest_dir}/{i}/mysql-server-source/pass_gcov/{pt}')
        GetSliceCriterion.clean_all_gcov(f"{build_dest_dir}/{i}/mysql-server-source/")
    # read .gcov files to record coverage info
    pass_gcov_map = {}
    for pgcov in PASS_GCOV_PATH:
        pass_gcov_root = f'{pgcov}'
        temp = record_cov_info(project_path, pass_gcov_root)
        pass_gcov_map = merge_dict(temp, pass_gcov_map)
    print(f'-------------- collect passing tests coverage info --------------')
    
    # calculate ochiai score
    ochiai_score_by_line = {}
    for fname, lineCount in fail_gcov_map.items():
        for line, count in lineCount.items():
            lineID = f'{fname}#{line}'
            num_fail_exec = count
            num_pass_exec = 0
            if fname in pass_gcov_map.keys():
                if line in pass_gcov_map[fname].keys():
                    num_pass_exec = pass_gcov_map[fname][line]
            ochiai_score_by_line[lineID] = cal_ochiai(num_fail_exec, num_pass_exec, num_fail_test)
    ochiai_score_by_line = dict(sorted(ochiai_score_by_line.items(), key=lambda x: x[1], reverse=True))
    groundtruth_rank = cal_rank(ochiai_score_by_line, GetSliceCriterion.bugs[i-1].bug_src, GetSliceCriterion.bugs[i-1].bug_line_no)
    output_file(f'{SCORE_BASE_ROOT}/{i}/ochiai.txt', ochiai_score_by_line)
    print(f'-------------- calculate and output ochiai score for statements --------------')
    return groundtruth_rank


#available_bugs = [2,3]
#available_bugs = [2,3,4,5,6,7,8,9,10,12,14,16,17,20,22,23,24,25,26,28,29,30,31,35,36,37,38,39,40,41,42,44,45,46,48,49,50,51,52,53,58,59,60,61,62]

#groundtruth_ranks = collections.OrderedDict()
#for i in available_bugs:
i = int(sys.argv[1])
groundtruth_ranks = run(f'{build_dest_dir}/{i}', i)
#output_file(f'{SCORE_BASE_ROOT}/ochiai_ranks.txt', groundtruth_ranks)
print('finish')