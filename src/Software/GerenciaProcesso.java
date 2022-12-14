package Software;

import Hardware.Memory;
import Hardware.Opcode;
import Hardware.Word;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class GerenciaProcesso {
    public GerenciaMemoria gerenciaMemoria;
    private Queue<PCB> listaPCBs;
    private Escalonador escalonador;

    public int processId = 0;

    public GerenciaProcesso(Memory memory) {
        this.gerenciaMemoria = new GerenciaMemoria(memory);
        this.listaPCBs = new LinkedList<>();
    }

    public PCB create(Word[] p) {
        System.out.println("Novo processo criado");
        PCB processControlBlock;

        int tamanhoAlocar = p.length;

        for(Word w : p){
            if (w.opc.equals(Opcode.LDD) || w.opc.equals(Opcode.STD))
                if (w.p > tamanhoAlocar)
                    tamanhoAlocar = w.p; //se utilizar algum LDD ou STD com posicao de memoria maior que o numero de palavras declaradas, aloca mais paginas
        }

        if (gerenciaMemoria.temEspacoParaAlocar(tamanhoAlocar)) {
            //System.out.println("Alocando espaço para: " + p.length + " palavras...");
            ArrayList<Integer> paginas = gerenciaMemoria.aloca(p);
            processControlBlock = new PCB(processId, paginas, (paginas.get(0)*gerenciaMemoria.tamFrame));
            ++processId;

            listaPCBs.add(processControlBlock);
            escalonador.setProntos(getProntos());
        } else {
            System.out.println("Sem espaço na memória para criar o processo de ID: " + processId);
            processControlBlock = null;
        }

        return processControlBlock;
    }

    public void finish(PCB processo) {
        System.out.println("Processo encerrado: " + processo.id);
        for(int page : processo.getAllocatedPages()){
            gerenciaMemoria.memory.dump(page*16, (page+1)*16);
        }
        gerenciaMemoria.desaloca(processo.getAllocatedPages());
        listaPCBs.remove(processo);
        escalonador.setProntos(getProntos());
    }

    public PCB getProcessByID(int id) {
        for (PCB pcb : listaPCBs){
            if (pcb.getId() == id){
                return pcb;
            }
        }
        return null;
    }

    public void listAllProcesses(){
        System.out.println("Processos: ");
        for(PCB pcb : listaPCBs){
            System.out.println(pcb.toString());
        }
    }

    public LinkedList<PCB> getProntos(){
        LinkedList<PCB> prontos = new LinkedList<>();
        for(PCB pcb : listaPCBs){
            if (pcb.status == ProcessStatus.READY) prontos.add(pcb);
        }
        return prontos;
    }

    public LinkedList<PCB> getBloqueados(){
        LinkedList<PCB> bloqueados = new LinkedList<>();
        for(PCB pcb : listaPCBs){
            if (pcb.status == ProcessStatus.BLOCKED) bloqueados.add(pcb);
        }
        return bloqueados;
    }

    public int getProcessLineFromMemory(int line, int processId){
        int pageId = getProcessByID(processId).getAllocatedPages().get(line/gerenciaMemoria.tamFrame);
        return pageId*gerenciaMemoria.tamFrame+(line% gerenciaMemoria.tamFrame);
    }

    public boolean hasProcess(int pid){
        for(PCB pcb : listaPCBs)
            if (pcb.id == pid) return true;
        return false;
    }

    public int adicionaPaginaEmProcesso(int pid){
        if (getProcessByID(pid) != null){
            int newPageId = gerenciaMemoria.adicionaPage();
            if (newPageId >= 0){
                getProcessByID(pid).adicionaNovaPagina(newPageId);
                return newPageId;
            }
        }
        return -1;
    }

    public void adicionaExistingPaginaEmProcesso(int pid, int page){
        if (getProcessByID(pid) != null){
            getProcessByID(pid).adicionaNovaPagina(page);
        }
    }

    public void setEscalonador(Escalonador escalonador) {
        this.escalonador = escalonador;
    }
}
