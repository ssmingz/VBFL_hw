import os
import sys

gcov_root=f'/mnt/AllTestGcov'
bugs_txt=f'/mnt/autoRun/bugs.txt'
large_test_txt='/mnt/build_bugs/merge_large_tests-t.txt'
small_test_txt='/mnt/build_bugs/merge_small_tests-t.txt'


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



def read_test_from_txt():
    large_test_list, small_test_list = [], []
    # large
    with open(large_test_txt, 'r') as f:
        test_suite, single_test = '', ''
        for l in f:
            if l.startswith('  '):
                # single test
                single_test = l.split()[0]
                if test_suite != '' and single_test != '':
                    large_test_list.append(f'{test_suite}.{single_test}')
                else:
                    print(f'read_test_from_txt() illegal format : {l.strip()} in {large_test_txt}')
            else:
                # test suite
                test_suite = l.split('.')[0]
    # small
    with open(small_test_txt, 'r') as f:
        test_suite, single_test = '', ''
        for l in f:
            if l.startswith('  '):
                # single test
                single_test = l.split()[0]
                if test_suite != '' and single_test != '':
                    small_test_list.append(f'{test_suite}.{single_test}')
                else:
                    print(f'read_test_from_txt() illegal format : {l.strip()} in {small_test_txt}')
            else:
                # test suite
                test_suite = l.split('.')[0]
    return large_test_list, small_test_list


def get_all_covered_tests(src_file_name, covered_lines, large_test_list, small_test_list):
    # find all .gcov
    cmd_find_gcov = f"find {gcov_root}/ -name '*{src_file_name}.gcov'"
    all_gcovs = os.popen(cmd_find_gcov).read().split('\n')
    #print(cmd_find_gcov)
    #print(f'all .gcov : {len(all_gcovs)}')
    covered_large_tests, covered_small_tests = set(), set()
    for g in all_gcovs:
        if g == '':
            continue
        # 5 default lines
        for cline in covered_lines:
            gline = os.popen(f"sed -n {5+int(cline)}p \'{g}\'").read()
            if gline != "":
                first = gline.split(':')[0].strip()
                second = gline.split(':')[1].strip()
                if first.isdigit() and int(first)>0 and second==cline:
                    test_name = g.split('/')[3].replace('|','/')
                    if test_name in large_test_list and test_name not in small_test_list:
                        covered_large_tests.add(test_name)
                    elif test_name in small_test_list and test_name not in large_test_list:
                        covered_small_tests.add(test_name)
                    #elif test_name not in small_test_list and test_name not in large_test_list:
                    #    print(f'could not find test-bin for {test_name}')
                    #else:
                    #    print(f'both in large and small test for {test_name}')
    return covered_large_tests, covered_small_tests


def read_bugs_from_txt(file_path):
    bugs = []
    with open(file_path, "r") as f:
        for line in f:
            bugs.append(Bug(line))
    return bugs


def print_command(covered_tests, test_bin):
    if len(covered_tests) == 0:
        return None
    test_filter = ':'.join(covered_tests)
    cmd = f'build/bin/merge_{test_bin}_tests-t --gtest_filter={test_filter}'
    return cmd


def run_for_autoRun(large_test_list, small_test_list, id):
    bugs = read_bugs_from_txt(bugs_txt)
    for bug in bugs:
        if bug.bug_id != str(id):
            continue
        covered_large_tests, covered_small_tests = get_all_covered_tests(bug.bug_src.split('/')[-1], bug.bug_line_no.split(':'), large_test_list, small_test_list)
        cmd1 = print_command(covered_large_tests, 'large')
        cmd2 = print_command(covered_small_tests, 'small')
        if cmd1 != None:
            print(f'timeout 15m /mnt/out_put/{bug.bug_id}_llvm/mysql-server-source/{cmd1} | tee /mnt/values/{bug.bug_id}/values-large.txt')
        if cmd2 != None:
            print(f'timeout 15m /mnt/out_put/{bug.bug_id}_llvm/mysql-server-source/{cmd2} | tee /mnt/values/{bug.bug_id}/values-small.txt')
        #if cmd1 == None and cmd2 == None:
        #    print(f'bug {bug.bug_id} : No test covered ')


if __name__ == '__main__':
    id = sys.argv[1]
    large_test_list, small_test_list = read_test_from_txt()
    run_for_autoRun(large_test_list, small_test_list, id)
    #print('finish')