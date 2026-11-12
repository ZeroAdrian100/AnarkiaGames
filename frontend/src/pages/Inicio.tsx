import estadio from '../assets/inicio/estadio.webp'
import hero from '../assets/inicio/hero.webp'
import nintendo from '../assets/inicio/nintendo.webp'
import logo1 from '../assets/logos/logo-1.webp'
import logo2 from '../assets/logos/logo-2.webp'
import logo3 from '../assets/logos/logo-3.webp'
import logo4 from '../assets/logos/logo-4.webp'
import logo5 from '../assets/logos/logo-5.webp'
import logo6 from '../assets/logos/logo-6.webp'
import logo7 from '../assets/logos/logo-7.webp'
import logo8 from '../assets/logos/logo-8.webp'
import logo9 from '../assets/logos/logo-9.webp'
import logo10 from '../assets/logos/logo-10.webp'
import logo11 from '../assets/logos/logo-11.webp'
import logo12 from '../assets/logos/logo-12.webp'

const juegos = [
  { src: logo1, nombre: 'Fortnite' },
  { src: logo2, nombre: 'Street Fighter II' },
  { src: logo3, nombre: 'Valorant' },
  { src: logo4, nombre: 'Counter-Strike 2' },
  { src: logo5, nombre: 'EA Sports FIFA' },
  { src: logo6, nombre: 'Dragon Ball Sparking! Zero' },
  { src: logo7, nombre: 'Splatoon 2' },
  { src: logo8, nombre: 'Super Smash Bros. Ultimate' },
  { src: logo9, nombre: 'Guitar Hero' },
  { src: logo10, nombre: 'Overwatch' },
  { src: logo11, nombre: 'Rocket League' },
  { src: logo12, nombre: 'League of Legends' },
]

const frase = 'px-4 text-center text-2xl leading-normal sm:text-4xl lg:text-5xl'

export function Inicio() {
  return (
    <>
      <h1 className="sr-only">Anarkia Games: torneos de videojuegos</h1>

      <div className="mx-auto max-w-8xl px-4 pt-6 sm:px-10 lg:px-[164px] lg:pt-4">
        <img
          src={hero}
          alt="Jugador celebrando frente a las pantallas de un torneo de esports"
          className="aspect-[1592/896] w-full object-cover shadow-[0_4px_4px_rgba(0,0,0,0.25),0_4px_4px_rgba(0,0,0,0.67)]"
        />
      </div>

      <p className={`${frase} py-24 lg:py-44`}>Forma parte del evento gamer que estabas esperando.</p>

      <img src={estadio} alt="Estadio lleno durante la final de un torneo" loading="lazy" className="aspect-[1920/845] w-full object-cover" />
      <img src={nintendo} alt="Jugadores compitiendo con consolas Nintendo Switch" loading="lazy" className="aspect-[1920/845] w-full object-cover" />

      <p className={`${frase} py-24 lg:py-36`}>Participa en el evento y conquista numerosos premios.</p>

      <ul aria-label="Juegos del evento" className="mx-auto grid max-w-6xl grid-cols-2 items-center justify-items-center gap-x-8 gap-y-12 px-4 pb-24 sm:grid-cols-4 lg:pb-36">
        {juegos.map((juego) => (
          <li key={juego.nombre}>
            <img src={juego.src} alt={juego.nombre} loading="lazy" className="h-20 w-auto max-w-[160px] object-contain sm:h-28 sm:max-w-[240px]" />
          </li>
        ))}
      </ul>
    </>
  )
}
