package com.example.spring_postgres_spatial;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
class City {

	@Id
	@GeneratedValue
	private Long id;

	private String name;

	@Column(columnDefinition = "geography(Point, 4326)")
	private Point location;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class CityDto {
	private String name;
	private double lat;
	private double lng;
}

interface CityRepository extends CrudRepository<City, Long> {

	@Query("SELECT c FROM City c WHERE function('ST_DWithin', c.location, :point, :distance) = true")
	Iterable<City> findNearestCities(Point point, double distance);
}

@RestController
@RequestMapping("/api/v1/cities")
@RequiredArgsConstructor
class CityController {


	private final CityRepository cityRepository;

	private final GeometryFactory geometryFactory;

	@PostMapping
	void create(@RequestBody CreateCityRequest request) {
		Point point = geometryFactory.createPoint(new Coordinate(request.getLng(), request.getLat()));
		City city = new City();
		city.setName(request.getName());
		city.setLocation(point);
		cityRepository.save(city);
	}

	@GetMapping
	List<CityDto> findAll() {
		List<CityDto> cities = new ArrayList<>();
		cityRepository.findAll().forEach(c -> {
			cities.add(new CityDto(c.getName(), c.getLocation().getY(), c.getLocation().getX()));
		});
		return cities;
	}

	@GetMapping("/nearest")
	List<CityDto> findNearestCities(@RequestParam("lat") float lat, @RequestParam("lng") float lng, @RequestParam("distance") int distance) {
		List<CityDto> cities = new ArrayList<>();
		Point point = geometryFactory.createPoint(new Coordinate(lng, lat));
		cityRepository.findNearestCities(point, distance).forEach(c -> {
			cities.add(new CityDto(c.getName(), c.getLocation().getY(), c.getLocation().getX()));
		});
		return cities;
	}
}

@AllArgsConstructor
@NoArgsConstructor
@Data
class CreateCityRequest {

	private String name;
	private double lat;
	private double lng;
}

@Configuration
class GeometryConfig {
	@Bean
	GeometryFactory geometryFactory() {
		return new GeometryFactory();
	}
}

