package it.polito.tdp.metroparis.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.event.ConnectedComponentTraversalEvent;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListener;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.jgrapht.traverse.DepthFirstIterator;

import it.polito.tdp.metroparis.db.MetroDAO;

public class Model {

	// creo un grafico i cui vertici sono oggetti di tipo Fermata
	
	Graph<Fermata, DefaultEdge> grafo;
	private Map<Fermata, Fermata> predecessore;
	
	public void creaGrafo() {
		this.grafo = new SimpleGraph<>(DefaultEdge.class);
		
		MetroDAO dao = new MetroDAO();
		List<Fermata> fermate = dao.getAllFermate();
		
//		for(Fermata f: fermate) {
//			this.grafo.addVertex(f);
//		} // aggiunge al grafo tutte le fermate
		
		// Graphs è una classe fatta di metodi statici, che sono delle scorciatoie
		// es. contiene addAllVertices
		
		Graphs.addAllVertices(this.grafo, fermate);
		
		// ora possiamo aggiungere gli archi
		// per farlo ci chiediamo se una coppia di vertici deve essere collegata 
		
		// aggiungiamo gli archi
		
		for(Fermata f1: this.grafo.vertexSet()) {
			for(Fermata f2: this.grafo.vertexSet()) {
				// tra questa coppia ci deve essere un arco o no?
				// potrebbe dirmelo il database
				if(!f1.equals(f2) && dao.fermateCollegate(f1,f2)) { // verifico anche che le due fermate siano diverse perchè altrimenti si creerebbe un loop
					this.grafo.addEdge(f1, f2);
				}
			}
		}
		
		List<Connessione> connessioni = dao.getAllConnessioni(fermate);
		for(Connessione c: connessioni) {
			this.grafo.addEdge(c.getStazP(), c.getStazA());
		}
		System.out.println(this.grafo);
		
		// per trovare la fermata conoscendo un vertice e l'arco che la collega
		// per trovare gli archi data una fermata->
		Fermata f;
		Set<DefaultEdge> archi = this.grafo.edgeSet();
		// per trovare l'altra fermata collegata a ciascuno degli archi ->
		/*for(DefaultEdge e:archi) {
			// quando da un vertice trovo tutti gli archi adiacenti, posso interrogarli
			// e scoprire i 2 vertici che collega. sicuramente uno dei due sarà uguale al
			// vertice di partenza da cui ho ricavato gli archi, quindi tramite if posso
			// fare dei controlli
			/*Fermata f1 = this.grafo.getEdgeSource(e); 
			Fermata f2 = this.grafo.getEdgeTarget(e);
			if(f1.equals(f)) {
				// f2 è quello che mi serve
			}
			else {
				// f1 è quello che mi serve
			}
			
			// NELLA PRATICA NON SI USA TUTTO QUESTO PROCEDIAMENTO, MA SI USA
			f1 = Graphs.getOppositeVertex(this.grafo, e, f);
		}*/
		
		// OPPURE, ANCORA PIU' VELOCEMENTE ->
		//List<Fermata> fermateAdiacenti = Graphs.successorListOf(this.grafo, f);
		// DA UN VERTICE ARRIVO AI VERTICI AD ESSO ADIACENTI USO QUESTO METODO
		
		// proviamo ad implementare n JGraphT gli algoritmi di visita
		// 
		
	}
	
	public List<Fermata> fermateRaggiungibili(Fermata partenza) {
		// visita in ampiezza
		BreadthFirstIterator<Fermata, DefaultEdge> bfv = new BreadthFirstIterator<Fermata, DefaultEdge>(this.grafo, partenza);
		
		this.predecessore = new HashMap<>();
		this.predecessore.put(partenza, null);
		
		bfv.addTraversalListener(new TraversalListener<Fermata, DefaultEdge>() {

			@Override
			public void connectedComponentFinished(ConnectedComponentTraversalEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void connectedComponentStarted(ConnectedComponentTraversalEvent e) {
			}

			@Override
			public void edgeTraversed(EdgeTraversalEvent<DefaultEdge> e) {
			// viene chiamato ogni volta che si attraversa un nuovo arco
			// qui ho l'informazione sull'arco, quindi mi può dare l'informazione sulla partenza e sull'arrivo
				DefaultEdge arco = e.getEdge();
				Fermata a = grafo.getEdgeSource(arco);
				Fermata b = grafo.getEdgeTarget(arco);
				// due casi
				// 1: ho scoperto a arrivando da b e si verifica se b lo conoscevo già
				if(predecessore.containsKey(b) && !predecessore.containsKey(a)) {
					// se è coì, ossia che b era già presente nella mappa, allora 
					// a viene scoperto da b
					predecessore.put(a, b);
					System.out.println(a+" scoperto da " +b);
				}
				else if(predecessore.containsKey(a) && !predecessore.containsKey(b)) {
					// 2: di sicuro conoscevo a e quindi ho scoperto b
					predecessore.put(b, a);
					System.out.println(b+" scoperto da " +a);
				}
				
				// è più semplice utilizzare questo rispetto a VertexTraversed
			}

			@Override
			public void vertexTraversed(VertexTraversalEvent<Fermata> e) {
//				Fermata nuova = e.getVertex();
//				Fermata precedente = // vertice adiacente a nuova e che sia già raggiunto
//						// cioè è già presente nelle keys della mappa
//				predecessore.put(nuova, precedente);		
				
			}

			@Override
			public void vertexFinished(VertexTraversalEvent<Fermata> e) {
			}
			
		});
		//DepthFirstIterator<Fermata, DefaultEdge> dfv = new DepthFirstIterator<Fermata, DefaultEdge>(this.grafo, partenza);
		// ho creato l'iteratore che ha potenzialmente tutti i vertici pronti ma me li deve dare uno per volta
		List<Fermata> result = new ArrayList<>();
		
		while(bfv.hasNext()) { // finchè ho dei vertici che posso aggiungere
			Fermata f = bfv.next();
			result.add(f);
		}
		return result;
	}
	
	public Fermata trovaFermata(String nome) {
		for(Fermata f: this.grafo.vertexSet()) {
			if(f.getNome().equals(nome)) {
				return f;
			}
		}
		return null;
		// se questo metodo diventa troppo pesante, allora convieneinserire una mappa nel momento della creazione del grafo
	}
	
	public List<Fermata> trovaCammino(Fermata partenza, Fermata arrivo) {
		fermateRaggiungibili(partenza);
		List<Fermata> result = new ArrayList<>();
		result.add(arrivo);
		// poi vado all'indietro fino a quando trovo un predecessore
		
		Fermata f = arrivo;
		while(predecessore.get(f) != null) { // il predecessore di f non è null
			f= predecessore.get(f);
			result.add(f);
		}
		
		// bfv implementa un metodo detto getParent per trovare il vertice genitore, quindi tutto quello scritto in 
		// EdgeTraversed è inutile, non serve creare tuta quella mappa. Ma dfv non lo implementa
		// quindi questo metodo può essere utilizzato nel caso di dfv.
		// inoltre tutti questi metodi non possono essere utilizzati nel caso della ricerca di cammini di costo minimo.
		
		
		return result;
	}
}
