package pe.marvisur.sgldqr.arquitectura;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "pe.marvisur.sgldqr")
public class ArquitecturaHexagonalTest {

    @ArchTest
    public static final ArchRule noDependerDeSpring =
            noClasses()
                    .that().resideInAPackage("pe.marvisur.sgldqr..")
                    .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
                    .because("El núcleo de dominio en marvisur-core debe ser Java puro y desacoplado de Spring");

    @ArchTest
    public static final ArchRule noDependerDeJPA =
            noClasses()
                    .that().resideInAPackage("pe.marvisur.sgldqr..")
                    .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "javax.persistence..")
                    .because("El núcleo de dominio no debe depender de anotaciones ni APIs de JPA/Hibernate");

    @ArchTest
    public static final ArchRule noDependerDeJavaFX =
            noClasses()
                    .that().resideInAPackage("pe.marvisur.sgldqr..")
                    .should().dependOnClassesThat().resideInAnyPackage("javafx..")
                    .because("El core de dominio no debe acoplarse a la interfaz de usuario de escritorio");

    @ArchTest
    public static final ArchRule dominioNoDebeDependerDePuertosNiAplicacion =
            noClasses()
                    .that().resideInAPackage("pe.marvisur.sgldqr.dominio..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "pe.marvisur.sgldqr.aplicacion..",
                            "pe.marvisur.sgldqr.puertos.."
                    )
                    .because("El dominio es el núcleo central y no debe conocer puertos ni casos de uso de aplicación");

    @ArchTest
    public static final ArchRule puertosNoDebenDependerDeAplicacion =
            noClasses()
                    .that().resideInAPackage("pe.marvisur.sgldqr.puertos..")
                    .should().dependOnClassesThat().resideInAnyPackage("pe.marvisur.sgldqr.aplicacion..")
                    .because("Los puertos solo deben referenciar el modelo de dominio y tipos de Java");
}
