import os
import sys

large_test_txt='/mnt/build_bugs/merge_large_tests-t.txt'
small_test_txt='/mnt/build_bugs/merge_small_tests-t.txt'
large_test_ignore=['Microbenchmarks']
small_test_ignore=['Microbenchmarks']
project_path_large='/mnt/mysql-server-source_new'
project_path_small='/mnt/mysql-server-source_new2'
output_dir='/mnt/AllTestGcov'

test_type = sys.argv[1]  # large or small


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
                    if test_suite not in large_test_ignore:
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
                    if test_suite not in small_test_ignore:
                        small_test_list.append(f'{test_suite}.{single_test}')
                else:
                    print(f'read_test_from_txt() illegal format : {l.strip()} in {small_test_txt}')
            else:
                # test suite
                test_suite = l.split('.')[0]
    return large_test_list, small_test_list


def collect_all_gcov(test_list, project_dir, output_root, test_bin, totoal_num):
    counter = 0
    for t in test_list:
        counter += 1
        print(f'---------------------- Test : {counter}/{totoal_num} ----------------------')
        collect_single_gcov(t, project_dir, output_root, test_bin, totoal_num, counter)
        print(f'Finish collecting all .gcov file for {test_bin} {t}')


def collect_single_gcov(test_name, project_dir, output_root, test_bin, totoal_num, index):
    # clean all .gcda and .gcov
    os.system(f"find {project_dir}/build -name '*.gcda' | xargs rm -rf")
    os.system(f"find {project_dir}/build -name '*.gcov' | xargs rm -rf")
    # run a single test
    cmd_test = f'timeout 10m {project_dir}/build/bin/{test_bin} --gtest_filter={test_name}'
    os.system(cmd_test)
    print(cmd_test)
    # find all .gcda
    cmd_find_gcda = f"find {project_dir}/build -name '*.gcda'"
    all_gcdas = os.popen(cmd_find_gcda).read().split('\n')
    # get .gcov
    counter = 0
    for gcda in all_gcdas:
        os.chdir(f"{project_dir}/build")
        counter += 1
        if gcda == '':
            continue
        print(f'------------- Collect gcov : {counter}/{len(all_gcdas)}, Test:{index}/{totoal_num} -------------')
        src_file_name = gcda.split('/')[-1].replace('.gcda','')
        cmd_gcov = f'llvm-cov-12 gcov {gcda}'
        gcov_result = os.popen(cmd_gcov).read()
        src_file, exe_percent, gcov_name = '', '', ''
        flag = False
        for l in gcov_result.split('\n'):
            if l.startswith('File '):
                src_file = l.strip().split()[-1].replace('\'','')
            elif l.startswith('Lines '):
                exe_percent = l[l.find(':')+1:l.find('%')]
            elif l.startswith('Creating '):
                gcov_name = l.strip().split()[-1].replace('\'','')
            elif l == '':
                if gcov_name == f'{src_file_name}.gcov' and exe_percent != '0.00':
                    flag = True
                    break
                src_file, exe_percent, gcov_name = '', '', ''
        if flag:
            # replace / in test name for path -> |
            test_name_for_path = test_name.replace('/','|')
            output_path = f'{output_root}/{test_name_for_path}/'
            if not os.path.exists(output_path):
                os.makedirs(output_path)
            cmd_cp_gcov = f'cp {project_dir}/build/{src_file_name}.gcov {output_path}'
            os.system(cmd_cp_gcov)


if __name__ == '__main__':
    large_test_list, small_test_list = read_test_from_txt()
    totoal_num = len(large_test_list) + len(small_test_list)
    print(f'Total test size : {totoal_num}')
    if test_type == 'large':
        collect_all_gcov(large_test_list, project_path_large, output_dir, 'merge_large_tests-t', len(large_test_list))
    elif test_type == 'small':
        collect_all_gcov(small_test_list, project_path_small, output_dir, 'merge_small_tests-t', len(small_test_list))
    else:
        print('invalid input')
    print('finish')

