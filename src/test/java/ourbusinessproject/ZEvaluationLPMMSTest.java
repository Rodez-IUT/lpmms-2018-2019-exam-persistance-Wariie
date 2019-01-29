package ourbusinessproject;

import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManager;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class ZEvaluationLPMMSTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EnterpriseProjectService enterpriseProjectService;

    @Autowired
    private Bootstrap bootstrap;

    

    @Test
    public void testSaveDetachedEnterprise() {

        // given a detached enterprise
        Enterprise enterprise = bootstrap.getInitializationService().getEnterprise1();

        // when modifying the enterprise
        enterprise.setName("E1 modified");

        // and saving the enterprise
        Enterprise managedEnterprise = enterpriseProjectService.saveEnterprise(enterprise);

        // then the managed enterprise references a different instance of enterprise
        assertThat(managedEnterprise, not(enterprise));

        // but the managed enterprise has the id of the initial instance
        assertThat(managedEnterprise.getId(), is(enterprise.getId()));

        // and it has the new name as expected
        assertThat(managedEnterprise.getName(), is("E1 modified"));

    }
    
    @Test
    public void testSaveDetachedProject() {

        // given a detached project
        Project project = bootstrap.getInitializationService().getProject1E1();

        // when modifying the project
        project.setTitle("P1 modified");

        // and saving the project
        Project managedProject = enterpriseProjectService.saveProjectForEnterprise(project, project.getEnterprise());

        // then the managed project references a different instance of project
        assertThat(managedProject, not(project));

        // but the managed project has the id of the initial instance
        assertThat(managedProject.getId(), is(project.getId()));

        // and it has the new name as expected
        assertThat(managedProject.getTitle(), is("P1 modified"));

    }
    
    @Test
    public void testSaveOfProjectAfterEnterpriseSwitch() {

        // given an existing project and its attached enterprise
        Project project = entityManager.merge(bootstrap.getInitializationService().getProject1E1());

        // expect the project is attached to enterprise 1
        Enterprise enterprise1 = entityManager.merge(bootstrap.getInitializationService().getEnterprise1());
        assertThat(project.getEnterprise(), is(enterprise1));

        // and enterprise 1 has 2 relative projects
        assertThat(project.getEnterprise().getProjects().size(), is(2));

        // given enterprise 2 with one project
        Enterprise enterprise2 = entityManager.merge(bootstrap.getInitializationService().getEnterprise2());
        assertThat(enterprise2.getProjects().size(), is(1));

        // When we save the project switching enterprise2 with enterprise 1
        Project savedProject = enterpriseProjectService.saveProjectForEnterprise(project,enterprise2);
        assertThat(savedProject, is(project));

        // then the saved project is attached to enterprise 2
        assertThat(savedProject.getEnterprise(), is(enterprise2));
        
        // then enterprise 1 has only one project affected
        assertThat(enterprise1.getProjects().size(), is(1));

        // and enterprise does not contain saved project
        assertThat(enterprise1.getProjects(), not(hasItem(savedProject)));

        // and enterprise 2 now has 2 projects
        assertThat(enterprise2.getProjects().size(), is(2));

        // and it is attached to project
        assertThat(enterprise2.getProjects(), hasItem(savedProject));
    }
    
    @Test
    public void testProjectsAreVersionned() {

        // given an existing project
        Project project = entityManager.merge(bootstrap.getInitializationService().getProject1E1());

        // expected project is in version 1
        assertThat(project.getVersion(), is(0L));

        // when we modify the project
        project.setTitle("New title");

        // and we saveEnterprise it
        enterpriseProjectService.saveProjectForEnterprise(project,project.getEnterprise());

        // then the project is in a new version
        assertThat(project.getVersion(), is(1L));

    }

    @Test(expected = OptimisticLockException.class)
    public void testOptimisticLockingOnConcurrentProjectModification() {

        // given an existing project loaded in current persistence contexts
        Project project1 = entityManager.merge(bootstrap.getInitializationService().getProject1E1());

        // when we modify via jdbc transaction to simulate concurrent access
        jdbcTemplate.update("update project set title= ?, version = 2 where id = ?", "New title", project1.getId());

        // and we modify the project through the entity manager
        project1.setTitle("New title from em");
        entityManager.merge(project1);
        entityManager.flush();

        // an optimistic lock exception is thrown

    }
    
    /* ****
    ***** */

}