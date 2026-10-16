package com.swe307.pro2.repository;

import com.swe307.pro2.model.Emp;
import com.swe307.pro2.dto.EmpDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface EmpRepository extends JpaRepository<Emp, Integer> {

    @Query("""
        select new com.swe307.pro2.dto.EmpDto(e.empno, e.ename, e.job, m.ename, e.hiredate, e.sal, e.comm, d.dname, e.img)
        from Emp e
        left join Emp m on e.mgr = m.empno
        join Dept d on e.deptno = d.deptno
        order by e.empno
        """)
    List<EmpDto> findAllView();
}