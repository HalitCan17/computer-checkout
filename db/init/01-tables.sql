USE swe307pro2;
 
CREATE TABLE dept (
    deptno INT PRIMARY KEY ,
    dname VARCHAR(30) NOT NULL,
    loc VARCHAR(30) NOT NULL
);

CREATE TABLE emp(
    empno INT PRIMARY KEY,
    ename VARCHAR(30) NOT NULL,
    job VARCHAR(30),
    mgr INT NULL,
    hiredate DATE,
    sal DECIMAL(8,2),
    comm DECIMAL(8,2) NULL,
    deptno INT,
    img VARCHAR(50),
    
    FOREIGN KEY (deptno) REFERENCES dept(deptno)
);