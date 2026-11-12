package co.umariana.construccion;

import co.umariana.modelo.ArbolGenealogico;
import co.umariana.modelo.ArbolInvalidoException;
import co.umariana.modelo.Persona;
import co.umariana.modelo.Posicion;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Construye un árbol genealógico a partir de un texto: cada línea es una
 * persona.
 *
 * posicion;nombre;nacimiento;fallecimiento
 *
 * Ejemplo (el fallecimiento puede quedar vacío):
 *
 * 1;John Doe;2002;
 * 2;Henry Doe;1920;1990
 * 3;Maria Doe;1921;
 * 1.1;Timmy Doe;2021;
 * 1.2;Amy Doe;2026;
 *
 * INSTRUCCIONES PARA EL ESTUDIANTE (Etapa 3)
 * ------------------------------------------
 * Reemplace el "throw new UnsupportedOperationException" por su implementación.
 * Esta clase usa lo que ya
 * hizo en las etapas anteriores (Posicion, Persona y ArbolGenealogico). NO
 * modifique las pruebas (src/test).
 *
 * REGLAS
 * 1. Si el texto es null o no tiene ninguna línea con contenido:
 * ArbolInvalidoException (mensaje: "El texto está vacío...").
 * 2. Las líneas pueden terminar en \n o en \r\n. Las líneas en blanco se
 * ignoran, pero los números de línea
 * que se informan cuentan TODAS las líneas del texto (la primera es la línea
 * 1).
 * 3. Cada línea tiene 4 campos separados por ';' (el cuarto, el fallecimiento,
 * puede estar vacío). Se acepta
 * también la línea con solo 3 campos y sin el último ';'. Los campos se
 * recortan (trim).
 * Si hay menos de 3 o más de 4 campos: error.
 * 4. El nacimiento y el fallecimiento deben ser números enteros (el
 * fallecimiento vacío es "no tiene").
 * 5. Cada persona debe ser válida (Persona.validar) y no puede haber dos líneas
 * con la misma posición.
 * 6. Debe existir la persona "1" (el sujeto), pero puede estar en cualquier
 * línea.
 * 7. Las líneas pueden venir en CUALQUIER orden. El constructor debe agregarlas
 * al árbol en un orden en el
 * que cada persona ya tenga a la que necesita (por ejemplo, primero los
 * ascendientes por número Ahnentafel
 * ascendente y los descendientes por generación). Las reglas del árbol (ver
 * ArbolGenealogico) las valida el árbol.
 * 8. Todos los errores asociados a una línea se informan con
 * ArbolInvalidoException y un mensaje que EMPIEZA con
 * "Línea N: " (N es el número de línea de la persona con el problema), por
 * ejemplo:
 * "Línea 3: la posición 2 está repetida."
 * Los errores que no son de una línea (texto vacío, falta el sujeto) no llevan
 * ese prefijo.
 * 9. Solo se muestran los miembros que están en el texto: no se crean personas
 * vacías para los que faltan.
 */
public final class ConstructorArbol {

    private ConstructorArbol() {
    }

    private static class LineaPersona {
        int numeroLinea;
        Persona persona;

        LineaPersona(int numeroLinea, Persona persona) {
            this.numeroLinea = numeroLinea;
            this.persona = persona;
        }
    }

    /**
     * Convierte el texto en un árbol (ver las reglas arriba).
     *
     * @throws ArbolInvalidoException si el texto no cumple las reglas
     */
    public static ArbolGenealogico construir(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ArbolInvalidoException("El texto está vacío.");
        }

        String[] lineas = texto.split("\r?\n", -1);
        List<LineaPersona> personasLeidas = new ArrayList<>();
        Set<String> posicionesVistas = new HashSet<>();
        boolean tieneContenido = false;

        for (int i = 0; i < lineas.length; i++) {
            int numLinea = i + 1;
            String lineaRaw = lineas[i];
            if (lineaRaw.trim().isEmpty()) {
                continue;
            }
            tieneContenido = true;

            String[] campos = lineaRaw.split(";", -1);
            if (campos.length < 3 || campos.length > 4) {
                throw new ArbolInvalidoException("Línea " + numLinea + ": formato incorrecto de campos.");
            }

            String pos = campos[0].trim();
            String nombre = campos[1].trim();
            String nacStr = campos[2].trim();
            String fallStr = (campos.length == 4) ? campos[3].trim() : "";

            int nac;
            try {
                nac = Integer.parseInt(nacStr);
            } catch (NumberFormatException e) {
                throw new ArbolInvalidoException(
                        "Línea " + numLinea + ": el año de nacimiento debe ser un número entero.");
            }

            Integer fall = null;
            if (!fallStr.isEmpty()) {
                try {
                    fall = Integer.parseInt(fallStr);
                } catch (NumberFormatException e) {
                    throw new ArbolInvalidoException(
                            "Línea " + numLinea + ": el año de fallecimiento debe ser un número entero.");
                }
            }

            Persona p = new Persona(pos, nombre, nac, fall);
            try {
                p.validar();
            } catch (Exception e) {
                throw new ArbolInvalidoException("Línea " + numLinea + ": " + e.getMessage());
            }

            if (posicionesVistas.contains(pos)) {
                throw new ArbolInvalidoException("Línea " + numLinea + ": la posición " + pos + " está repetida.");
            }
            posicionesVistas.add(pos);
            personasLeidas.add(new LineaPersona(numLinea, p));
        }

        if (!tieneContenido) {
            throw new ArbolInvalidoException("El texto está vacío.");
        }

        LineaPersona sujetoLinea = null;
        for (LineaPersona lp : personasLeidas) {
            if (Posicion.esSujeto(lp.persona.getPosicion())) {
                sujetoLinea = lp;
                break;
            }
        }

        if (sujetoLinea == null) {
            throw new ArbolInvalidoException("Falta el sujeto (persona '1').");
        }

        ArbolGenealogico arbol;
        try {
            arbol = new ArbolGenealogico(sujetoLinea.persona);
        } catch (Exception e) {
            throw new ArbolInvalidoException("Línea " + sujetoLinea.numeroLinea + ": " + e.getMessage());
        }

        List<LineaPersona> ascendientes = new ArrayList<>();
        List<LineaPersona> descendientes = new ArrayList<>();

        for (LineaPersona lp : personasLeidas) {
            String pos = lp.persona.getPosicion();
            if (Posicion.esAscendiente(pos)) {
                ascendientes.add(lp);
            } else if (Posicion.esDescendiente(pos)) {
                descendientes.add(lp);
            }
        }

        ascendientes.sort(Comparator.comparingInt(lp -> Integer.parseInt(lp.persona.getPosicion())));
        descendientes.sort((a, b) -> {
            int genA = Posicion.generacion(a.persona.getPosicion());
            int genB = Posicion.generacion(b.persona.getPosicion());
            if (genA != genB)
                return Integer.compare(genA, genB);
            return a.persona.getPosicion().compareTo(b.persona.getPosicion());
        });

        for (LineaPersona lp : ascendientes) {
            try {
                arbol.agregar(lp.persona);
            } catch (Exception e) {
                throw new ArbolInvalidoException("Línea " + lp.numeroLinea + ": " + e.getMessage());
            }
        }

        for (LineaPersona lp : descendientes) {
            try {
                arbol.agregar(lp.persona);
            } catch (Exception e) {
                throw new ArbolInvalidoException("Línea " + lp.numeroLinea + ": " + e.getMessage());
            }
        }

        return arbol;
    }
}