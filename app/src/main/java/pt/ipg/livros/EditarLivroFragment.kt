package pt.ipg.livros

import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.SimpleCursorAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.navigation.fragment.findNavController
import pt.ipg.livros.databinding.FragmentEditarLivroBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

private const val ID_LOADER_CATEGORIAS = 0

class EditarLivroFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {
    private var livro: Livro?= null
    private var _binding: FragmentEditarLivroBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditarLivroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val loader = LoaderManager.getInstance(this)
        loader.initLoader(ID_LOADER_CATEGORIAS, null, this)

        val activity = activity as MainActivity
        activity.fragment = this
        activity.idMenuAtual = R.menu.menu_guardar_cancelar

        val livro = EditarLivroFragmentArgs.fromBundle(requireArguments()).livro

        if (livro != null) {
            binding.editTextTitulo.setText(livro.titulo)
            binding.editTextIsbn.setText(livro.isbn)
            if (livro.dataPublicacao != null) {
                binding.editTextDataPub.setText(
                    DateFormat.format("yyyy-MM-dd", livro.dataPublicacao)
                )
            }
        }

        this.livro = livro
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun processaOpcaoMenu(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_guardar -> {
                guardar()
                true
            }
            R.id.action_cancelar -> {
                voltaListaLivros()
                true
            }
            else -> false
        }
    }

    private fun voltaListaLivros() {
        findNavController().navigate(R.id.action_editarLivroFragment_to_ListaLivrosFragment)
    }

    private fun guardar() {
        val titulo = binding.editTextTitulo.text.toString()
        if (titulo.isBlank()) {
            binding.editTextTitulo.error = getString(R.string.titulo_obrigatorio)
            binding.editTextTitulo.requestFocus()
            return
        }

        val categoriaId = binding.spinnerCategorias.selectedItemId

        val data: Date?
        val df = SimpleDateFormat("dd-MM-yyyy")
        try {
            data = df.parse(binding.editTextDataPub.text.toString())
        } catch (e: Exception) {
            binding.editTextDataPub.error = getString(R.string.data_invalida)
            binding.editTextDataPub.requestFocus()
            return
        }

        val isbn = binding.editTextIsbn.text.toString()

        val calendario = Calendar.getInstance()
        calendario.time = data

        if (livro == null) {
            val livro = Livro(
                titulo,
                Categoria("?", categoriaId),
                isbn,
                calendario
            )

            insereLivro(livro)
        } else {
            val livro = livro!!
            livro.titulo = titulo
            livro.categoria = Categoria("?", categoriaId)
            livro.isbn = isbn
            livro.dataPublicacao = calendario

            alteraLivro(livro)
        }
    }

    private fun alteraLivro(livro: Livro) {
        val enderecoLivro = Uri.withAppendedPath(LivrosContentProvider.ENDERECO_LIVROS, livro.id.toString())
        val livrosAlterados = requireActivity().contentResolver.update(enderecoLivro, livro.toContentValues(), null, null)

        if (livrosAlterados == 1) {
            Toast.makeText(requireContext(), R.string.livro_guardado_com_sucesso, Toast.LENGTH_LONG).show()
            voltaListaLivros()
        } else {
            binding.editTextTitulo.error = getString(R.string.erro_guardar_livro)
        }
    }

    private fun insereLivro(
        livro: Livro
    ) {
        val id = requireActivity().contentResolver.insert(
            LivrosContentProvider.ENDERECO_LIVROS,
            livro.toContentValues()
        )

        if (id == null) {
            binding.editTextTitulo.error = getString(R.string.erro_guardar_livro)
            return
        }

        Toast.makeText(
            requireContext(),
            getString(R.string.livro_guardado_com_sucesso),
            Toast.LENGTH_SHORT
        ).show()

        voltaListaLivros()
    }

    /**
     * Instantiate and return a new Loader for the given ID.
     *
     *
     * This will always be called from the process's main thread.
     *
     * @param id The ID whose loader is to be created.
     * @param args Any arguments supplied by the caller.
     * @return Return a new Loader instance that is ready to start loading.
     */
    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        return CursorLoader(
            requireContext(),
            LivrosContentProvider.ENDERECO_CATEGORIAS,
            TabelaCategorias.CAMPOS,
            null, null,
            TabelaCategorias.CAMPO_DESCRICAO
        )
    }

    /**
     * Called when a previously created loader is being reset, and thus
     * making its data unavailable.  The application should at this point
     * remove any references it has to the Loader's data.
     *
     *
     * This will always be called from the process's main thread.
     *
     * @param loader The Loader that is being reset.
     */
    override fun onLoaderReset(loader: Loader<Cursor>) {
        if (_binding != null) {
            binding.spinnerCategorias.adapter = null
        }
    }

    /**
     * Called when a previously created loader has finished its load.  Note
     * that normally an application is *not* allowed to commit fragment
     * transactions while in this call, since it can happen after an
     * activity's state is saved.  See [ FragmentManager.openTransaction()][androidx.fragment.app.FragmentManager.beginTransaction] for further discussion on this.
     *
     *
     * This function is guaranteed to be called prior to the release of
     * the last data that was supplied for this Loader.  At this point
     * you should remove all use of the old data (since it will be released
     * soon), but should not do your own release of the data since its Loader
     * owns it and will take care of that.  The Loader will take care of
     * management of its data so you don't have to.  In particular:
     *
     *
     *  *
     *
     *The Loader will monitor for changes to the data, and report
     * them to you through new calls here.  You should not monitor the
     * data yourself.  For example, if the data is a [android.database.Cursor]
     * and you place it in a [android.widget.CursorAdapter], use
     * the [android.widget.CursorAdapter] constructor *without* passing
     * in either [android.widget.CursorAdapter.FLAG_AUTO_REQUERY]
     * or [android.widget.CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER]
     * (that is, use 0 for the flags argument).  This prevents the CursorAdapter
     * from doing its own observing of the Cursor, which is not needed since
     * when a change happens you will get a new Cursor throw another call
     * here.
     *  *  The Loader will release the data once it knows the application
     * is no longer using it.  For example, if the data is
     * a [android.database.Cursor] from a [android.content.CursorLoader],
     * you should not call close() on it yourself.  If the Cursor is being placed in a
     * [android.widget.CursorAdapter], you should use the
     * [android.widget.CursorAdapter.swapCursor]
     * method so that the old Cursor is not closed.
     *
     *
     *
     * This will always be called from the process's main thread.
     *
     * @param loader The Loader that has finished.
     * @param data The data generated by the Loader.
     */
    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        if (data == null) {
            binding.spinnerCategorias.adapter = null
            return
        }

        binding.spinnerCategorias.adapter = SimpleCursorAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            data,
            arrayOf(TabelaCategorias.CAMPO_DESCRICAO),
            intArrayOf(android.R.id.text1),
            0
        )

        mostraCategoriaSelecionadaSpinner()
    }

    private fun mostraCategoriaSelecionadaSpinner() {
        if (livro == null) return

        val idCategoria = livro!!.categoria.id

        val ultimaCategoria = binding.spinnerCategorias.count - 1
        for (i in 0..ultimaCategoria) {
            if (idCategoria == binding.spinnerCategorias.getItemIdAtPosition(i)) {
                binding.spinnerCategorias.setSelection(i)
                return
            }
        }
    }
}