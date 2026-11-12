package co.umariana.modelo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * El árbol genealógico: un SUJETO (posición "1") con sus ASCENDIENTES (padres,
 * abuelos... en Ahnentafel)
 * y sus DESCENDIENTES (hijos, nietos... en d'Aboville). Ver la clase Posicion.
 *
 * Son en realidad dos árboles unidos en la raíz:
 *
 * ascendientes: árbol BINARIO. Cada persona tiene como mucho un padre (2n) y
 * una madre (2n + 1).
 * descendientes: árbol GENERAL. Cada persona tiene una lista de hijos (1.1,
 * 1.2, ...) ordenada por número.
 *
 * El árbol solo guarda a los miembros que EXISTEN: no hay "huecos" ni personas
 * vacías. Puede faltar el padre
 * de alguien, o el hijo 1.1 aunque exista el 1.2.
 *
 * INSTRUCCIONES PARA EL ESTUDIANTE (Etapa 2)
 * ------------------------------------------
 * 1. Implemente el árbol con sus PROPIOS NODOS: una clase interna "Nodo" con la
 * Persona, el nodo del
 * padre, el nodo de la madre y la lista de hijos. NO se permite usar Map ni Set
 * (las pruebas lo
 * revisan): para buscar una posición recorra el árbol siguiendo su camino
 * - Ahnentafel: los bits de n después del primer 1 dicen el camino (0 = padre,
 * 1 = madre).
 * Ej.: 5 = 101 -> desde el sujeto: padre (0), luego madre (1).
 * - D'Aboville: cada número de la posición dice a qué hijo bajar. Ej.: 1.2.3 ->
 * hijo 2, luego su hijo 3.
 * Se permite usar List/ArrayList para la lista de hijos.
 * 2. Todos los errores de las reglas se informan con
 * {@link ArbolInvalidoException} (un mensaje claro).
 * Los datos inválidos de una Persona los detecta Persona.validar()
 * (IllegalArgumentException).
 * 3. NO modifique las pruebas (src/test).
 *
 * REGLAS DEL ÁRBOL
 * - La posición "1" (el sujeto) existe siempre y no se puede eliminar.
 * - Una posición no puede repetirse.
 * - Un ascendiente n solo se puede agregar si ya existe su hijo n / 2, y debe
 * haber NACIDO ANTES
 * (año de nacimiento menor) que ese hijo.
 * - Un descendiente solo se puede agregar si ya existe su progenitor, y debe
 * haber NACIDO DESPUÉS
 * (año de nacimiento mayor) que su progenitor.
 */
public class ArbolGenealogico {

    private static class Nodo {
        Persona persona;
        Nodo padre; // 2n (Ahnentafel)
        Nodo madre; // 2n + 1 (Ahnentafel)
        List<Nodo> hijos = new ArrayList<>(); // d'Aboville

        Nodo(Persona persona) {
            this.persona = persona;
        }
    }

    private final Nodo raiz;

    public ArbolGenealogico(Persona sujeto) {
        if (sujeto == null) {
            throw new IllegalArgumentException("El sujeto no puede ser nulo.");
        }
        sujeto.validar();
        if (!Posicion.esSujeto(sujeto.getPosicion())) {
            throw new ArbolInvalidoException("La posición del sujeto debe ser '1'.");
        }
        this.raiz = new Nodo(sujeto);
    }

    public Persona sujeto() {
        return raiz.persona;
    }

    public int tamano() {
        return contarNodos(raiz);
    }

    private int contarNodos(Nodo nodo) {
        if (nodo == null)
            return 0;
        int total = 1;
        total += contarNodos(nodo.padre);
        total += contarNodos(nodo.madre);
        for (Nodo h : nodo.hijos) {
            total += contarNodos(h);
        }
        return total;
    }

    private Nodo buscarNodo(String posicion) {
        if (!Posicion.esValida(posicion) || raiz == null) {
            return null;
        }
        if (Posicion.esSujeto(posicion)) {
            return raiz;
        }
        if (Posicion.esAhnentafel(posicion)) {
            int n = Integer.parseInt(posicion);
            String binario = Integer.toBinaryString(n);
            Nodo actual = raiz;
            for (int i = 1; i < binario.length(); i++) {
                if (actual == null)
                    return null;
                char bit = binario.charAt(i);
                if (bit == '0') {
                    actual = actual.padre;
                } else {
                    actual = actual.madre;
                }
            }
            return actual;
        } else if (Posicion.esDAboville(posicion)) {
            String[] partes = posicion.split("\\.");
            Nodo actual = raiz;
            for (int i = 1; i < partes.length; i++) {
                if (actual == null)
                    return null;
                int ordenBuscado = Integer.parseInt(partes[i]);
                Nodo encontrado = null;
                for (Nodo h : actual.hijos) {
                    if (Posicion.orden(h.persona.getPosicion()) == ordenBuscado) {
                        encontrado = h;
                        break;
                    }
                }
                actual = encontrado;
            }
            return actual;
        }
        return null;
    }

    public Persona buscar(String posicion) {
        Nodo n = buscarNodo(posicion);
        return n != null ? n.persona : null;
    }

    public boolean contiene(String posicion) {
        return buscarNodo(posicion) != null;
    }

    public Persona agregar(Persona persona) {
        if (persona == null) {
            throw new IllegalArgumentException("La persona no puede ser nula.");
        }
        persona.validar();
        String pos = persona.getPosicion();

        if (contiene(pos)) {
            throw new ArbolInvalidoException("La posición " + pos + " ya existe en el árbol.");
        }

        if (Posicion.esAhnentafel(pos)) {
            if (Posicion.esSujeto(pos)) {
                throw new ArbolInvalidoException("El sujeto (posición 1) ya existe.");
            }
            String posHijo = Posicion.posicionDelHijoEnLinea(pos);
            Nodo nodoHijo = buscarNodo(posHijo);
            if (nodoHijo == null) {
                throw new ArbolInvalidoException("Falta la persona de la que depende (hijo en línea " + posHijo + ").");
            }
            if (persona.getNacimiento() >= nodoHijo.persona.getNacimiento()) {
                throw new ArbolInvalidoException("El ascendiente debe haber nacido antes que su hijo.");
            }
            Nodo nuevo = new Nodo(persona);
            int n = Integer.parseInt(pos);
            if (n % 2 == 0) {
                nodoHijo.padre = nuevo;
            } else {
                nodoHijo.madre = nuevo;
            }
        } else if (Posicion.esDAboville(pos)) {
            String posProgenitor = Posicion.posicionDelProgenitor(pos);
            Nodo nodoProg = buscarNodo(posProgenitor);
            if (nodoProg == null) {
                throw new ArbolInvalidoException(
                        "Falta el progenitor (" + posProgenitor + ") para agregar a " + pos + ".");
            }
            if (persona.getNacimiento() <= nodoProg.persona.getNacimiento()) {
                throw new ArbolInvalidoException("El descendiente debe haber nacido después que su progenitor.");
            }
            Nodo nuevo = new Nodo(persona);
            nodoProg.hijos.add(nuevo);
            nodoProg.hijos.sort(Comparator.comparingInt(h -> Posicion.orden(h.persona.getPosicion())));
        } else {
            throw new ArbolInvalidoException("Posición no válida: " + pos);
        }
        return persona;
    }

    public Persona agregarPadre(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        if (!Posicion.esAhnentafel(posicion)) {
            throw new ArbolInvalidoException("Solo se pueden agregar padres a posiciones Ahnentafel.");
        }
        Nodo n = buscarNodo(posicion);
        if (n == null) {
            throw new ArbolInvalidoException("La posición " + posicion + " no existe.");
        }
        if (n.padre != null) {
            throw new ArbolInvalidoException("La persona en " + posicion + " ya tiene padre.");
        }
        String posPadre = Posicion.posicionDelPadre(posicion);
        if (!Posicion.esAhnentafel(posPadre)) {
            throw new ArbolInvalidoException("La posición del padre supera el límite permitido.");
        }
        Persona p = new Persona(posPadre, nombre, nacimiento, fallecimiento);
        return agregar(p);
    }

    public Persona agregarMadre(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        if (!Posicion.esAhnentafel(posicion)) {
            throw new ArbolInvalidoException("Solo se pueden agregar madres a posiciones Ahnentafel.");
        }
        Nodo n = buscarNodo(posicion);
        if (n == null) {
            throw new ArbolInvalidoException("La posición " + posicion + " no existe.");
        }
        if (n.madre != null) {
            throw new ArbolInvalidoException("La persona en " + posicion + " ya tiene madre.");
        }
        String posMadre = Posicion.posicionDeLaMadre(posicion);
        if (!Posicion.esAhnentafel(posMadre)) {
            throw new ArbolInvalidoException("La posición de la madre supera el límite permitido.");
        }
        Persona p = new Persona(posMadre, nombre, nacimiento, fallecimiento);
        return agregar(p);
    }

    public Persona agregarHijo(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        Nodo n = buscarNodo(posicion);
        if (n == null) {
            throw new ArbolInvalidoException("La posición " + posicion + " no existe.");
        }
        if (!Posicion.esSujeto(posicion) && !Posicion.esDescendiente(posicion)) {
            throw new ArbolInvalidoException("Solo se pueden agregar hijos al sujeto o a descendientes.");
        }
        int maxOrden = 0;
        for (Nodo h : n.hijos) {
            int ord = Posicion.orden(h.persona.getPosicion());
            if (ord > maxOrden) {
                maxOrden = ord;
            }
        }
        String nuevaPos = posicion + "." + (maxOrden + 1);
        Persona p = new Persona(nuevaPos, nombre, nacimiento, fallecimiento);
        return agregar(p);
    }

    public int eliminar(String posicion) {
        if (Posicion.esSujeto(posicion)) {
            throw new ArbolInvalidoException("El sujeto no se puede eliminar.");
        }
        Nodo n = buscarNodo(posicion);
        if (n == null) {
            throw new ArbolInvalidoException("La posición " + posicion + " no existe.");
        }
        int cantidad = contarNodos(n);

        if (Posicion.esAhnentafel(posicion)) {
            String posHijo = Posicion.posicionDelHijoEnLinea(posicion);
            Nodo nodoHijo = buscarNodo(posHijo);
            if (nodoHijo != null) {
                if (nodoHijo.padre == n)
                    nodoHijo.padre = null;
                else if (nodoHijo.madre == n)
                    nodoHijo.madre = null;
            }
        } else if (Posicion.esDAboville(posicion)) {
            String posProg = Posicion.posicionDelProgenitor(posicion);
            Nodo nodoProg = buscarNodo(posProg);
            if (nodoProg != null) {
                nodoProg.hijos.remove(n);
            }
        }
        return cantidad;
    }

    public List<Persona> miembros() {
        List<Persona> lista = new ArrayList<>();
        lista.add(raiz.persona);
        lista.addAll(ascendientes());
        lista.addAll(descendientes());
        return lista;
    }

    public List<Persona> ascendientes() {
        List<Persona> lista = new ArrayList<>();
        colectarAscendientes(raiz, lista);
        lista.sort(Comparator.comparingInt(p -> Integer.parseInt(p.getPosicion())));
        return lista;
    }

    private void colectarAscendientes(Nodo nodo, List<Persona> lista) {
        if (nodo == null)
            return;
        if (nodo.padre != null) {
            lista.add(nodo.padre.persona);
            colectarAscendientes(nodo.padre, lista);
        }
        if (nodo.madre != null) {
            lista.add(nodo.madre.persona);
            colectarAscendientes(nodo.madre, lista);
        }
    }

    public List<Persona> descendientes() {
        List<Persona> lista = new ArrayList<>();
        colectarDescendientesPreorden(raiz, lista);
        return lista;
    }

    private void colectarDescendientesPreorden(Nodo nodo, List<Persona> lista) {
        if (nodo == null)
            return;
        for (Nodo h : nodo.hijos) {
            lista.add(h.persona);
            colectarDescendientesPreorden(h, lista);
        }
    }
}