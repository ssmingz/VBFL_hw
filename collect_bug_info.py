groundtruth='./groundtruth_12_28.txt'
test_commands='./build_bugs/test_commands.txt'
counter=44
buffer=''
buffer2=''
with open(groundtruth,'r') as f:
#    21##
#    条件取反##
#    sql/table_cache.cc:191##
#    TableCacheBasicDeathTest.*##
#    /*m_table_cache[i].init()*/!m_table_cache[i].init()##
#    TableCacheBasicDeathTest.ManagerCreateAndDestroy,多个是:
    for line in f:
        info_list = line.strip().split('##')
        if len(info_list) != 6:
            print(f'invalid format for line : {line.strip()}')
            continue
        bug_type = ''
        if info_list[1] == '条件取反':
            bug_type = 'change condition'
        elif info_list[1] == '条件逻辑错误':
            bug_type = 'change condition'
        elif info_list[1] == '条件符号错误':
            bug_type = 'change condition'
        elif info_list[1] == '条件添加':
            bug_type = 'change condition'
        elif info_list[1] == '条件删除':
            bug_type = 'delete condition'
        elif info_list[1] == '条件逻辑符号错误':
            bug_type = 'change condition'
        elif info_list[1] == '条件常量错误':
            bug_type = 'change condition'
        else:
            print(f'invalid format for bug type : {info_list[1]}')
            continue
        buggy_file, buggy_line = info_list[2].split(':')[0], info_list[2].split(':')[1].replace(',',':')
        test_suite = info_list[3]
        action = info_list[4]
        fail_tests = info_list[5]
        test_command = ''
        with open(test_commands, 'r') as f2:
            for l2 in f2:
                if f'--gtest_filter={test_suite}' in l2:
                    if 'merge_large_tests-t' in l2:
                        test_command = 'merge_large_tests-t'
                    elif 'merge_small_tests-t' in l2:
                        test_command = 'merge_small_tests-t'
                    break
        #22,change assign,sql/regexp/regexp_facade.cc,188,RegexpFacadeTest.*,RegexpFacadeTest.Find,merge_large_tests-t
        new_info_list = [str(counter), bug_type, buggy_file, buggy_line, test_suite, fail_tests, test_command]
        new_info_str = ','.join(new_info_list)
        print(new_info_str)
        new_info_list2 = [str(counter),info_list[1],info_list[2],info_list[3],info_list[4],info_list[5]]
        new_info_str2 = '\t'.join(new_info_list2)
        buffer+=f'{new_info_str2}\n'
        #/mnt/out_put/1_llvm/mysql-server-source/build/bin/merge_large_tests-t --gtest_filter=SqlTableTest.* | tee /mnt/values/1/values.txt
        buffer2 += f'mkdir /mnt/values/{counter} && /mnt/out_put/{counter}_llvm/mysql-server-source/build/bin/{test_command} --gtest_filter={test_suite} | tee /mnt/values/{counter}/values.txt\n'
        counter+=1
print(buffer)
print(buffer2)