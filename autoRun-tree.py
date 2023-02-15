import os
import traceback

# 运行完autoRun-tree.py再运行/mnt/code/VBFL_hw/collect_changed_coverage.py得到最终结果
# 运行/mnt/code/VBFL_hw/ochiai.py得到ochiai.txt,需要有/mnt/out_put/i/文件夹（非i_llvm

avail = [i for i in range(111, 195)]
for i in avail:
    try:
        get_test_cmd = f'python3 find_all_covered_tests.py {i}'
        test_cmds = os.popen(get_test_cmd).read()
        for test_cmd in test_cmds.split('\n'):
            if not test_cmd.startswith('timeout '):
                continue
            # run tests and collect values
            print(test_cmd)
            os.system(test_cmd)
            

            # match graph file and method
            match_cmd = f'python3 match_method.py {i}'
            match_out = os.popen(match_cmd).read()
            if f'[ERROR] no graph generated' in match_out:
                print('[WARN] bug {i} : no graph generated')
            
            # parse values and split by method
            parse_cmd = f'python3 parse_values.py {i}'
            parse_out = os.popen(parse_cmd).read()
            if f'[ERROR] no values collected' in parse_out:
                print('[WARN] bug {i} : no values collected')

            # build tree
            tree_cmd = f'java -cp "target:lib/*" fl.weka.IntraGenTree {i}'
            os.system(tree_cmd)

            # check rank
            check_cmd = f'python3 check_rank.py {i}'
            os.system(check_cmd)


    
    except BaseException as e:
        print(traceback.format_exc())
        with open("error.txt", "w") as file:
            file.write(traceback.format_exc() + "\n")
            file.write(str(i) + "\n")
        continue


