import os

groundtruth='/mnt/autoRun/groundtruth_1_2_forcopy.txt'
src_root='/mnt/mengjiao/build_bugs/new_bugs/bugs_condition'
dest_root='/mnt/autoRun/mysql_bug'
counter=44
with open(groundtruth, 'r') as f:
    for line in f:
        bugid = line.split('##')[0]
        src_path = f'{src_root}/{bugid}'
        dest_path = f'{dest_root}/{counter}'
        os.system(f'mkdir {dest_path}')
        os.system(f'cp -r {src_path}/* {dest_path}/')
        counter+=1
        print(f'copy {line}')
os.system(f'find {dest_root} -name "*.txt" |xargs rm -rf')
print('finish')
