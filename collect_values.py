import os

bugs_txt='/mnt/autoRun/bugs.txt'
available_bugs=[2,3,4,5,6,7,8,9,10,12,14,16,17,20,22,23,24,25,26,28,29,30,31,35,36,37,38,39,40,41,42,44,45,46,48,49,50,51,52,53,58,59,60,61,62]

for i in available_bugs:
    with open(bugs_txt, 'r') as f:
        for l in f:
            if l.startswith(f'{i},'):
                test_suite = l.split(',')[4]
                test_runner = l.split(',')[-1].strip()
                if not os.path.exists(f'/mnt/values/{i}'):
                    os.system(f'mkdir /mnt/values/{i}')
                cmd = f'/mnt/out_put/{i}_llvm/mysql-server-source/build/bin/{test_runner} --gtest_filter={test_suite} | tee /mnt/values/{i}/values.txt'
                print(cmd)
                os.system(cmd)
print('finish')