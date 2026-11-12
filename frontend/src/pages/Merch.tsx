import nintendo from '../assets/inicio/nintendo.webp'
import polo1 from '../assets/merch/polo-1.webp'
import polo2 from '../assets/merch/polo-2.webp'
import polo3 from '../assets/merch/polo-3.webp'
import polo4 from '../assets/merch/polo-4.webp'
import polo5 from '../assets/merch/polo-5.webp'
import polo6 from '../assets/merch/polo-6.webp'

const polos = [polo1, polo2, polo3, polo4, polo5, polo6]

export function Merch() {
  return (
    <>
      <h1 className="sr-only">Merch de Anarkia Games</h1>
      <img src={nintendo} alt="Jugadores compitiendo con consolas Nintendo Switch" className="aspect-[1440/540] w-full object-cover" />

      <section className="mx-auto max-w-5xl px-4 py-16 text-center lg:py-24">
        <p className="text-2xl lg:text-4xl">Compra exclusivo en el evento y vive el gaming al máximo.</p>
        <p className="mt-6 inline-block rounded-full border border-verde px-5 py-1 font-display italic tracking-wide text-verde">
          Próximamente
        </p>

        <ul aria-label="Polos del evento" className="mt-14 grid grid-cols-2 gap-6 sm:grid-cols-3 lg:gap-x-20 lg:gap-y-14">
          {polos.map((polo, indice) => (
            <li key={polo} className="flex aspect-[303/337] items-center justify-center rounded-[20px] bg-claro p-3">
              <img src={polo} alt={`Polo gamer, modelo ${indice + 1}`} loading="lazy" className="max-h-full w-full object-contain" />
            </li>
          ))}
        </ul>
      </section>
    </>
  )
}
